package scheduler.env;


/**
 * @Author: Chen
 * @File Name: VmFactory.java
 */


import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Vm;
import scheduler.model.VmConfig;

import java.util.ArrayList;
import java.util.List;

public class VmFactory {

    public static List<Vm> createVmList(int brokerId) {
        List<Vm> list = new ArrayList<>();

        int numVms = VmConfig.MIPS_LIST.length;
        for (int i = 0; i < numVms; i++) {
            Vm vm = new Vm(
                    i, brokerId,
                    VmConfig.MIPS_LIST[i],
                    VmConfig.PES_LIST[i],
                    VmConfig.RAM_LIST[i],
                    VmConfig.BW_LIST[i],
                    VmConfig.SIZE_LIST[i],
                    VmConfig.VMM,
                    new CloudletSchedulerSpaceShared()  // 也可以传参切换策略
            );
            list.add(vm);
        }

        return list;
    }
}

