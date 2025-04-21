package scheduler.brokers;


/**
*@Author: Chen
*@File Name: DynamicRLBroker.java
*/


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import scheduler.eval.EvaluationMetrics;
import scheduler.model.CloudletConfig;
import scheduler.model.VmConfig;
import scheduler.rl.RLClient;
import scheduler.rl.RLRewardCalculator;
import scheduler.rl.RLStateEncoder;

import java.io.IOException;
import java.util.*;


public class DynamicRLBroker extends DatacenterBroker {

    private static RLClient rlClient;
    private Queue<Cloudlet> taskQueue = new LinkedList<>();

    private Double[] vmCosts = new Double[VmConfig.VM_NUMS] ;

    public DynamicRLBroker(String name) throws Exception {
        super(name);
        Arrays.fill(vmCosts, 0.0);
    }

    public static void connectClient(){
        try {
            rlClient = new RLClient("localhost", 5678);
        } catch (Exception e) {
            System.err.println("⚠️ 无法连接 RL 服务: " + e.getMessage());
        }

        try {
            rlClient.sendConfig(VmConfig.VM_NUMS, CloudletConfig.NUM_CLOUDLETS, CloudletConfig.ITERATION_NUMS, CloudletConfig.DATASET_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeClient(){
        try {
            rlClient.close(); // 关闭连接
            System.out.println("✅ RL Client connection closed.");
        } catch (IOException e) {
            System.err.println("⚠️ RL Client connection failed to close.");
        }
    }

    @Override
    protected void submitCloudlets() {
        // 将 Cloudlet 缓存并清空 CloudletList
        taskQueue.addAll(getCloudletList());
        getCloudletList().clear();

        // 启动首次调度
        for (int i=0; i< VmConfig.VM_NUMS; i++){
            Cloudlet c = taskQueue.poll();
            int selectedVm = i;
            c.setVmId(i);
            send(
                    getVmsToDatacentersMap().get(0),
                    0.0,
                    CloudActionTags.CLOUDLET_SUBMIT,
                    c
            );
            cloudletsSubmitted++;
        }

    }


    @Override
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet)ev.getData();
        this.getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + this.getName() + ": Cloudlet " + cloudlet.getCloudletId() + " Completed");
        --this.cloudletsSubmitted;

        int guestId = cloudlet.getGuestId();
        double cloudletExecTime = cloudlet.getExecFinishTime() - cloudlet.getExecStartTime();
        vmCosts[guestId] += cloudletExecTime * (VmConfig.COST_C1[guestId] + VmConfig.COST_C2[guestId] + VmConfig.COST_C3[guestId]);

        if (taskQueue.isEmpty()) {
            Log.printLine(CloudSim.clock() + ": " + this.getName() + ": All Cloudlets executed. Finishing...");
            this.clearDatacenters();
            this.finishExecution();
        }
        else {
            scheduleNext();
        }
    }

    private void scheduleNext() {
        // 确保任务队列不为空
        if (taskQueue.isEmpty()) return;

        Cloudlet c = taskQueue.poll();

        // 获取当前 VM 状态（负载）
        List<Double> state = RLStateEncoder.buildVmsState(getGuestsCreatedList(), vmCosts, c);
        List<Double> estimateRuntime = new ArrayList<>();
        for (int i = 0; i < vmList.size(); i++){
            estimateRuntime.add(i, c.getCloudletLength()/vmList.get(i).getMips());
        }

        int selectedVm = 0;
        // 使用 RL 服务返回的动作来选择 VM
        try {
            selectedVm = rlClient.getAction(state, c.getCloudletLength(), c.getCloudletId(), estimateRuntime);
            System.out.printf("Cloudlet %d → VM %d\n", c.getCloudletId(), selectedVm);
        } catch (Exception e) {
            System.err.println("使用默认策略 (VM 0)");
        }

        // 设置 VM ID 并提交 Cloudlet
        c.setVmId(selectedVm);
        send(
                getVmsToDatacentersMap().get(selectedVm),
                0.0,
                CloudActionTags.CLOUDLET_SUBMIT,
                c
        );
        cloudletsSubmitted++;
    }
}
