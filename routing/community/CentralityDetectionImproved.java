package routing.community;

import core.DTNHost;

import java.util.*;

public interface CentralityDetectionImproved {

    /**
     * Called when get Centrality for
     * compute aggregation interaction strength
     *
     * @param matrixEgoNetwork
     * @param neighborsHistory
     * @return
     */
    public double getCentrality(double[][] matrixEgoNetwork, Map<DTNHost, ArrayList<Double>> neighborsHistory);

    /**
     * replicate the Centrality Object
     *
     * @return
     */
    public CentralityDetectionImproved replicate();
}
