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
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import scheduler.rl.RLClient;
import scheduler.rl.RLStateEncoder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class DynamicRLBroker extends DatacenterBroker {

    private RLClient rlClient;
    private Queue<Cloudlet> taskQueue = new LinkedList<>();

    public DynamicRLBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        // 确保提交 VM 列表
        submitGuestList(getGuestList());

        // 将 Cloudlet 缓存并清空 CloudletList
        taskQueue.addAll(getCloudletList());
        getCloudletList().clear();

        try {
            rlClient = new RLClient("localhost", 5555);
        } catch (Exception e) {
            System.err.println("⚠️ 无法连接 RL 服务: " + e.getMessage());
        }

        // 启动首次调度
        scheduleNext();
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
//        super.processCloudletReturn(ev);
//


        Cloudlet cloudlet = (Cloudlet)ev.getData();
        this.getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + this.getName() + ": Cloudlet " + cloudlet.getCloudletId() + " received");
        --this.cloudletsSubmitted;
        if (taskQueue.isEmpty()) {
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


        // 📈 可以在这里计算 reward 并传给 RL（后续扩展）
        scheduleNext();
    }

    private void scheduleNext() {
        // 确保任务队列不为空
        if (taskQueue.isEmpty()) return;

        Cloudlet c = taskQueue.poll();

        // 获取当前 VM 状态（负载）
        double[] state = RLStateEncoder.buildVmLoadState(getGuestsCreatedList(), getCloudletSubmittedList());

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
