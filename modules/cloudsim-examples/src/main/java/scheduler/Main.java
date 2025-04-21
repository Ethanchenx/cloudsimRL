package scheduler;

import org.cloudbus.cloudsim.DatacenterBroker;
import scheduler.brokers.DynamicRLBroker;
import scheduler.core.DynamicRLExample;
import scheduler.core.RRExample;
import scheduler.core.PSOExample;
import scheduler.model.CloudletConfig;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        PSOExample.run();

//        RRExample.run();


        DynamicRLExample dynamicRLExample = new DynamicRLExample();
        DynamicRLBroker.connectClient();
        for (int i = 0; i< CloudletConfig.ITERATION_NUMS; i++){
            DynamicRLExample.run();
        }
        dynamicRLExample.printResultList();
        DynamicRLBroker.closeClient();

    }

}
