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
    private Map<DTNHost, ArrayList<Double>> intercontactData = new HashMap<>();

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
            DurationCalculable durationPropeties = (DurationCalculable) de;
            Map<DTNHost, ArrayList<Double>> NENT = durationPropeties.getIntercontactDuration();

            if (host.getAddress() == nodeAddress) {
                intercontactData.putAll(NENT);
            }

        }

        write("Intercontact Time To " +nodeAddress);
        for (Map.Entry<DTNHost, ArrayList<Double>> entry : intercontactData.entrySet()) {
            DTNHost key = entry.getKey();
            ArrayList<Double> value = entry.getValue();
            write(key+" "+ ' '+ value );
        }
        super.done();
    }
}
