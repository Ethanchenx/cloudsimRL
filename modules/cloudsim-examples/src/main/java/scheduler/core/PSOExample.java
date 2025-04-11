package scheduler.core;


/**
 * @Author: Chen
 * @File Name: PSOExample.java
 */


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import scheduler.brokers.PSOBroker;
import scheduler.env.CloudletFactory;
import scheduler.env.DataCenterFactory;
import scheduler.env.VmFactory;
import scheduler.eval.EvaluationMetrics;

import java.util.Calendar;
import java.util.List;

public class PSOExample {

    public static void run() {
        try {
            // 初始化 CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // 创建数据中心
            Datacenter datacenter = DataCenterFactory.createSimpleDatacenter("Datacenter_PSO");

            // 创建 Broker（不需要 Cloudlet 和 VM 参数）
            PSOBroker broker = new PSOBroker("PSO_Broker");
            int brokerId = broker.getId();

            List<Vm> vmList = VmFactory.createVmList(brokerId);
            List<Cloudlet> cloudletList = CloudletFactory.createCloudletList(brokerId);

            // 提交给 Broker
            broker.submitGuestList(vmList);
            broker.submitCloudletList(cloudletList);

            // 启动仿真
            CloudSim.startSimulation();
            List<Cloudlet> results = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // 输出结果
            System.out.println("\n=== PSO 调度结果 ===");
            for (Cloudlet cl : results) {
                System.out.printf("Cloudlet %d → VM %d | Start %.2f | Finish %.2f | Length %d\n",
                        cl.getCloudletId(),
                        cl.getVmId(),
                        cl.getExecStartTime(),
                        cl.getFinishTime(),
                        cl.getCloudletLength()
                );
            }


            // 模拟实际最大 finishTime
            double actualMakespan = results.stream()
                    .mapToDouble(Cloudlet::getFinishTime)
                    .max().orElse(0.0);

            System.out.println("🧪 实际模拟 Makespan = " + actualMakespan);


            EvaluationMetrics.Result result = EvaluationMetrics.evaluate(results);
            EvaluationMetrics.print(result, "PSO"); // 或 "RR"


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
