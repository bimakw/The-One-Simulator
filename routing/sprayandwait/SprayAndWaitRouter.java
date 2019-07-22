/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.sprayandwait;

import core.*;
import java.util.*;
import routing.RoutingDecisionEngine;
import routing.RoutingDecisionEngineImproved;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public class SprayAndWaitRouter implements RoutingDecisionEngineImproved{

    public static final String NROF_COPIES_S = "nrofCopies";
    public static final String MSG_COUNT_PROP = "copies";
    public static final String BINARY_MODE = "binaryMode";
    
    protected int initialNrofCopies;
    protected boolean isBinary;
    
    public SprayAndWaitRouter(Settings s){
        if (s.contains(BINARY_MODE)) {
            isBinary = s.getBoolean(BINARY_MODE);
        } else {
            isBinary = true;
        }
        
        if (s.contains(NROF_COPIES_S)) {
            initialNrofCopies = s.getInt(NROF_COPIES_S);
        } else {
            initialNrofCopies = 6;
        }
        
    }
    
    public SprayAndWaitRouter(SprayAndWaitRouter prototype){
        isBinary = prototype.isBinary;
        initialNrofCopies = prototype.initialNrofCopies;
    }
    

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROP, new Integer(initialNrofCopies));
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        
        if (isBinary) {
            nrofCopies = (int) Math.ceil(nrofCopies/2.0);
        } else {
            nrofCopies = 1;
        }
        
        m.updateProperty(MSG_COUNT_PROP, nrofCopies);
        
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return true;
        }
        
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        
        if (nrofCopies>1) {
            return true;
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
    public void update(DTNHost host) {}

    @Override
    public void transferDone(Connection con) {
        Message m = con.getMessage();
        
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        
        if (isBinary) {
            nrofCopies /= 2;
        } else {
            nrofCopies--;
        }
        
        m.updateProperty(MSG_COUNT_PROP, nrofCopies);
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SprayAndWaitRouter(this);
    }

}
