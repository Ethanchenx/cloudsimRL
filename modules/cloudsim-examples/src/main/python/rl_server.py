import socket
import json
import numpy as np
import random
import torch
import torch.nn as nn
import torch.optim as optim
from collections import deque
import math
from pathlib import Path
import draw_metrics
import draw_compare

# DQN模型
class DQN(nn.Module):
    def __init__(self, input_dim, output_dim):
        super(DQN, self).__init__()
        self.fc1 = nn.Linear(input_dim, 64)
        self.fc2 = nn.Linear(64, 64)
        self.fc3 = nn.Linear(64, output_dim)
    
    def forward(self, state):
        x = torch.relu(self.fc1(state))
        x = torch.relu(self.fc2(x))
        return self.fc3(x)

# 经验回放池
class ReplayBuffer:
    def __init__(self, capacity):
        self.buffer = deque(maxlen=capacity)
    
    def add(self, experience):
        self.buffer.append(experience)
    
    def sample(self, batch_size):
        return random.sample(self.buffer, batch_size)
    
    def size(self):
        return len(self.buffer)

def select_action(state, model, epsilon, vm_nums, use_softmax=False, temperature=1.0):
    """
    Epsilon-greedy or softmax-based action selection
    """
    if random.random() < epsilon:
        return random.randint(0, vm_nums-1)     #

    state_tensor = torch.tensor(state, dtype=torch.float32)
    q_values = model(state_tensor)

    print(q_values)  # Debug print

    if use_softmax:
        # Temperature-scaled softmax sampling
        probs = torch.softmax(q_values / temperature, dim=0).detach().numpy()
        action = np.random.choice(len(state) - 1, p=probs)
        return action
    else:
        # Greedy action
        return torch.argmax(q_values).item()

def train_dqn(model, target_model, replay_buffer, optimizer, batch_size, gamma):
    """训练DQN模型"""
    if replay_buffer.size() < batch_size:
        return
    
    # 采样一批数据
    batch = replay_buffer.sample(batch_size)
    states, actions, rewards, next_states, dones = zip(*batch)
    
    states = torch.tensor(states, dtype=torch.float32)
    actions = torch.tensor(actions, dtype=torch.long)
    rewards = torch.tensor(rewards, dtype=torch.float32)
    next_states = torch.tensor(next_states, dtype=torch.float32)
    dones = torch.tensor(dones, dtype=torch.bool)
    
    # 计算Q值
    q_values = model(states).gather(1, actions.unsqueeze(1)).squeeze(1)
    next_q_values = target_model(next_states).max(1)[0]
    target_q_values = rewards + gamma * next_q_values * (~dones)

    print(f'Predicted Q-values: {q_values}')
    print(f'Target Q-values: {target_q_values}')
    
    # 计算损失
    loss = nn.MSELoss()(q_values, target_q_values)
    print(f'loss: {loss.item()}')
    
    # 反向传播更新模型
    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

