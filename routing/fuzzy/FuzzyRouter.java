/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.fuzzy;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class FuzzyRouter implements RoutingDecisionEngine{

    public static final String FCL_NAMES = "fcl";
    public static final String CONTACT_DURATION = "contactDuration";
    public static final String INTERCONTACT_DURATION = "intercontactDuration";
    public static final String TRANSFER_OF_UTILITY = "TOU";
    private FIS fcl;
    protected Map<DTNHost, Double> meetings;
    protected Map<DTNHost, Double> disconnects;
    /** Encounter Time */
    protected Map<DTNHost, Double> contactDuration;
    /** Non-Encounter Time */
    protected Map<DTNHost, Double> intercontactDuration;
    /**
     * The fuzzy value from defuzzification 
     * for this scheme called Transfer of Utility (TOU)
     */
    protected Map<DTNHost, Double> TOU;
    
    public FuzzyRouter(Settings s){
        String fclFromString = s.getSetting(FCL_NAMES);
        fcl = FIS.load(fclFromString);
        meetings = new HashMap<>();
        intercontactDuration = new HashMap<>();
        disconnects = new HashMap<>();
        contactDuration = new HashMap<>();
        TOU = new HashMap<>();
    }
    
    public FuzzyRouter(FuzzyRouter prototype){
        this.fcl = prototype.fcl;
        meetings = new HashMap<>();
        intercontactDuration = new HashMap<>();
        disconnects = new HashMap<>();
        contactDuration = new HashMap<>();
        TOU = new HashMap<>();
    }
    
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        double currentTime = SimClock.getTime();
        /** Put the non encounter time with peer */
        double timeDiff = currentTime - getDisconnectFor(peer);
        this.intercontactDuration.put(peer, timeDiff);
        this.meetings.put(peer, currentTime);
        
        defuzzification(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double currentTime = SimClock.getTime();
        
        double timeDifferent = currentTime - getMeetingFor(peer);
        this.contactDuration.put(peer, timeDifferent);
        this.disconnects.put(peer, currentTime);
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
        FuzzyRouter peer = getOtherFuzzyRouter(otherHost);
        if (m.getTo() == otherHost) {
            return true;
        }
        return this.getTOUValueFor(m.getTo()) < peer.getTOUValueFor(m.getTo());
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
        return new FuzzyRouter(this);
    }
    
    private FuzzyRouter getOtherFuzzyRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (FuzzyRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
    
    private void defuzzification(DTNHost host){
        FunctionBlock functionBlock = fcl.getFunctionBlock(null);
        
        functionBlock.setVariable(CONTACT_DURATION, getEncounterFor(host));
        functionBlock.setVariable(INTERCONTACT_DURATION, getIntercontactFor(host));
        functionBlock.evaluate();
        
        Variable transferOfUtility = functionBlock.getVariable(TRANSFER_OF_UTILITY);
        TOU.put(host, transferOfUtility.getValue());
    }
    
    /**
     * Get utility value for host
     * @param host
     * @return The utility value for host
     */
    private double getTOUValueFor(DTNHost host) {
        if (TOU.containsKey(host)) {
            return TOU.get(host);
        } else {
            return 0;
        }
    }
    
    /**
     * Get last record contact duration for host
     * @param host
     * @return The last record of contact duration
     */
    private double getMeetingFor(DTNHost host){
        if (meetings.containsKey(host)) {
            return meetings.get(host);
        } else {
            return 0;
        }
    }
    
    /**
     * Get input variable contact duration for host
     * @param host
     * @return Input variable contact duration  
     */
    private double getEncounterFor(DTNHost host){
        if (contactDuration.containsKey(host)) {
            return contactDuration.get(host);
        } else {
            return 0;
        }
    }
    
    /**
     * Get input variable inter-contact duration for host
     * @param host
     * @return Input variable inter-contact duration
     */
    private double getIntercontactFor(DTNHost host){
        if (intercontactDuration.containsKey(host)) {
            return intercontactDuration.get(host);
        } else {
            return 0;
        }
    }
    
    /**
     * Get last record inter-contact duration for host
     * @param host
     * @return the last record of inter-contact duration
     */
    private double getDisconnectFor(DTNHost host){
        if (disconnects.containsKey(host)) {
            return disconnects.get(host);
        } else {
            return 0;
        }
    }
    
}
