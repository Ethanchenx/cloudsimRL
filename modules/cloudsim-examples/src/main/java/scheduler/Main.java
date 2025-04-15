package scheduler;

import scheduler.core.DynamicRLExample;
import scheduler.core.RRExample;
import scheduler.core.PSOExample;

public class Main {
    public static void main(String[] args) {
//        PSOExample.run();
//        RRExample.run();
        DynamicRLExample dynamicRLExample = new DynamicRLExample();
        for (int i = 0; i<5; i++){
            DynamicRLExample.run();
        }
        dynamicRLExample.printResultList();

    }

}
