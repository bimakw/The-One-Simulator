/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import core.Duration;
import core.Settings;
import core.SimScenario;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class IntercontactDurationReport extends Report{
    public static final String NODE_ID = "intercontactToNodeID";
    private int nodeAddress;
    private Map<DTNHost, List<Duration>> intercontactData = new HashMap<>();
    private Map<DTNHost, Double> avgIntercontactData = new HashMap<>();

    public IntercontactDurationReport() {
        super();
        Settings s = getSettings();
        if (s.contains(NODE_ID)) {
            nodeAddress = s.getInt(NODE_ID);
        } else {
            nodeAddress = 0;
        }
    }

    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();
        for (DTNHost host : nodes) {
            MessageRouter router = host.getRouter();
            if (!(router instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) router).getDecisionEngine();
            if (!(de instanceof RoutingDecisionEngine)) {
                continue;
            }
            DurationStampable durationPropeties = (DurationStampable) de;
            Map<DTNHost, List<Duration>> NENT = durationPropeties.getIntercontactHistory();

            if (host.getAddress() == nodeAddress) {
                intercontactData = NENT;
            }

        }
        
        for (DTNHost node : nodes) {
            if (intercontactData.containsKey(node)) {
                double avgIntercontactDuration = calculateAvgIntercontact(intercontactData.get(node));
                avgIntercontactData.put(node, avgIntercontactDuration);
            }
        }
        double values = 0;
        for (Double avgEncounter : avgIntercontactData.values()) {
            values += avgEncounter;
        }
        
        double avgValues = values/avgIntercontactData.size();

        write("Inter-contact Time To " +nodeAddress);
        for (Map.Entry<DTNHost, Double> entry : avgIntercontactData.entrySet()) {
            DTNHost key = entry.getKey();
            Double value = entry.getValue();
            write(key+" "+ ' '+ value );
        }
        write("Average Intercontact Duration = "+avgValues);
        super.done();
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
}
