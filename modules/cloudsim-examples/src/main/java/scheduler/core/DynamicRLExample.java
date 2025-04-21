package scheduler.core;


/**
 * @Author: Chen
 * @File Name: DynamicRLExample.java
 */


import com.opencsv.CSVWriter;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import scheduler.brokers.DynamicRLBroker;
import scheduler.env.CloudletFactory;
import scheduler.env.DataCenterFactory;
import scheduler.env.VmFactory;
import scheduler.eval.EvaluationMetrics;
import scheduler.model.CloudletConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DynamicRLExample {
    private static List<EvaluationMetrics.Result> resultList;
    private static String resultOutputFileName = "modules/cloudsim-examples/src/main/java/scheduler/results/output.csv";

    public DynamicRLExample() {
        resultList = new ArrayList<>();
    };

    public static void run() {
        try {
            // 初始化 CloudSim
            CloudSim.init(1, Calendar.getInstance(), false);

            // 创建数据中心
            Datacenter datacenter = DataCenterFactory.createSimpleDatacenter("Datacenter_DRL");

            // 创建 Broker（不需要 Cloudlet 和 VM 参数）
            DynamicRLBroker broker = new DynamicRLBroker("DRL_Broker");
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
            System.out.println("\n=== DRL 调度结果 ===");
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


            EvaluationMetrics.Result result = EvaluationMetrics.evaluate(results);
            EvaluationMetrics.print(result, "DRL");
            resultList.add(result);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void printResultList() throws IOException {
        // 打开 CSV 文件进行写入
        CSVWriter writer = new CSVWriter(new FileWriter(resultOutputFileName));

        // 写入表头
        String[] header = {"Iteration", "Makespan", "TotalCost", "Utilization", "Imbalance"};
        writer.writeNext(header);

        // 遍历 resultList 并写入每个结果
        for (int i = 0; i < resultList.size(); i++) {
            List<String> data = new ArrayList<>();
            EvaluationMetrics.Result result = resultList.get(i);

            // 将每个 result 的字段添加到数据列表
            data.add(String.valueOf((i+1) * CloudletConfig.NUM_CLOUDLETS));
            data.add(String.valueOf(result.makespan));
            data.add(String.valueOf(result.totalCost));
            data.add(String.valueOf(result.utilization));
            data.add(String.valueOf(result.imbalance));

            // 写入数据行
            writer.writeNext(data.toArray(new String[0]));

            // 你还可以根据需要输出 EvaluationMetrics 的其他信息
            EvaluationMetrics.print(result, String.format("Iteration %d", (i+1) * CloudletConfig.NUM_CLOUDLETS));
        }

        // 关闭 CSVWriter
        writer.close();
    }
}
