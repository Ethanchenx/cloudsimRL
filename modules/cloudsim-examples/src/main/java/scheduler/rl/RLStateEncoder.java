package scheduler.rl;


/**
 * @Author: Chen
 * @File Name: RLStateEncoder.java
 */


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.Arrays;
import java.util.List;

public class RLStateEncoder {

    public static double[] buildVmLoadState(List<Vm> vmList, List<Cloudlet> submittedCloudlets) {
        int n = vmList.size();
        double[] vmLoads = new double[n];

        if (submittedCloudlets != null) {
            for (Cloudlet c : submittedCloudlets) {
                int index = findVmIndexById(vmList, c.getVmId());
                if (index != -1) {
                    vmLoads[index] += c.getCloudletLength(); // 或其他负载估计方式
                }
            }
        }

        // 归一化（避免除以0）
        double max = Arrays.stream(vmLoads).max().orElse(1.0);
        if (max > 0) {
            for (int i = 0; i < n; i++) {
                vmLoads[i] /= max;
            }
        }

        return vmLoads;
    }


    private static int findVmIndexById(List<Vm> vmList, int vmId) {
        for (int i = 0; i < vmList.size(); i++) {
            if (vmList.get(i).getId() == vmId) return i;
        }
        return -1;
    }
}

