/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.epidemic;

import core.*;
import java.util.*;
import routing.RoutingDecisionEngineImproved;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class EpidemicGameOfLifeRouter implements RoutingDecisionEngineImproved {

    
    public static final String MSG_COUNTER_PROPERTY = "counter";
    public static final String COPY_THRESHOLD = "copyThreshold";
    public static final String DELETE_THRESHOLD = "dropThreshold";

    private final int copyThreshold;
    private final int deleteThreshold;

    public EpidemicGameOfLifeRouter(Settings s) {
        if (s.contains(COPY_THRESHOLD)) {
            this.copyThreshold = s.getInt(COPY_THRESHOLD);
        } else {
            this.copyThreshold = 3; //default threshold
        }
        if (s.contains(DELETE_THRESHOLD)) {
            this.deleteThreshold = s.getInt(DELETE_THRESHOLD);
        } else {
            this.deleteThreshold = -3; //default threshold
        }
    }

    public EpidemicGameOfLifeRouter(EpidemicGameOfLifeRouter prototype) {
        this.copyThreshold = prototype.copyThreshold;
        this.deleteThreshold = prototype.deleteThreshold;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        Vector<String> messagesToDelete = new Vector<>();

        /**
         * Checking for each message in thisHost collection if peer has the
         * same message with thisHost's message then the message counter decremented
         * else incremented
         */
        for (Message myMsg : thisHost.getMessageCollection()) {
            Integer msgCounter = (Integer) myMsg.getProperty(MSG_COUNTER_PROPERTY);
            if (peer.getRouter().hasMessage(myMsg.getId())) {
                msgCounter--;
            } else {
                msgCounter++;
            }
            myMsg.updateProperty(MSG_COUNTER_PROPERTY, msgCounter);
            Integer updatedMsgCounter = (Integer) myMsg.getProperty(MSG_COUNTER_PROPERTY);
            /** Message marked for being deleted */
            if (updatedMsgCounter == deleteThreshold) {
                messagesToDelete.add(myMsg.getId());
                 myMsg.updateProperty(MSG_COUNTER_PROPERTY, updatedMsgCounter);
            }
           
        }

        /** Message deleted */
        for (String msgId : messagesToDelete) {
            thisHost.deleteMessage(msgId, true);
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
        m.addProperty(MSG_COUNTER_PROPERTY, new Integer(0));
        return true;
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
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost){
        Integer msgCounter = (Integer) m.getProperty(MSG_COUNTER_PROPERTY);
        /** Message counter reseted to 0 when it's about to sent */
        if (msgCounter == copyThreshold) {
            m.updateProperty(MSG_COUNTER_PROPERTY, 0);
            return true;
        } 
        
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return true;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    @Override
    public RoutingDecisionEngineImproved replicate() {
        return new EpidemicGameOfLifeRouter(this);
    }

    @Override
    public void update(DTNHost host) {
    }

    @Override
    public void transferDone(Connection con) {
    }

}
