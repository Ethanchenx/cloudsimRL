package scheduler.rl;


/**
 * @Author: Chen
 * @File Name: RLStateEncoder.java
 */


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RLStateEncoder {

    public static List<Double> buildVmsState(List<Vm> vmList, Double[] vmCosts, Cloudlet cloudlet) {
        int n = vmList.size();
        double[] vmEstimateRemainingTimes = new double[n];
        List<Double> vmStates = new ArrayList<>();

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
            vmStates.add(estimateRemainingTime);
        }

        return vmStates;
    }


}

