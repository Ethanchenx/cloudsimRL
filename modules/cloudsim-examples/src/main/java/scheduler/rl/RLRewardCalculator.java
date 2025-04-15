package scheduler.rl;


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @Author: Chen
 * @File Name: RLRewardCalculator.java
 */

public class RLRewardCalculator {
    public static Double calculateReward(List<Vm> vmList, Double[] vmCosts, Cloudlet cloudlet, double postImbalanceRate) {
        int n = vmList.size();
        double[] vmEstimateRemainingTimes = new double[n];

        for (int i = 0; i < n; i++){
            List<Cloudlet> execList = vmList.get(i).getCloudletScheduler().getCloudletExecList();
            List<Cloudlet> waitingList = vmList.get(i).getCloudletScheduler().getCloudletWaitingList();

            double estimateRemainingTime = (waitingList.stream()
                    .mapToDouble(Cloudlet::getCloudletLength) // 获取每个 Cloudlet 的执行长度
                    .sum() +
                    execList.stream()
                            .mapToDouble(Cloudlet::getCloudletLength)
                            .sum()) / vmList.get(i).getMips();

            vmEstimateRemainingTimes[i] = estimateRemainingTime;
        }

        vmEstimateRemainingTimes[cloudlet.getGuestId()] += cloudlet.getExecFinishTime() - cloudlet.getExecStartTime();

        double meanLoad = Arrays.stream(vmEstimateRemainingTimes).sum() / n;
        double imbalance = 0.0;
        for (double load : vmEstimateRemainingTimes) {
            imbalance += Math.abs(load - meanLoad);
        }
        double imbalanceRate = imbalance / (meanLoad * n);




        return postImbalanceRate - imbalanceRate;
    }
}