# 服务器代码
def run_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('localhost', 5678))
    server.listen(1)
    print("Server listening on port 5678")

    conn, addr = server.accept()
    print(f"Connection from {addr}")

    config_data = conn.recv(4096).decode().strip()
    config_data = json.loads(config_data)
    vm_nums = config_data['vm_nums']
    task_nums = config_data['task_nums']
    iteration_nums = config_data['iteration_nums']
    dataset_name = config_data['dataset_name']
    print(config_data)

    max_iterations = task_nums * iteration_nums
    saving_path = f"modules/cloudsim-examples/src/main/python/{dataset_name}_{task_nums}_{vm_nums}_{iteration_nums}"

    # DQN和优化器初始化
    input_dim = 6  # 假设state有7个维度（虚拟机负载等信息）
    output_dim = vm_nums  # align with parameter in java (vm nums) 
    train_count = 0

    model = DQN(input_dim, output_dim)
    target_model = DQN(input_dim, output_dim)
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    replay_buffer = ReplayBuffer(10000)

    epsilon = 0.9  # epsilon-greedy策略中的探索率
    gamma = 0.99   # 奖励折扣因子
    batch_size = 32

    epsilon_start = 0.9
    epsilon_end = 0

    try:
        while True:
            # 1. Receive state data
            data = conn.recv(4096).decode().strip()

            # Check if the client has disconnected (empty data)
            if not data:
                print("Client disconnected.")
                break  # Exit the while loop if client disconnects

            print(f"\nReceived state data: {data}")

            # Parse state and cloudletId
            data = json.loads(data)
            state = np.array(data['state'])
            cloudlet_length = data['cloudletLength']
            cloudlet_id = data['cloudletId']
            estimate_runtime = data['estimateRuntime']
            state = np.append(state, estimate_runtime)

            if cloudlet_id == vm_nums:
                train_count += vm_nums

            # Adjust epsilon dynamically
            epsilon = adjust_epsilon_linear(train_count, max_iterations, epsilon_start, epsilon_end)
            print(epsilon)

            # Select action
            if train_count < 200:
                action = random.randint(0, vm_nums-1)
            else:
                action = select_action(state, model, epsilon, vm_nums)

            # Return selected action (VM ID)
            print(f"[State] {state} → [Action] {action}")
            conn.sendall((json.dumps({"action": action}) + "\n").encode())
            print('Action sent successfully')

            reward, next_state = calculate_reward(state, action, vm_nums)
            print(reward)
            print(next_state)

            # Store experience in replay buffer
            replay_buffer.add((state, action, reward, next_state, False))  # No termination condition assumed

            # Train DQN model
            train_dqn(model, target_model, replay_buffer, optimizer, batch_size, gamma)
            train_count += 1

            # if train_count % 100 == 0 and epsilon > 0.1:
            #     epsilon -= 0.1
            print(train_count)

            if (train_count) % 100 == 0:
                save_model(target_model, saving_path + "/models", f"target_model_{train_count}.pth")

    # except Exception as e:
    #     print(f"Error: {e}")

    finally:
        conn.close()  # Ensure the connection is closed properly when done
        print("Connection closed.")

    draw_plot(saving_path)


# 保存模型的权重（state_dict）
def save_model(model, path, file_name):
    folder_path = Path(path)
    folder_path.mkdir(parents=True, exist_ok=True)

    torch.save(model.state_dict(), f"{path}/{file_name}")
    print(f"Model saved to {path + file_name}")

def calculate_reward(state, action, vm_nums):
    vm_loads = state[:vm_nums]              # current remaining exec times
    est_runtimes = state[vm_nums:]          # estimate runtime for this task on each VM

    updated_loads = vm_loads.copy()
    updated_loads[action] += est_runtimes[action]

    predicted_makespan = max(updated_loads)

    next_state = np.append(updated_loads, est_runtimes)

    all_makespan = []
    for i in range(3):
        loads = vm_loads.copy()
        loads[i] += est_runtimes[i]
        all_makespan.append(max(loads)-predicted_makespan)
    normalized = normalize_to_minus_one_one(all_makespan)
    print(normalized)

    reward = -normalized[action] * 1000
    return reward, next_state

def adjust_epsilon_linear(train_count, max_iterations, epsilon_start=1.0, epsilon_end=0.1):
    epsilon = epsilon_start - (epsilon_start - epsilon_end) * (train_count / max_iterations)
    return max(epsilon, epsilon_end)  # Ensure epsilon doesn't go below epsilon_end


def normalize_to_minus_one_one(arr):
    # Find the minimum and maximum values of the array
    arr_min = np.min(arr)
    arr_max = np.max(arr)
    
    # Apply the normalization formula
    normalized_arr = 2 * (arr - arr_min) / (arr_max - arr_min) - 1
    return normalized_arr

def draw_plot(path):
    draw_metrics.draw_training_process_metrics(path, "result.csv")
    # draw_compare.draw_compare(path, "compare.csv")

if __name__ == '__main__':
    run_server()
