/*
 * @(#)Centrality.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing.community;

import core.DTNHost;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Abstracts the concept of a centrality computation algorithm (where Centrality
 * is defined in the context of a social network). For the purposes of routing
 * protocols like Distributed Bubble Rap, centrality must be computed globally,
 * using the history of all previous contacts, and locally, using only the
 * contact history of those hosts in some local community, where the community
 * is defined by some community detection algorithm.
 * </p>
 * 
 * <p>
 * In this way, the Centrality interface semantically requires any class
 * employing one of its subclasses to keep track of the connection history of
 * the node at which these instancces are stored. To use the local centrality
 * computation, the using object would also have to create and use a
 * CommunityDetection instance. As of right now,
 * {@link DistributedBubbleRap} is the only class that does
 * this.
 * </p>
 *
 * @author PJ Dillon, University of Pittsburgh
 */
public interface Matrix
{
	public double[][] getMatrix();

}
