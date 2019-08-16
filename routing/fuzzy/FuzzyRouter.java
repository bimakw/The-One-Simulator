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
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class FuzzyRouter implements RoutingDecisionEngine {

    public static final String FCL_NAMES = "fcl";
    public static final String CONTACT_DURATION = "contactDuration";
    public static final String INTERCONTACT_DURATION = "intercontactDuration";
    public static final String TRANSFER_OF_UTILITY = "TOU";
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
    
    public FuzzyRouter(Settings s) {
        String fclFromString = s.getSetting(FCL_NAMES);
        fcl = FIS.load(fclFromString);
    }

    public FuzzyRouter(FuzzyRouter prototpye) {
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
        DTNHost messageDestination = m.getTo();

        double myDestValue = this.defuzzification(messageDestination);
        double peerDestValue = peer.defuzzification(messageDestination);

        if (m.getTo() == otherHost) {
            return true;
        }

        return myDestValue < peerDestValue;
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

    private double getLastEncounterFor(DTNHost host) {
        if (startEncounterTimestamps.containsKey(host)) {
            return startEncounterTimestamps.get(host);
        } else {
            return 0;
        }
    }

    private double getLastDisconectedFor(DTNHost host) {
        if (startIntercontactTimestamps.containsKey(host)) {
            return startIntercontactTimestamps.get(host);
        } else {
            return 0;
        }
    }

    private List<Duration> getListEncounterWith(DTNHost host) {
        if (connHistory.containsKey(host)) {
            return connHistory.get(host);
        } else {
            List<Duration> encounterHistory = new LinkedList<>();
            return encounterHistory;
        }
    }

    private List<Duration> getListIntercontactWith(DTNHost host) {
        if (connIntercontactistory.containsKey(host)) {
            return connIntercontactistory.get(host);
        } else {
            List<Duration> intercontactHistory = new LinkedList<>();
            return intercontactHistory;
        }
    }

}
