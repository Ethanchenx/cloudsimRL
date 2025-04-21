package scheduler.env;


/**
 * @Author: Chen
 * @File Name: CloudletFactory.java
 */

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModelFull;
import scheduler.model.CloudletConfig;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CloudletFactory {

    public static List<Cloudlet> createCloudletList(int brokerId) {
        List<Cloudlet> list = new ArrayList<>();
        Random rand = new Random(CloudletConfig.SEED);

        if (Objects.equals(CloudletConfig.DATASET_NAME, "random")){
            for (int i = 0; i < CloudletConfig.NUM_CLOUDLETS; i++) {
                long length = CloudletConfig.CLOUDLET_LENGTH;
                if (CloudletConfig.RANDOM_LENGTH) {
                    long delta = rand.nextInt(CloudletConfig.RANDOM_DELTA * 2) - CloudletConfig.RANDOM_DELTA;
                    length = Math.max(CloudletConfig.MIN_LENGTH, length + delta);
                }

                Cloudlet cl = new Cloudlet(
                        i,
                        length,
                        CloudletConfig.CLOUDLET_PES,
                        CloudletConfig.CLOUDLET_FILESIZE,
                        CloudletConfig.CLOUDLET_OUTPUTSIZE,
                        new UtilizationModelFull(),
                        new UtilizationModelFull(),
                        new UtilizationModelFull()
                );

                cl.setUserId(brokerId);
                list.add(cl);
            }
        }
        else {   // using dataset
            List<Integer> lengthList = new ArrayList<>();

            try (FileReader reader = new FileReader(CloudletConfig.DATASET_PATH);
                 BufferedReader br = new BufferedReader(reader) // 建立一个对象，它把文件内容转成计算机能读懂的语言
            ) {
                String line;
                //网友推荐更加简洁的写法
                while ((line = br.readLine()) != null) {
                    // 一次读入一行数据
                    Integer val = Integer.parseInt(line);
                    lengthList.add(val);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < CloudletConfig.NUM_CLOUDLETS; i++) {
                Cloudlet cl = new Cloudlet(
                        i,
                        lengthList.get(i),
                        CloudletConfig.CLOUDLET_PES,
                        CloudletConfig.CLOUDLET_FILESIZE,
                        CloudletConfig.CLOUDLET_OUTPUTSIZE,
                        new UtilizationModelFull(),
                        new UtilizationModelFull(),
                        new UtilizationModelFull()
                );

                cl.setUserId(brokerId);
                list.add(cl);
            }
        }


        return list;
    }
}
