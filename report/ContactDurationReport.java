/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import core.Settings;
import core.SimScenario;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class ContactDurationReport extends Report {

    public static final String NODE_ID = "contactDurationToNodeID";
    private int nodeAddress;
    private Map<DTNHost, ArrayList<Double>> encounterData = new HashMap<>();
    private Map<DTNHost, ArrayList<Double>> intercontactData = new HashMap<>();

    public ContactDurationReport() {
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
            MessageRouter r = host.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            if (!(de instanceof RoutingDecisionEngine)) {
                continue;
            }
            DurationCalculable durationPropeties = (DurationCalculable) de;
            Map<DTNHost, ArrayList<Double>> ENT = durationPropeties.getContactDuration();

            if (host.getAddress() == nodeAddress) {
                encounterData.putAll(ENT);
            }

        }

        write("Encounter Time To " +nodeAddress);
        for (Map.Entry<DTNHost, ArrayList<Double>> entry : encounterData.entrySet()) {
            DTNHost key = entry.getKey();
            ArrayList<Double> value = entry.getValue();
            write(key+" "+ ' '+ value );
        }
        super.done();
    }
}
