/*
 * @(#)CommunityDetection.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing.community;

//import core.*;


public interface SimilarityCounter
{
//	public Map<DTNHost, Double> countSimilarity(Map<DTNHost, Set<DTNHost>> neighboursNode,
//			DTNHost myHost, double[][] matrixEgoNetwork);
	
//	public double countSimilarity(Map<DTNHost, Set<DTNHost>> neighboursNode,
//			DTNHost dest);
	
	public double countSimilarity(double[][] matrixEgoNetwork, double[][] matrixIndirectNode, int index);
	
	public SimilarityCounter replicate();
}
