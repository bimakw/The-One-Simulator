/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.decisionengine;

import core.*;
import java.util.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class ProphetRouterWithDelegationForwarding implements RoutingDecisionEngine {

    public static final String MESSAGE_UTILITY = "value";
    protected final static String BETA_SETTING = "beta";
    protected final static String P_INIT_SETTING = "initial_p";
    protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

    protected static final double DEFAULT_P_INIT = 0.75;
    protected static final double GAMMA = 0.92;
    protected static final double DEFAULT_BETA = 0.45;
    protected static final int DEFAULT_UNIT = 30;

    protected double beta;
    protected double pinit;
    protected double lastAgeUpdate;
    protected int secondsInTimeUnit;

    /** Delivery Predictabilities */
    private Map<DTNHost, Double> predictabilities;

    public ProphetRouterWithDelegationForwarding(Settings s) {
        if (s.contains(BETA_SETTING)) {
            beta = s.getDouble(BETA_SETTING);
        } else {
            beta = DEFAULT_BETA;
        }

        if (s.contains(P_INIT_SETTING)) {
            pinit = s.getDouble(P_INIT_SETTING);
        } else {
            pinit = DEFAULT_P_INIT;
        }

        if (s.contains(SECONDS_IN_UNIT_S)) {
            secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
        } else {
            secondsInTimeUnit = DEFAULT_UNIT;
        }

        predictabilities = new HashMap<>();
        this.lastAgeUpdate = 0.0;
    }

    public ProphetRouterWithDelegationForwarding(ProphetRouterWithDelegationForwarding de) {
        beta = de.beta;
        pinit = de.pinit;
        secondsInTimeUnit = de.secondsInTimeUnit;
        predictabilities = new HashMap<>();
        this.lastAgeUpdate = de.lastAgeUpdate;
    }

    public RoutingDecisionEngine replicate() {
        return new ProphetRouterWithDelegationForwarding(this);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ProphetRouterWithDelegationForwarding partner = getOtherProphetRouter(peer);
        Set<DTNHost> hostSet = new HashSet<DTNHost>(this.predictabilities.size()
                + partner.predictabilities.size());
        hostSet.addAll(this.predictabilities.keySet());
        hostSet.addAll(partner.predictabilities.keySet());

        this.agePreds();
        partner.agePreds();

        // Update preds for this connection
        double myOldValue = this.getPredFor(peer),
                peerOldValue = partner.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * partner.pinit;
        predictabilities.put(peer, myPforHost);
        partner.predictabilities.put(myHost, peerPforMe);

        // Update transistivities
        for (DTNHost h : hostSet) {
            myOldValue = 0.0;
            peerOldValue = 0.0;

            if (predictabilities.containsKey(h)) {
                myOldValue = predictabilities.get(h);
            }
            if (partner.predictabilities.containsKey(h)) {
                peerOldValue = partner.predictabilities.get(h);
            }

            if (h != myHost) {
                predictabilities.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != peer) {
                partner.predictabilities.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }
    }

    public boolean newMessage(Message m) {
        m.addProperty(MESSAGE_UTILITY, getPredFor(m.getTo()));
        return true;
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        ProphetRouterWithDelegationForwarding Partner = getOtherProphetRouter(otherHost);

        /** Delegation Forwarding */
        Double utilityValue = (Double) m.getProperty(MESSAGE_UTILITY);
        if (this.getPredFor(m.getTo()) < Partner.getPredFor(m.getTo())) {
            if (utilityValue < Partner.getPredFor(m.getTo())) {
                utilityValue = Partner.getPredFor(m.getTo());
                m.updateProperty(MESSAGE_UTILITY, utilityValue);
            return true;
            }
        } 
        return false;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    private ProphetRouterWithDelegationForwarding getOtherProphetRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (ProphetRouterWithDelegationForwarding) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : predictabilities.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    /**
     * Returns the current prediction (P) value for a host or 0 if entry for the
     * host doesn't exist.
     *
     * @param host The host to look the P for
     * @return the current P value
     */
    private double getPredFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (predictabilities.containsKey(host)) {
            return predictabilities.get(host);
        } else {
            return 0;
        }
    }

}
