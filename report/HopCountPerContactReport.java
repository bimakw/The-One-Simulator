/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author by Gregorius Bima, Sanata Dharma University
 */
public class HopCountPerContactReport extends Report implements MessageListener, ConnectionListener {

    private List<Integer> hopCounts;
    private int lastRecord = 0;
    public static final int DEFAULT_CONTACT_COUNT = 500;
    private int interval;

    private Map<Integer, Integer> hopCount;
    private int TOTAL_CONTACT = 0;

    /**
     * Constructor.
     */
    public HopCountPerContactReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.hopCounts = new ArrayList<Integer>();
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
            boolean finalTarget) {
        if (finalTarget) {
            this.hopCounts.add(m.getHops().size() - 1);
        }
    }

    public void newMessage(Message m) {
    }

    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void done() {
        String statsText = "Contact\tHop Count\n";
        for (Map.Entry<Integer, Integer> entry : hopCount.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            statsText += key + "\t" + value + "\n";
        }
        write(statsText);
        super.done();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        TOTAL_CONTACT++;
        int totalHopCount = 0;
        if (TOTAL_CONTACT - lastRecord >= interval) {
            lastRecord = TOTAL_CONTACT;
            for (Iterator<Integer> iterator = hopCounts.iterator(); iterator.hasNext();) {
                Integer next = iterator.next();
                totalHopCount = totalHopCount + next;
            }
            hopCount.put(lastRecord, totalHopCount);
        }
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
    }

}
