package scheduler.pso;


import net.sourceforge.jswarm_pso.Particle;

/**
 * @Author: Chen
 * @File Name: DiscreteParticleSoftmax.java
 */

public class DiscreteParticleSoftmax extends Particle {

    private static int numCloudlets;
    private static int numVms;

    public static void setDimensions(int c, int v) {
        numCloudlets = c;
        numVms = v;
    }

    public DiscreteParticleSoftmax() {
        super(numCloudlets * numVms);
        initialize();
    }

    private void initialize() {
        double[] pos = new double[numCloudlets * numVms];
        double[] vel = new double[numCloudlets * numVms];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = Math.random();
            vel[i] = 0.0;
        }
        setPosition(pos);
        setVelocity(vel);
    }
}


