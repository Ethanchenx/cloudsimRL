import pandas as pd
import matplotlib.pyplot as plt

def draw_training_process_metrics(file_path, file_name):
    df = pd.read_csv(f"{file_path}/{file_name}")

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
    plt.savefig(f"{file_path}/metrics.png")

    # 显示图表
    plt.show()

if __name__ == '__main__':
    draw_training_process_metrics("modules/cloudsim-examples/src/main/python/GoCJ_250_3_10", "result.csv")
