/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.Duration;
import core.DTNHost;
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
 * Provides the contact duration data 
 * for making probability density function
 * @author Gregorius Bima, Sanata Dharma University
 */
public class ContactDurationReport extends Report {

    public static final String NODE_ID = "contactDurationToNodeID";
    private int nodeAddress;
    private Map<DTNHost, List<Duration>> encounterData ;
    private Map<DTNHost, Double> avgEncounterData ;

    public ContactDurationReport() {
        super();
        Settings s = getSettings();
        if (s.contains(NODE_ID)) {
            nodeAddress = s.getInt(NODE_ID);
        } else {
            nodeAddress = 0;
        }
        encounterData= new HashMap<>();
        avgEncounterData = new HashMap<>();
    }

    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();
        for (DTNHost host : nodes) {
            MessageRouter r = host.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            if (!(de instanceof RoutingDecisionEngine)) {
                continue;
            }
            DurationCalculable durationPropeties = (DurationCalculable) de;
            Map<DTNHost, List<Duration>> ENT = durationPropeties.getEncounterHistory();

            if (host.getAddress() == nodeAddress) {
                encounterData.putAll(ENT);
            }

        }
        
        
        for (DTNHost node : nodes) {
            if (encounterData.containsKey(node)) {
                double avgContactDuration = calculateAvgDuration(encounterData.get(node));
                avgEncounterData.put(node, avgContactDuration);
            }
        }
        double values = 0;
        for (Double avgEncounter : avgEncounterData.values()) {
            values += avgEncounter;
        }
        
        double avgValues = values/avgEncounterData.size();
        
        

        write("Encounter Time To " +nodeAddress);
        for (Map.Entry<DTNHost, Double> entry : avgEncounterData.entrySet()) {
            DTNHost key = entry.getKey();
            Double value = entry.getValue();
            write(key+" "+ ' '+ value );
        }
        write("Average Encounter Duration = "+avgValues);
        super.done();
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
}
