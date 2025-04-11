package scheduler.brokers;


/**
 * @Author: Chen
 * @File Name: RRBroker.java
 */


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudActionTags;

import java.util.*;

public class RRBroker extends DatacenterBroker {

    public RRBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        List<Cloudlet> cloudletList = getCloudletList();
        List<Vm> vmList = getGuestsCreatedList();

        int numVms = vmList.size();
        int vmIndex = 0;

        System.out.println("🎯 RR 调度开始: Cloudlets = " + cloudletList.size() + ", VMs = " + numVms);

        for (Cloudlet cl : cloudletList) {
            int vmId = vmList.get(vmIndex).getId();

            cl.setVmId(vmId);
            send(
                    getVmsToDatacentersMap().get(vmId),
                    0.0,
                    CloudActionTags.CLOUDLET_SUBMIT,
                    cl
            );

            getCloudletSubmittedList().add(cl);
            cloudletsSubmitted++;

            // 🌀 Round-Robin: 下一个 VM
            vmIndex = (vmIndex + 1) % numVms;
        }

        getCloudletList().clear();
    }
}
