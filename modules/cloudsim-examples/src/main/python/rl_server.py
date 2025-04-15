import socket
import json
import numpy as np
import random
import torch
import torch.nn as nn
import torch.optim as optim
from collections import deque

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

# 强化学习环境和DQN训练
def select_action(state, model, epsilon):
    """选择动作：epsilon-greedy策略"""
    if random.random() < epsilon:
        return random.randint(0, (int)(len(state)/2) - 1)  # 随机选择一个虚拟机
    else:
        state_tensor = torch.tensor(state, dtype=torch.float32)
        q_values = model(state_tensor)
        print(q_values)
        return torch.argmax(q_values).item()  # 选择Q值最大的虚拟机

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
    input_dim = 7  # 假设state有7个维度（虚拟机负载等信息）
    output_dim = 3  # 假设有3台虚拟机
    model = DQN(input_dim, output_dim)
    target_model = DQN(input_dim, output_dim)
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    replay_buffer = ReplayBuffer(10000)

    epsilon = 0.1  # epsilon-greedy策略中的探索率
    gamma = 0.99   # 奖励折扣因子
    batch_size = 32
    counts = 0

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
                print(f"Received state data: {data}")

                # 解析状态和cloudletId
                data = json.loads(data)
                state = np.array(data['state'])
                cloudlet_id = data['cloudletId']

                # 选择动作
                action = select_action(state, model, epsilon)

                # 返回选定的动作（虚拟机ID）
                print(f"[State] {state} → [Action] {action}")
                conn.sendall((json.dumps({"action": action}) + "\n").encode())
                print('Action sent successfully')

                # 2. 接收reward数据
                reward_data = conn.recv(4096).decode().strip()
                if not reward_data:
                    print("Error: No reward data received!")
                    break
                print(f"Received reward data: {reward_data}")

                # 假设reward数据以json格式发送
                reward_data = json.loads(reward_data)
                reward = reward_data.get('reward', 0)  # 从reward数据中获取奖励

                # 将经验存入回放池
                replay_buffer.add((state, action, reward, state, False))  # 这里假设没有终止条件

                # 训练DQN模型
                train_dqn(model, target_model, replay_buffer, optimizer, batch_size, gamma)
                counts += 1
                if counts >= 1000:
                    break

        except Exception as e:
            print(f"Error: {e}")
        finally:
            conn.close()

        # 每隔一定步骤更新目标网络
        target_model.load_state_dict(model.state_dict())
        counts = 0

if __name__ == '__main__':
    run_server()
