package scheduler.pso;


/**
 * @Author: Chen
 * @File Name: SchedulingProblem.java
 */


import net.sourceforge.jswarm_pso.FitnessFunction;

import java.util.Arrays;

/**
 * 高质量多目标调度评分函数：
 * F1 = makespan 越小越好（响应性）
 * F2 = 成本越低越好（经济性）
 * F3 = 利用率越高越好（资源使用效率）
 * F4 = 负载越均衡越好（公平性）
 */
public class SchedulingProblem extends FitnessFunction {

    private final int numCloudlets;
    private final int numVms;
    private final double[][] execTime; // [cloudlet][vm]
    private final double[] costC1;
    private final double[] costC2;
    private final double[] costC3;

    // 权重（可调）
    private final double gamma1 = 0.6;
    private final double gamma2 = 0.1;
    private final double gamma3 = 0.15;
    private final double gamma4 = 0.15;

    //
    private double minMakespan = 0;
    private double maxMakespan = 0;

    private ParticleDecoder particleDecoder;

    public SchedulingProblem(int numCloudlets, int numVms, double[][] execTimeMatrix,
                             double[] costC1, double[] costC2, double[] costC3) {
        super(true); // ✅ 最大化目标函数
        this.numCloudlets = numCloudlets;
        this.numVms = numVms;
        this.execTime = execTimeMatrix;
        this.costC1 = costC1;
        this.costC2 = costC2;
        this.costC3 = costC3;

        double[] range = estimateMakespanRange(execTimeMatrix, numVms);
        this.minMakespan = range[0];
        this.maxMakespan = range[1];

        System.out.printf("🔍 Estimated makespan range: [%.2f, %.2f]%n", minMakespan, maxMakespan);

    }

    @Override
    public double evaluate(double[] position) {
        double[] vmTimes = new double[numVms];
        double[] vmLoads = new double[numVms];
        double totalCost = 0.0;
        double totalExecTime = 0.0;
        double[] minCostPerTask = new double[numCloudlets];
        int[] vmIndexList = ParticleDecoder.decodeSoftmax(position, numCloudlets, numVms);

        for (int i = 0; i < numCloudlets; i++) {
//            int vmIndex = Math.max(0, Math.min(numVms - 1, (int) Math.floor(position[i])));
            int vmIndex = vmIndexList[i];
            double time = execTime[i][vmIndex];

            vmTimes[vmIndex] += time;
            vmLoads[vmIndex] += time;
            totalExecTime += time;

            double cost = time * (costC1[vmIndex] + costC2[vmIndex] + costC3[vmIndex]);
            totalCost += cost;

            double minCost = Double.MAX_VALUE;
            for (int j = 0; j < numVms; j++) {
                double candidate = execTime[i][j] * (costC1[j] + costC2[j] + costC3[j]);
                if (candidate < minCost) minCost = candidate;
            }
            minCostPerTask[i] = minCost;
        }

        // === F1: Makespan (越小越好 → 越大越优)
        double makespan = Arrays.stream(vmTimes).max().orElse(1.0);
        double f1 = (maxMakespan - makespan) / (maxMakespan - minMakespan);
        f1 = Math.max(0.0, Math.min(1.0, f1));


        // === F2: 成本最小化（越小越好 → 越大越优）
        double minCostSum = Arrays.stream(minCostPerTask).sum();
        double f2 = minCostSum / totalCost;
        f2 = Math.max(0.0, Math.min(1.0, f2));

        // === F3: 利用率 = 所有任务时间 / (makespan × VM 数)
        double f3 = totalExecTime / (makespan * numVms);
        f3 = Math.max(0.0, Math.min(1.0, f3));

        // === F4: 负载均衡度 = 1 - 不均衡程度
        double meanLoad = totalExecTime / numVms;
        double imbalance = 0.0;
        for (double load : vmLoads) {
            imbalance += Math.abs(load - meanLoad);
        }
        double f4 = 1.0 - (imbalance / (meanLoad * numVms));
        f4 = Math.max(0.0, f4); // 保证非负

        // === 综合评分
        double score = gamma1 * f1 + gamma2 * f2 + gamma3 * f3 + gamma4 * f4;

        // ✅ 日志输出
//        System.out.printf("F1=%.4f | F2=%.4f | F3=%.4f | F4=%.4f | Score=%.4f\n", f1, f2, f3, f4, score);

        return score;
    }

    public static double[] estimateMakespanRange(double[][] execTimeMatrix, int numVms) {
        int numTasks = execTimeMatrix.length;

        // 估计最小 makespan：最优调度下的理想情况（均分总工作量）
        double totalExecWork = 0.0;
        for (int i = 0; i < numTasks; i++) {
            // 每个任务在最优 VM 上执行
            totalExecWork += Arrays.stream(execTimeMatrix[i]).min().orElse(0.0);
        }
        double idealMinMakespan = totalExecWork / numVms;

        // 估计最大 makespan：最差调度下所有任务都分给最慢 VM
        double worstMakespan = 0.0;
        for (int i = 0; i < numTasks; i++) {
            worstMakespan += Arrays.stream(execTimeMatrix[i]).max().orElse(0.0);
        }

        return new double[]{idealMinMakespan, worstMakespan};
    }

}
