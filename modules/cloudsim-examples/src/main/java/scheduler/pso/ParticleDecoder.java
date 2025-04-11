package scheduler.pso;


/**
 * @Author: Chen
 * @File Name: ParticleDecoder.java
 */


import java.util.Random;

public class ParticleDecoder {

    private static final Random random = new Random();

    /**
     * 将 cloudlet × vm 向量解码为分配表
     */
    public static int[] decodeSoftmax(double[] position, int numCloudlets, int numVms) {
        int[] assignment = new int[numCloudlets];
        for (int i = 0; i < numCloudlets; i++) {
            double[] vmPref = new double[numVms];
            System.arraycopy(position, i * numVms, vmPref, 0, numVms);
            assignment[i] = softmaxSample(vmPref);
        }
        return assignment;
    }

    public static int softmaxSample(double[] vmPreferences) {
        int numVms = vmPreferences.length;
        double[] expVals = new double[numVms];
        double sum = 0.0;
        for (int i = 0; i < numVms; i++) {
            expVals[i] = Math.exp(vmPreferences[i]); // 可以乘以温度 τ
            sum += expVals[i];
        }

        double r = random.nextDouble();
        double cumulative = 0.0;
        for (int i = 0; i < numVms; i++) {
            cumulative += expVals[i] / sum;
            if (r <= cumulative) return i;
        }
        return numVms - 1;
    }

    public static void printSoftmax(double[] vmPreferences) {
        int n = vmPreferences.length;
        double[] expVals = new double[n];
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            expVals[i] = Math.exp(vmPreferences[i]);
            sum += expVals[i];
        }
        System.out.print("Softmax Probabilities: ");
        for (int i = 0; i < n; i++) {
            System.out.printf("VM%d=%.3f ", i, expVals[i] / sum);
        }
        System.out.println();
    }
}
