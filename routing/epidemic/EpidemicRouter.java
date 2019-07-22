/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.epidemic;

import core.*;
import java.util.*;
import report.DurationCalculable;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public class EpidemicRouter implements RoutingDecisionEngine, DurationCalculable {

    protected Map<DTNHost, Double> meetings;
    protected Map<DTNHost, Double> disconnects;
    /**
     * Encounter Time
     */
    protected Map<DTNHost, ArrayList<Double>> ENT;
    /**
     * Non Encounter Time
     */
    protected Map<DTNHost, ArrayList<Double>> NENT;

    public EpidemicRouter(Settings s) {
        meetings = new HashMap<>();
        NENT = new HashMap<>();
        disconnects = new HashMap<>();
        ENT = new HashMap<>();
    }

    public EpidemicRouter(EpidemicRouter prototype) {
        meetings = new HashMap<>();
        NENT = new HashMap<>();
        disconnects = new HashMap<>();
        ENT = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        double currentTime = SimClock.getTime();

        ArrayList<Double> contactDuration = getDataIntercontactFor(peer);
        double timeDiff = currentTime - getDisconnectFor(peer);
        contactDuration.add(timeDiff);
        this.NENT.put(peer, contactDuration);
        this.meetings.put(peer, currentTime);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double currentTime = SimClock.getTime();

        ArrayList<Double> contactDuration = getDataEncounterFor(peer);
        double timeDifferent = currentTime - getMeetingFor(peer);
        this.disconnects.put(peer, currentTime);
        contactDuration.add(timeDifferent);
        this.ENT.put(peer, contactDuration);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        return false;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return !thisHost.getRouter().hasMessage(m.getId());
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return true;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    private double getMeetingFor(DTNHost host) {
        if (meetings.containsKey(host)) {
            return meetings.get(host);
        } else {
            return 0;
        }
    }

    private double getDisconnectFor(DTNHost host) {
        if (disconnects.containsKey(host)) {
            return disconnects.get(host);
        } else {
            return 0;
        }
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new EpidemicRouter(this);
    }

    private ArrayList<Double> getDataEncounterFor(DTNHost host) {
        if (ENT.containsKey(host)) {
            return ENT.get(host);
        } else {
            ArrayList<Double> contacDuration = new ArrayList<>();
            return contacDuration;
        }
    }

    private ArrayList<Double> getDataIntercontactFor(DTNHost host) {
        if (NENT.containsKey(host)) {
            return NENT.get(host);
        } else {
            ArrayList<Double> contacDuration = new ArrayList<>();
            return contacDuration;
        }
    }

    @Override
    public Map<DTNHost, ArrayList<Double>> getContactDuration() {
        return this.ENT;
    }

    @Override
    public Map<DTNHost, ArrayList<Double>> getIntercontactDuration() {
        return this.NENT;
    }
}
