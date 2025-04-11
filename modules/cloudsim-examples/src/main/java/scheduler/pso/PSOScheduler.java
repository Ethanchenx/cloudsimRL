package scheduler.pso;


/**
 * @Author: Chen
 * @File Name: PSOScheduler.java
 */



import net.sourceforge.jswarm_pso.Swarm;
import scheduler.model.VmConfig;

import java.util.Arrays;


public class PSOScheduler {

    private int numCloudlets;
    private int numVms;
    private double[][] executionTimeMatrix;

    public PSOScheduler(int numCloudlets, int numVms, double[][] executionTimeMatrix) {
        this.numCloudlets = numCloudlets;
        this.numVms = numVms;
        this.executionTimeMatrix = executionTimeMatrix;
    }

    public int[] run() {
        SchedulingProblem schedulingProblem = new SchedulingProblem(
                numCloudlets,
                numVms,
                executionTimeMatrix,
                VmConfig.COST_C1,
                VmConfig.COST_C2,
                VmConfig.COST_C3
        );


        DiscreteParticleSoftmax.setDimensions(numCloudlets, numVms);
        Swarm swarm = new Swarm(
                50,
                new DiscreteParticleSoftmax(),  // ✅ 现在是无参构造函数
                schedulingProblem
        );

        // 设置每个维度的范围
        int dimensions = numCloudlets * numVms;

        double[] maxPos = new double[dimensions];
        double[] minPos = new double[dimensions];
        Arrays.fill(maxPos, 1.0);  // 让softmax起作用，控制权重范围
        Arrays.fill(minPos, -1.0);
        swarm.setMaxPosition(maxPos);
        swarm.setMinPosition(minPos);

        double[] maxVel = new double[dimensions];
        double[] minVel = new double[dimensions];
        Arrays.fill(maxVel, 0.1);
        Arrays.fill(minVel, -0.1);
        swarm.setMaxVelocity(maxVel);
        swarm.setMinVelocity(minVel);


        swarm.setInertia(0.9);

        // PSO 主循环
        int maxIterations = 150;
        for (int i = 0; i < maxIterations; i++) {
            swarm.evolve();

            // 日志：显示当前最优值
            double bestFitness = swarm.getBestFitness();
            System.out.printf("Iteration %d: Best Makespan = %.4f%n", i + 1, bestFitness);
        }

        // 获取最优解（Cloudlet→VM 分配方案）
        double[] bestPosition = swarm.getBestPosition();
        return ParticleDecoder.decodeSoftmax(bestPosition, numCloudlets, numVms);
    }
}

