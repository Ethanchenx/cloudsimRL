import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Read data from CSV file
df = pd.read_csv("modules/cloudsim-examples/src/main/python/GoCJ_200_3_10/compare.csv")

print(df)
# Create a 2x2 grid of subplots
fig, axs = plt.subplots(2, 2, figsize=(12, 10))

# Plot Makespan with log scale
axs[0, 0].bar(df['Algo'], df['Makespan'], color='skyblue')
axs[0, 0].set_title('Makespan Comparison')
axs[0, 0].set_ylabel('Makespan')
axs[0, 0].set_yscale('log')  # Apply logarithmic scale to the y-axis

# Plot TotalCost with log scale
axs[0, 1].bar(df['Algo'], df['TotalCost'], color='lightgreen')
axs[0, 1].set_title('TotalCost Comparison')
axs[0, 1].set_ylabel('Total Cost')
axs[0, 1].set_yscale('log')  # Apply logarithmic scale to the y-axis

# Plot Utilization with log scale
axs[1, 0].bar(df['Algo'], df['Utilization'], color='lightcoral')
axs[1, 0].set_title('Utilization Comparison')
axs[1, 0].set_ylabel('Utilization')
axs[1, 0].set_yscale('log')  # Apply logarithmic scale to the y-axis

# Plot Imbalance with log scale
axs[1, 1].bar(df['Algo'], df['Imbalance'], color='lightyellow')
axs[1, 1].set_title('Imbalance Comparison')
axs[1, 1].set_ylabel('Imbalance')
axs[1, 1].set_yscale('log')  # Apply logarithmic scale to the y-axis

# Adjust layout
plt.tight_layout()

# Show the plot
plt.show()