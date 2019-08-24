/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.fuzzy;

import core.Connection;
import core.DTNHost;
import core.Duration;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 * <p> This is a routing algorithm based on fuzzy logic with 
 * two input variable, this scheme using contact duration and inter-contact
 * duration for input variable. Based on the probability density function to 
 * arrange the membership function using JFuzzyLogic for the fuzzy engine. 
 * This routing protocol provide efficiency in routing performance.
 * Improved with delegation forwarding scheme.
 * </p>
 * @author Gregorius Bima, Sanata Dharma University
 */
public class FuzzyDelegationForwardingRouter implements RoutingDecisionEngine{
    
    public static final String FCL_NAMES = "fcl";
    public static final String CONTACT_DURATION = "Contact_Duration";
    public static final String INTERCONTACT_DURATION = "Intercontact_Duration";
    public static final String TRANSFER_OF_UTILITY = "TRANSFER_OF_UTILITY";
    public static final String MESSAGE_UTILITY = "value";
    /**
     * Fuzzy Control Language
     */
    private FIS fcl;
    protected Map<DTNHost, Double> startEncounterTimestamps;
    protected Map<DTNHost, Double> startIntercontactTimestamps;
    /**
     * Encounter Time
     */
    protected Map<DTNHost, List<Duration>> connHistory;
    /**
     * Non-Encounter Time
     */
    protected Map<DTNHost, List<Duration>> connIntercontactistory;
    
    public FuzzyDelegationForwardingRouter(Settings s) {
        String fclFromString = s.getSetting(FCL_NAMES);
        fcl = FIS.load(fclFromString);
    }

    public FuzzyDelegationForwardingRouter(FuzzyDelegationForwardingRouter prototpye) {
        this.fcl = prototpye.fcl;
        startEncounterTimestamps = new HashMap<>();
        startIntercontactTimestamps = new HashMap<>();
        connHistory = new HashMap<>();
        connIntercontactistory = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        double lastDisconnectTime = getLastDisconectedFor(peer);
        double currentTime = SimClock.getTime();

        this.startEncounterTimestamps.put(peer, currentTime);

        List<Duration> intercontactHistory;
        if (!connIntercontactistory.containsKey(peer)) {
            intercontactHistory = new LinkedList<Duration>();
        } else {
            intercontactHistory = connIntercontactistory.get(peer);
        }

        
        // add this connection to the list
        if (currentTime - lastDisconnectTime > 0) {
            intercontactHistory.add(new Duration(lastDisconnectTime, currentTime));
        }

        connIntercontactistory.put(peer, intercontactHistory);
        startIntercontactTimestamps.remove(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double lastEncounterTime = getLastEncounterFor(peer);
        double currentTime = SimClock.getTime();

        startIntercontactTimestamps.put(peer, currentTime);

        // Find or create the connection history list
        List<Duration> encounterHistory;
        if (!connHistory.containsKey(peer)) {
            encounterHistory = new LinkedList<Duration>();
        } else {
            encounterHistory = connHistory.get(peer);
        }
        

        // add this connection to the list
        if (currentTime - lastEncounterTime > 0) {
            encounterHistory.add(new Duration(lastEncounterTime, currentTime));
        }
        
        connHistory.put(peer, encounterHistory);

        startEncounterTimestamps.remove(peer);

    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        
    }

    @Override
    public boolean newMessage(Message m) {
        double myDestValue = this.defuzzification(m.getTo());
        m.addProperty(MESSAGE_UTILITY, myDestValue);
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
        FuzzyDelegationForwardingRouter peer = getOtherFuzzyRouter(otherHost);
        DTNHost messageDestination = m.getTo();

        double myDestValue = this.defuzzification(messageDestination);
        double peerDestValue = peer.defuzzification(messageDestination);

        if (m.getTo() == otherHost) {
            return true;
        }

        /** Delegation Forwarding */
        Double utilityValue = (Double) m.getProperty(MESSAGE_UTILITY);
        if (myDestValue < peerDestValue) {
            if (utilityValue < peerDestValue) {
                utilityValue = peerDestValue;
                m.updateProperty(MESSAGE_UTILITY, utilityValue);
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
        return new FuzzyDelegationForwardingRouter(this);
    }

    private FuzzyDelegationForwardingRouter getOtherFuzzyRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (FuzzyDelegationForwardingRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    /**
     * Compute defuzzification value for Message Destination
     * @param host Message Destination
     * @return crisp value 
     */
    private double defuzzification(DTNHost host) {
        double avgContactDuration = calculateAvgDuration(getListEncounterWith(host));
        double avgIntercontacDuration = calculateAvgIntercontact(getListIntercontactWith(host));
        FunctionBlock functionBlock = fcl.getFunctionBlock(null);

        functionBlock.setVariable(CONTACT_DURATION, avgContactDuration);
        functionBlock.setVariable(INTERCONTACT_DURATION, avgIntercontacDuration);
        functionBlock.evaluate();

        Variable transferOfUtility = functionBlock.getVariable(TRANSFER_OF_UTILITY);

        return transferOfUtility.getValue();
    }

    /**
     * Calculate average contact duration for a host
     * @param encounterDuration List contact duration
     * @return average value
     */
    private double calculateAvgDuration(List<Duration> encounterDuration) {
        Iterator<Duration> i = encounterDuration.iterator();
        double time = 0;
        while (i.hasNext()) {
            Duration d = i.next();
            time += d.end - d.start;
        }

        double avgDuration = time / encounterDuration.size();
        return avgDuration;
    }

    /**
     * Calculate average inter-contact duration for a host
     * @param intercontactDuration list inter-contact duration
     * @return average value
     */
    private double calculateAvgIntercontact(List<Duration> intercontactDuration) {
        Iterator<Duration> i = intercontactDuration.iterator();
        double time = 0;
        while (i.hasNext()) {
            Duration d = i.next();
            time += d.end - d.start;
        }

        double avgDuration = time / intercontactDuration.size();
        return avgDuration;
    }

    /**
     * Get the meet up contact duration during this connection 
     * @param host Peer of this connection
     * @return time
     */
    private double getLastEncounterFor(DTNHost host) {
        if (startEncounterTimestamps.containsKey(host)) {
            return startEncounterTimestamps.get(host);
        } else {
            return 0;
        }
    }

    /**
     * Get the time when connection is down during this connection 
     * @param host Peer of this connection
     * @return time
     */
    private double getLastDisconectedFor(DTNHost host) {
        if (startIntercontactTimestamps.containsKey(host)) {
            return startIntercontactTimestamps.get(host);
        } else {
            return 0;
        }
    }

    /**
     * Get the List of the connection history
     * @param host Peer of this connection
     * @return connection history
     */
    private List<Duration> getListEncounterWith(DTNHost host) {
        if (connHistory.containsKey(host)) {
            return connHistory.get(host);
        } else {
            List<Duration> encounterHistory = new LinkedList<>();
            return encounterHistory;
        }
    }

    /**
     * Get the List of the inter-contact history
     * @param host Peer of this connection
     * @return inter-contact history
     */
    private List<Duration> getListIntercontactWith(DTNHost host) {
        if (connIntercontactistory.containsKey(host)) {
            return connIntercontactistory.get(host);
        } else {
            List<Duration> intercontactHistory = new LinkedList<>();
            return intercontactHistory;
        }
    }
}
