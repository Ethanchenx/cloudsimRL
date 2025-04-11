package scheduler.eval;


/**
 * @Author: Chen
 * @File Name: EvaluationMetrics.java
 */


import org.cloudbus.cloudsim.Cloudlet;
import scheduler.model.VmConfig;

import java.util.List;


public class EvaluationMetrics {

    public static class Result {
        public double makespan;
        public double totalCost;
        public double utilization;
        public double imbalance;
    }

    /**
     * 评估指标：输入已完成 cloudlet 列表，计算核心指标
     */
    public static Result evaluate(List<Cloudlet> cloudletList) {
        Result r = new Result();

        int numVms = VmConfig.VM_NUMS;
        double[] vmTimes = new double[numVms];
        double totalExecTime = 0;
        double totalCost = 0;
        double makespan = 0;

        double[] costC1 = VmConfig.COST_C1;
        double[] costC2 = VmConfig.COST_C2;
        double[] costC3 = VmConfig.COST_C3;

        for (Cloudlet c : cloudletList) {
            int vmId = c.getVmId();
//            double execTime = c.getActualCPUTime();
            double execTime = c.getFinishTime() - c.getExecStartTime();
            vmTimes[vmId] += execTime;
            totalExecTime += execTime;
            makespan = Math.max(makespan, c.getFinishTime());

            // 成本估算
            double cost = execTime * (costC1[vmId] + costC2[vmId] + costC3[vmId]);
            totalCost += cost;
        }

        // 资源利用率
        double utilization = totalExecTime / (makespan * numVms);

        // 负载均衡：绝对偏差
        double mean = totalExecTime / numVms;
        double imbalance = 0.0;
        for (double t : vmTimes) {
            imbalance += Math.abs(t - mean);
        }
        double normalizedImbalance = imbalance / (mean * numVms);  // ∈ [0,1]


        r.makespan = makespan;
        r.totalCost = totalCost;
        r.utilization = utilization;
        r.imbalance = normalizedImbalance;

        return r;
    }

    public static void print(Result r, String name) {
        System.out.printf("📊 [%s] Evaluation Result:\n", name);
        System.out.printf("🕒 Makespan:      %.2f\n", r.makespan);
        System.out.printf("💰 Total Cost:    %.2f\n", r.totalCost);
        System.out.printf("⚙️  Utilization:   %.4f\n", r.utilization);
        System.out.printf("📊 Imbalance:     %.2f\n", r.imbalance);
        System.out.println();
    }
}

