/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.decisionengine;

import java.util.*;
import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class FairRoutingRouter implements RoutingDecisionEngine {

    public static final String ROUTER_SIGMA = "shortTermR";
    public static final String ROUTER_LAMBDA = "longTermR";
    public static String SIGMA = "Sigma";
    public static String LAMBDA = "Lambda";
    public static String TIME = "Time";

    private Map<DTNHost, Map<String, Double>> nodeHistory;
    private Map<DTNHost, Double> utilityForDestination;
    private double routerSigma;
    private double routerLambda;

    public FairRoutingRouter(Settings s) {
        if (s.contains(ROUTER_SIGMA)) {
            this.routerSigma = s.getDouble(ROUTER_SIGMA);
        } else {
            this.routerSigma = 0.6; //default
        }
        if (s.contains(ROUTER_LAMBDA)) {
            this.routerLambda = s.getDouble(ROUTER_LAMBDA);
        } else {
            this.routerLambda = 0.5; //default
        }
        nodeHistory = new HashMap<>();
        utilityForDestination = new HashMap<>();
    }

    protected FairRoutingRouter(FairRoutingRouter proto) {
        this.routerSigma = proto.routerSigma;
        this.routerLambda = proto.routerLambda;
        nodeHistory = new HashMap<>();
        utilityForDestination = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        double thisTime = SimClock.getTime();
        if (!this.nodeHistory.containsKey(peer)) {
            Map<String, Double> aggregatedComponent = new HashMap<>();
            aggregatedComponent.put(LAMBDA, 1.0);
            aggregatedComponent.put(SIGMA, 1.0);
            aggregatedComponent.put(TIME, thisTime);
            this.nodeHistory.put(peer, aggregatedComponent);
        } else {
            Map<String, Double> aggregatedComponent;
            for (Map.Entry<DTNHost, Map<String, Double>> entry : nodeHistory.entrySet()) {
                DTNHost key = entry.getKey();
                aggregatedComponent = entry.getValue();

                double sigmaBefore = aggregatedComponent.get(SIGMA);
                double lambdaBefore = aggregatedComponent.get(LAMBDA);
                if (key == peer) {
                    aggregatedComponent.put(SIGMA, sigmaBefore + 1);
                    aggregatedComponent.put(LAMBDA, lambdaBefore + 1);
                    aggregatedComponent.put(TIME, thisTime);
                    this.nodeHistory.put(key, aggregatedComponent);
                } else {
                    double timeBefore = aggregatedComponent.get(TIME);
                    double thisLambda = lambdaBefore * (Math.pow(Math.E, (-(this.routerLambda) * (thisTime - timeBefore))));
                    double thisSigma = sigmaBefore * (Math.pow(Math.E, (-(this.routerSigma) * (thisTime - timeBefore))));

                    aggregatedComponent.put(LAMBDA, thisLambda);
                    aggregatedComponent.put(SIGMA, thisSigma);

                    this.nodeHistory.put(key, aggregatedComponent);
                }
            }
        }

        updatePerceiveInteractionStrengths(thisHost, peer);
    }

    private void updatePerceiveInteractionStrengths(DTNHost thisHost, DTNHost peer) {
        FairRoutingRouter partner = getOtherFairRoutingRouter(peer);

        for (Message m : thisHost.getMessageCollection()) {
            double myLambda = 0, mySigma = 0;
            double partnerLambda = 0, partnerSigma = 0;
            double utility = 0;
            if (this.nodeHistory.containsKey(m.getTo())) {
                myLambda = this.nodeHistory.get(m.getTo()).get(LAMBDA);
                mySigma = this.nodeHistory.get(m.getTo()).get(SIGMA);
            }
            if (partner.nodeHistory.containsKey(m.getTo())) {
                partnerLambda = partner.nodeHistory.get(m.getTo()).get(LAMBDA);
                partnerSigma = partner.nodeHistory.get(m.getTo()).get(SIGMA);
            }

            if ((myLambda + partnerLambda) > 0) {

                double myInteractStrength = this.countAgrIntStrength(myLambda, mySigma);
                double partnerInteractStrength = partner.countAgrIntStrength(partnerLambda, partnerSigma);

                utility = this.countUtilityThroughPeer(partnerInteractStrength, myInteractStrength);
                this.utilityForDestination.put(m.getTo(), utility);
            } else {
                this.utilityForDestination.put(m.getTo(), 0.0);
            }

        }
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        FairRoutingRouter partner = getOtherFairRoutingRouter(otherHost);
        DTNHost thisHost = null;
        if (m.getTo() == otherHost) {
            return true;
        }

        List<DTNHost> listHopCounts = m.getHops();
        Iterator it = listHopCounts.iterator();
        while (it.hasNext()) {
            thisHost = (DTNHost) it.next();
        }
        
        double myLambda = this.getLambdaFor(m.getTo());
        double partnerLambda = partner.getLambdaFor(m.getTo());

        if (getUtilityThroughPeerForDest(m.getTo()) > 0.5) {
            if ((myLambda + partnerLambda) > 0) {
                if (otherHost.getBufferOccupancy() >= thisHost.getBufferOccupancy()) {
                    return true;
                }
            }
        }

        if (getUtilityThroughPeerForDest(m.getTo()) == 1) {
            if ((myLambda + partnerLambda) > 0) {
                return true;
            }
        }
        
        if (getUtilityPeerForAny(m.getTo()) > 0.5) {
            if (myLambda + partnerLambda == 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new FairRoutingRouter(this);
    }

    private double getLambdaFor(DTNHost messageDestination){
        if (this.nodeHistory.containsKey(messageDestination)) {
            return this.nodeHistory.get(messageDestination).get(LAMBDA);
        } else {
            return 0;
        }
    }
    private double countAgrIntStrength(double lambda, double sigma) {
        return lambda * (lambda - sigma);
    }

    private double countUtilityThroughPeer(double partnerInteractStrength, double myInteractStrength) {
        return partnerInteractStrength / (partnerInteractStrength + myInteractStrength);
    }

    private double getUtilityThroughPeerForDest(DTNHost messageDestination) {
        if (utilityForDestination.containsKey(messageDestination)) {
            return utilityForDestination.get(messageDestination);
        } else {
            return 0;
        }
    }

    private double getUtilityPeerForAny(DTNHost peer) {
        FairRoutingRouter partner = getOtherFairRoutingRouter(peer);
        double totalPeerForOther = 0;
        double totalThisHostForOther = 0;

        for (Map.Entry<DTNHost, Map<String, Double>> entry : partner.nodeHistory.entrySet()) {
            Map<String, Double> value = entry.getValue();
            double otherLambda = value.get(LAMBDA);
            double otherSigma = value.get(SIGMA);
            double peerForOther = countAgrIntStrength(otherLambda, otherSigma);
            totalPeerForOther = totalPeerForOther + peerForOther;
        }

        for (Map.Entry<DTNHost, Map<String, Double>> entry : this.nodeHistory.entrySet()) {
            Map<String, Double> value = entry.getValue();
            double otherLambda = value.get(LAMBDA);
            double otherSigma = value.get(SIGMA);
            double thisHostForOther = countAgrIntStrength(otherLambda, otherSigma);
            totalThisHostForOther = totalThisHostForOther + thisHostForOther;
        }

        return totalPeerForOther / (totalPeerForOther + totalThisHostForOther);
    }

    private FairRoutingRouter getOtherFairRoutingRouter(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (FairRoutingRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
}
