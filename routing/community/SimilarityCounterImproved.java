package routing.community;

import core.DTNHost;

import java.util.ArrayList;
import java.util.Map;

public interface SimilarityCounterImproved {

    public double countSimilarity(double[][] matrixEgoNetwork,
                                  double[][] matrixIndirectNode,
                                  int index,
                                  Map<DTNHost,ArrayList<Double>> neighborsHistory);

    public double countAggrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) ;

    public SimilarityCounterImproved replicate();
}
