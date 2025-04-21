package scheduler.rl;


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @Author: Chen
 * @File Name: RLRewardCalculator.java
 */

public class RLRewardCalculator {
    public static List<Double> calculateReward(List<Vm> vmList, Double[] vmCosts, Cloudlet cloudlet, double postImbalanceRate, List<Double> previousState, Cloudlet previousCloudlet) {
        int n = vmList.size();
        double[] vmEstimateRemainingTimes = new double[n];
        List<Double> nextState = new ArrayList<>();

//        for (int i = 0; i < n; i++){
//            List<Cloudlet> execList = vmList.get(i).getCloudletScheduler().getCloudletExecList();
//            List<Cloudlet> waitingList = vmList.get(i).getCloudletScheduler().getCloudletWaitingList();
//
//            double estimateRemainingTime = (waitingList.stream()
//                    .mapToDouble(Cloudlet::getCloudletLength) // 获取每个 Cloudlet 的执行长度
//                    .sum() +
//                    execList.stream()
//                            .mapToDouble(Cloudlet::getCloudletLength)
//                            .sum()) / vmList.get(i).getMips();
//
//            vmEstimateRemainingTimes[i] = estimateRemainingTime;
//            nextState.add(estimateRemainingTime);
//
//        }

//        vmEstimateRemainingTimes[cloudlet.getGuestId()] += cloudlet.getExecFinishTime() - cloudlet.getExecStartTime();
//        nextState.set(cloudlet.getGuestId(), nextState.get(cloudlet.getGuestId()) + cloudlet.getExecFinishTime() - cloudlet.getExecStartTime());
//
//        double meanLoad = Arrays.stream(vmEstimateRemainingTimes).sum() / n;
//        double imbalance = 0.0;
//        for (double load : vmEstimateRemainingTimes) {
//            imbalance += Math.abs(load - meanLoad);
//        }
//        double imbalanceRate = imbalance / (meanLoad * n);
//        nextState = previousState;
//        int guestId = previousCloudlet.getGuestId();
//        double a = nextState.get(guestId) + previousCloudlet.getCloudletLength() / vmList.get(guestId).getMips();
//        nextState.set(guestId, a);
//        double imbalanceRate = calculateImbalanceRate(nextState);


        List<Double> eachImbalanceRate = new ArrayList<>();
        for (int i = 0; i < previousState.size(); i++){
            List<Double> temp = previousState;
            double val = temp.get(i) + previousCloudlet.getCloudletLength() / vmList.get(i).getMips();
            temp.set(i, val);
            eachImbalanceRate.add(i, calculateImbalanceRate(temp));
            if (i == previousCloudlet.getGuestId()){
                nextState = temp;
            }
        }
        double bestRate = eachImbalanceRate.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double worstRate = eachImbalanceRate.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double imbalanceRate = eachImbalanceRate.get(previousCloudlet.getGuestId());


        double normalized = (imbalanceRate - bestRate) / (worstRate - bestRate);
        double reward = 1.0 - 2.0 * normalized;  // Maps best to +1, worst to -1
//        if (reward <= 0) {
//            reward = -1;
//        }
//        double reward = -(Arrays.stream(vmEstimateRemainingTimes).max().getAsDouble() - Arrays.stream(vmEstimateRemainingTimes).min().getAsDouble());

        nextState.add(reward);
        return nextState;
    }

    public static double calculateImbalanceRate(List<Double> list){
        double meanLoad = list.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double imbalance = 0.0;
        for (double load : list) {
            imbalance += Math.abs(load - meanLoad);
        }
        double imbalanceRate = imbalance / (meanLoad * list.size());
        return imbalanceRate;
    }
}
