package scheduler.brokers;


/**
*@Author: Chen
*@File Name: DynamicRLBroker.java
*/


import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import scheduler.model.VmConfig;
import scheduler.rl.RLClient;
import scheduler.rl.RLRewardCalculator;
import scheduler.rl.RLStateEncoder;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class DynamicRLBroker extends DatacenterBroker {

    private RLClient rlClient;
    private Queue<Cloudlet> taskQueue = new LinkedList<>();

    private Double[] vmCosts = new Double[VmConfig.VM_NUMS] ;
    private Double postImbalanceRate;

    public DynamicRLBroker(String name) throws Exception {
        super(name);
        Arrays.fill(vmCosts, 0.0);
        postImbalanceRate = -1.0;
    }

    @Override
    protected void submitCloudlets() {

        // 将 Cloudlet 缓存并清空 CloudletList
        taskQueue.addAll(getCloudletList());
        getCloudletList().clear();

        try {
            rlClient = new RLClient("localhost", 5678);
        } catch (Exception e) {
            System.err.println("⚠️ 无法连接 RL 服务: " + e.getMessage());
        }

        // 启动首次调度
//        scheduleNext();
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
        Log.printLine(CloudSim.clock() + ": " + this.getName() + ": Cloudlet " + cloudlet.getCloudletId() + " received");
        --this.cloudletsSubmitted;

        int guestId = cloudlet.getGuestId();
        double cloudletExecTime = cloudlet.getExecFinishTime() - cloudlet.getExecStartTime();
        vmCosts[guestId] = cloudletExecTime * (VmConfig.COST_C1[guestId] + VmConfig.COST_C2[guestId] + VmConfig.COST_C3[guestId]);


        if (this.cloudletsSubmitted == 0) {
            Log.printLine(CloudSim.clock() + ": " + this.getName() + ": All Cloudlets executed. Finishing...");
            this.clearDatacenters();
            this.finishExecution();
            try {
                rlClient.close(); // 关闭连接
                System.out.println("✅ RL Client connection closed.");
            } catch (IOException e) {
                System.err.println("⚠️ RL Client connection failed to close.");
            }
        }


        if (!taskQueue.isEmpty()) {
            Double reward;
            if (postImbalanceRate != -1){
                reward = RLRewardCalculator.calculateReward(getGuestsCreatedList(), vmCosts, cloudlet, postImbalanceRate);

                try {
                    rlClient.sendReward(reward);
                } catch (Exception e) {
                    System.err.println("⚠️ reward err");
                }
            }
            scheduleNext();
        }
    }

    private void scheduleNext() {
        // 确保任务队列不为空
        if (taskQueue.isEmpty()) return;

        Cloudlet c = taskQueue.poll();

        // 获取当前 VM 状态（负载）
        List<Double> state = RLStateEncoder.buildVmsState(getGuestsCreatedList(), vmCosts);
        postImbalanceRate = state.getLast();

        int selectedVm = 0;

        // 使用 RL 服务返回的动作来选择 VM
        try {
            selectedVm = rlClient.getAction(state, c.getCloudletId());
            System.out.printf("📤 Cloudlet %d → VM %d\n", c.getCloudletId(), selectedVm);
        } catch (Exception e) {
            System.err.println("⚠️ 使用默认策略 (VM 0)");
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
