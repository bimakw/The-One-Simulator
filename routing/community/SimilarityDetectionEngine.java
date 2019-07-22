/*
 * @(#)CommunityDetection.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing.community;

//import java.util.*;

import core.*;

/**
 * Declares a RoutingDecisionEngine object to also perform similarity detection
 * in some fashion. This is needed for Community Detection Reports that need
 * to print out the communities detected by each node and possibly other 
 * classes that want the community of a given node. 
 * 
 * @author PJ Dillon, University of Pittsburgh
 */
public interface SimilarityDetectionEngine
{
	/**
	 * Mengembalikan nilai Similarity dari ego node ke node dest
	 */
	public double getSimilarity(DTNHost dest);
}
