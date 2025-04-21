import socket
import json
import numpy as np
import random
import torch
import torch.nn as nn
import torch.optim as optim
from collections import deque
import math

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

def select_action(state, model, epsilon, use_softmax=False, temperature=1.0):
    """
    Epsilon-greedy or softmax-based action selection
    """
    if random.random() < epsilon:
        return random.randint(0, 2)     #

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
    
    # 计算损失
    loss = nn.MSELoss()(q_values, target_q_values)
    
    # 反向传播更新模型
    optimizer.zero_grad()
    loss.backward()
    optimizer.step()

# 服务器代码
def run_server():
    # DQN和优化器初始化
    input_dim = 6  # 假设state有7个维度（虚拟机负载等信息）
    output_dim = 3  # align with parameter in java (vm nums) 
    train_count = 0

    model = DQN(input_dim, output_dim)
    target_model = DQN(input_dim, output_dim)
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    replay_buffer = ReplayBuffer(10000)

    epsilon = 0.9  # epsilon-greedy策略中的探索率
    gamma = 0.99   # 奖励折扣因子
    batch_size = 32


    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('localhost', 5678))
    server.listen(1)
    print("Server listening on port 5678")

    while True:
        conn, addr = server.accept()
        print(f"Connection from {addr}")

        try:
            while True:
                # 1. 接收state数据
                data = conn.recv(4096).decode().strip()
                if not data:
                    break
                print()
                print(f"\nReceived state data: {data}")

                # 解析状态和cloudletId
                data = json.loads(data)
                state = np.array(data['state'])
                cloudlet_length = data['cloudletLength']
                estimate_runtime = data['estimateRuntime']
                state = np.append(state, estimate_runtime)   #

                # 选择动作
                if train_count < 200:
                    action = random.randint(0, 2)
                else:
                    action = select_action(state, model, epsilon)

                # 返回选定的动作（虚拟机ID）
                print(f"[State] {state} → [Action] {action}")
                conn.sendall((json.dumps({"action": action}) + "\n").encode())
                print('Action sent successfully')


                  
                reward, next_state = calculate_reward(state, action)
                print(reward)
                print(next_state)

                


                # 将经验存入回放池
                replay_buffer.add((state, action, reward, next_state, False))  # 这里假设没有终止条件

                # 训练DQN模型
                train_dqn(model, target_model, replay_buffer, optimizer, batch_size, gamma)
                train_count += 1

                if train_count % 100 == 0 and epsilon > 0.1:
                    epsilon -= 0.1

                if train_count % 100 == 0:
                    save_model(target_model, f"target_model_{train_count}.pth")


        # except Exception as e:
        #     print(f"Error: {e}")
        finally:
            conn.close()


# 保存模型的权重（state_dict）
def save_model(model, path):
    torch.save(model.state_dict(), "modules\\cloudsim-examples\\src\\main\\python\\" + path)
    print(f"Model saved to {path}")

def calculate_reward(state, action):
    vm_loads = state[:3]              # current remaining exec times
    est_runtimes = state[3:]          # estimate runtime for this task on each VM

    updated_loads = vm_loads.copy()
    updated_loads[action] += est_runtimes[action]

    predicted_makespan = max(updated_loads)

    # Use negative makespan as reward to encourage minimization
    reward = -predicted_makespan

    next_state = np.append(updated_loads, est_runtimes)
    return reward, next_state


if __name__ == '__main__':
    run_server()
