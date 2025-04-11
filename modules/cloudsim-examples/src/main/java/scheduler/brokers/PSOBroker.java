package scheduler.brokers;


/**
 * @Author: Chen
 * @File Name: PSOBroker.java
 */


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSimTags;
import scheduler.pso.PSOScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PSOBroker extends DatacenterBroker {

    public PSOBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        List<Cloudlet> cloudlets = getCloudletList();
        List<Vm> vms = getGuestsCreatedList();
        int numCloudlets = cloudlets.size();
        int numVms = vms.size();

        System.out.println("✅ PSOBroker 调度开始: " + numCloudlets + " cloudlets → " + numVms + " VMs");

        // 构造执行时间矩阵
        double[][] execMatrix = new double[numCloudlets][numVms];
        for (int i = 0; i < numCloudlets; i++) {
            Cloudlet cl = cloudlets.get(i);
            for (int j = 0; j < numVms; j++) {
                Vm vm = vms.get(j);
                execMatrix[i][j] = (double) cl.getCloudletLength() / vm.getMips();
            }
        }

        // 运行 PSO 调度
        PSOScheduler scheduler = new PSOScheduler(numCloudlets, numVms, execMatrix);
        int[] mapping = scheduler.run();

        for (int i = 0; i < numCloudlets; i++) {
            int vmId = mapping[i];
            Cloudlet cl = cloudlets.get(i);
            cl.setVmId(vmId);

            System.out.printf("Cloudlet %d → VM %d%n", cl.getCloudletId(), vmId);

            send(
                    getVmsToDatacentersMap().get(vmId),
                    0.0,
                    CloudActionTags.CLOUDLET_SUBMIT,
                    cl
            );

            cloudletsSubmitted++;
            getCloudletSubmittedList().add(cl);
        }
        Map<Integer, Integer> vmLoad = new HashMap<>();
        for (int vmId : mapping) {
            vmLoad.put(vmId, vmLoad.getOrDefault(vmId, 0) + 1);
        }
        System.out.println("📊 Cloudlet 分配分布: " + vmLoad);

        cloudlets.clear(); // 清空原始队列
    }
}
