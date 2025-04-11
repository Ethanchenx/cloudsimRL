package scheduler.model;


/**
 * @Author: Chen
 * @File Name: VmConfig.java
 */

public class VmConfig {
    public static final int VM_NUMS = 3;

    // 每台 VM 的差异化配置
    public static final int[] MIPS_LIST = {1000, 1500, 800};
    public static final int[] PES_LIST = {1, 1, 1};
    public static final int[] RAM_LIST = {2048, 4096, 1024};
    public static final int[] BW_LIST = {10000, 20000, 5000};
    public static final int[] SIZE_LIST = {100000, 150000, 80000};

    // 每台 VM 的定制单位资源成本
    public static final double[] COST_C1 = {0.001, 0.0015, 0.0008}; // CPU 时间权重
    public static final double[] COST_C2 = {0.002, 0.0025, 0.001};  // 内存权重
    public static final double[] COST_C3 = {0.001, 0.0015, 0.0005}; // 带宽权重


    // 虚拟机管理器类型
    public static final String VMM = "Xen";
}
