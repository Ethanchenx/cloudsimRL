package scheduler;

import scheduler.core.DynamicRLExample;
import scheduler.core.RRExample;
import scheduler.core.PSOExample;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        PSOExample.run();

//        RRExample.run();

        DynamicRLExample dynamicRLExample = new DynamicRLExample();
        for (int i = 0; i<10; i++){
            DynamicRLExample.run();
        }
        dynamicRLExample.printResultList();

    }

}
