import pandas as pd
import matplotlib.pyplot as plt

# 从 CSV 文件读取数据
file_path = "modules/cloudsim-examples/src/main/python/GoCJ_200_3_10/output.csv"  # 替换成你的 CSV 文件路径
df = pd.read_csv(file_path)

# 设置每个指标的折线图
metrics = ["Makespan", "TotalCost", "Utilization", "Imbalance"]

# 创建子图
fig, axes = plt.subplots(2, 2, figsize=(12, 10))  # 2x2 格局的子图
axes = axes.flatten()

# 为每个指标创建单独的折线图
for i, metric in enumerate(metrics):
    axes[i].plot(df["Iteration"], df[metric], label=metric, marker='o')
    axes[i].set_title(f"{metric} vs Iteration", fontsize=14)
    axes[i].set_xlabel("Iteration", fontsize=12)
    axes[i].set_ylabel(metric, fontsize=12)
    axes[i].grid(True)
    axes[i].legend()

# 调整布局
plt.tight_layout()

# 显示图表
plt.show()
