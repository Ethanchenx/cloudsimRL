package scheduler.model;


/**
 * @Author: Chen
 * @File Name: CloudletConfig.java
 */

public class CloudletConfig {
    // 任务总数
    public static final int ITERATION_NUMS = 10;
    public static final int NUM_CLOUDLETS = 100;
    public static String DATASET_NAME = "GoCJ";
    public static String DATASET_PATH = String.format("modules/cloudsim-examples/src/main/java/scheduler/dataset/%s/%s_Dataset_%d.txt", DATASET_NAME, DATASET_NAME, NUM_CLOUDLETS);

    // 默认任务参数
    public static final long CLOUDLET_LENGTH = 50000;
    public static final int CLOUDLET_PES = 1;
    public static final long CLOUDLET_FILESIZE = 300;
    public static final long CLOUDLET_OUTPUTSIZE = 300;

    // 是否使用随机长度任务
    public static final boolean RANDOM_LENGTH = true;
    public static final int RANDOM_DELTA = 30000; // ±值
    public static final long MIN_LENGTH = 1000;   // 随机最小值

    // 随机种子（调试可控）
    public static final int SEED = 42;
}
