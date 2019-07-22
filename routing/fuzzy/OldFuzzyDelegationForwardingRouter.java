/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.fuzzy;

import core.*;
import java.util.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class OldFuzzyDelegationForwardingRouter implements RoutingDecisionEngine {

    public static final String CONTACT_DURATION_INTERSECTION = "contactDuration";
    public static final String INTERCONTACT_DURATION_INTERSECTION = "intercontactDuration";
    public static final String MESSAGE_UTILITY = "value";
    /**
     * The output value of the fuzzy set
     */
    public static final double TOU_VALUE_WORST = 1, TOU_VALUE_WORSE = 2,
            TOU_VALUE_FAIR = 3, TOU_VALUE_GOOD = 4, TOU_VALUE_GRAND = 5;
    protected double[] contactDurationPoint;
    protected double[] intercontactDurationPoint;
    protected Map<DTNHost, Double> meetings;
    protected Map<DTNHost, Double> disconnects;
    /**
     * Encounter Time
     */
    protected Map<DTNHost, Double> contactDuration;
    /**
     * Non-Encounter Time
     */
    protected Map<DTNHost, Double> intercontactDuration;
    /**
     * The fuzzy value from defuzzification for this scheme called Transfer of
     * Utility (TOU)
     */
    protected Map<DTNHost, Double> TOU;

    public OldFuzzyDelegationForwardingRouter(Settings s) {
        this.contactDurationPoint = new double[8];
        this.intercontactDurationPoint = new double[8];
        double[] intercontactDataSet = s.getCsvDoubles(INTERCONTACT_DURATION_INTERSECTION, 8);
        int[] contactDurationDataSet = s.getCsvInts(CONTACT_DURATION_INTERSECTION, 8);
        for (int i = 0; i <= 7; i++) {
            this.contactDurationPoint[i] = contactDurationDataSet[i];
            this.intercontactDurationPoint[i] = intercontactDataSet[i];
        }

        meetings = new HashMap<>();
        intercontactDuration = new HashMap<>();
        disconnects = new HashMap<>();
        contactDuration = new HashMap<>();
        TOU = new HashMap<>();
    }

    public OldFuzzyDelegationForwardingRouter(OldFuzzyDelegationForwardingRouter prototype) {
        this.contactDurationPoint = prototype.contactDurationPoint;
        this.intercontactDurationPoint = prototype.intercontactDurationPoint;
        meetings = new HashMap<>();
        intercontactDuration = new HashMap<>();
        disconnects = new HashMap<>();
        contactDuration = new HashMap<>();
        TOU = new HashMap<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        double currentTime = SimClock.getTime();
        /**
         * Put the non encounter time with peer
         */
        double timeDiff = currentTime - getDisconnectFor(peer);
        this.intercontactDuration.put(peer, timeDiff);
        this.meetings.put(peer, currentTime);

        /**
         * Compute the defuzzification
         */
        this.computeDefuzzificationFor(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double currentTime = SimClock.getTime();

        double timeDifferent = currentTime - getMeetingFor(peer);
        this.contactDuration.put(peer, timeDifferent);
        this.disconnects.put(peer, currentTime);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MESSAGE_UTILITY, this.getTOUValueFor(m.getTo()));
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
        OldFuzzyDelegationForwardingRouter myPartner = getOtherFuzzyRouter(otherHost);
        if (m.getTo() == otherHost) {
            return true;
        }
        
        //Delegation Forwarding
        Double utilityValue = (Double) m.getProperty(MESSAGE_UTILITY);
        if (this.getTOUValueFor(m.getTo()) < myPartner.getTOUValueFor(m.getTo())) {
            if (utilityValue < myPartner.getTOUValueFor(m.getTo())) {
                utilityValue = myPartner.getTOUValueFor(m.getTo());
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
        return new OldFuzzyDelegationForwardingRouter(this);
    }

    private OldFuzzyDelegationForwardingRouter getOtherFuzzyRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (OldFuzzyDelegationForwardingRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    /**
     * Change from fuzzy value to crisp and compute utility value for host
     *
     * @param host
     */
    private void computeDefuzzificationFor(DTNHost host) {
        double defuzzification = (((degreeOfMembershipGrand1Function(host) * TOU_VALUE_GRAND) + (degreeOfMembershipGrand2Function(host) * TOU_VALUE_GRAND)
                + (degreeOfMembershipGrand3Function(host) * TOU_VALUE_GRAND) + (degreeOfMembershipGrand4Funciton(host) * TOU_VALUE_GRAND)
                + (degreeOfMembershipGrand5Funciton(host) * TOU_VALUE_GRAND) + (degreeOfMembershipGood1Funciton(host) * TOU_VALUE_GOOD)
                + (degreeOfMembershipGood2Funciton(host) * TOU_VALUE_GOOD) + (degreeOfMembershipGood3Funciton(host) * TOU_VALUE_GOOD)
                + (degreeOfMembershipGood4Funciton(host) * TOU_VALUE_GOOD) + (degreeOfMembershipGood5Function(host) * TOU_VALUE_GOOD)
                + (degreeOfMembershipFair1Function(host) * TOU_VALUE_FAIR) + (degreeOfMembershipFair2Function(host) * TOU_VALUE_FAIR)
                + (degreeOfMembershipFair3Funciton(host) * TOU_VALUE_FAIR) + (degreeOfMembershipFair4Funciton(host) * TOU_VALUE_FAIR)
                + (degreeOfMembershipFair5Funciton(host) * TOU_VALUE_FAIR) + (degreeOfMembershipWorse1Funciton(host) * TOU_VALUE_WORSE)
                + (degreeOfMembershipWorse2Funciton(host) * TOU_VALUE_WORSE) + (degreeOfMembershipWorse3Funciton(host) * TOU_VALUE_WORSE)
                + (degreeOfMembershipWorse4Function(host) * TOU_VALUE_WORSE) + (degreeOfMembershipWorse5Function(host) * TOU_VALUE_WORSE)
                + (degreeOfMembershipWorst1Function(host) * TOU_VALUE_WORST) + (degreeOfMembershipWorst2Funciton(host) * TOU_VALUE_WORST)
                + (degreeOfMembershipWorst3Funciton(host) * TOU_VALUE_WORST) + (degreeOfMembershipWorst4Funciton(host) * TOU_VALUE_WORST)
                + (degreeOfMembershipWorst5Funciton(host) * TOU_VALUE_WORST))
                / sumOfTheDegreeOfMembershipFor(host));
        TOU.put(host, defuzzification);
    }

    /**
     * Sum every value of each moment rules
     *
     * @param host
     * @return
     */
    private double sumOfTheDegreeOfMembershipFor(DTNHost host) {
        return degreeOfMembershipGrand1Function(host) + degreeOfMembershipGrand2Function(host)
                + degreeOfMembershipGrand3Function(host) + degreeOfMembershipGrand4Funciton(host)
                + degreeOfMembershipGrand5Funciton(host) + degreeOfMembershipGood1Funciton(host)
                + degreeOfMembershipGood2Funciton(host) + degreeOfMembershipGood3Funciton(host)
                + degreeOfMembershipGood4Funciton(host) + degreeOfMembershipGood5Function(host)
                + degreeOfMembershipFair1Function(host) + degreeOfMembershipFair2Function(host)
                + degreeOfMembershipFair3Funciton(host) + degreeOfMembershipFair4Funciton(host)
                + degreeOfMembershipFair5Funciton(host) + degreeOfMembershipWorse1Funciton(host)
                + degreeOfMembershipWorse2Funciton(host) + degreeOfMembershipWorse3Funciton(host)
                + degreeOfMembershipWorse4Function(host) + degreeOfMembershipWorse5Function(host)
                + degreeOfMembershipWorst1Function(host) + degreeOfMembershipWorst2Funciton(host)
                + degreeOfMembershipWorst3Funciton(host) + degreeOfMembershipWorst4Funciton(host)
                + degreeOfMembershipWorst5Funciton(host);
    }

    /**
     * Get utility value for host
     *
     * @param host
     * @return The utility value for host
     */
    private double getTOUValueFor(DTNHost host) {
        if (TOU.containsKey(host)) {
            return TOU.get(host);
        } else {
            return 0;
        }
    }

    /**
     * Get last record contact duration for host
     *
     * @param host
     * @return The last record of contact duration
     */
    private double getMeetingFor(DTNHost host) {
        if (meetings.containsKey(host)) {
            return meetings.get(host);
        } else {
            return 0;
        }
    }

    /**
     * Get input variable contact duration for host
     *
     * @param host
     * @return Input variable contact duration
     */
    private double getEncounterFor(DTNHost host) {
        if (contactDuration.containsKey(host)) {
            return contactDuration.get(host);
        } else {
            return 0;
        }
    }

    /**
     * Get input variable inter-contact duration for host
     *
     * @param host
     * @return Input variable inter-contact duration
     */
    private double getIntercontactFor(DTNHost host) {
        if (intercontactDuration.containsKey(host)) {
            return intercontactDuration.get(host);
        } else {
            return 0;
        }
    }

    /**
     * Get last record inter-contact duration for host
     *
     * @param host
     * @return the last record of inter-contact duration
     */
    private double getDisconnectFor(DTNHost host) {
        if (disconnects.containsKey(host)) {
            return disconnects.get(host);
        } else {
            return 0;
        }
    }

    // This part below for membership function 
    // for each fuzzy sets that being used in this routing algorithm
    private double encounterVeryLowFunciton(DTNHost host) {
        if (getEncounterFor(host) <= contactDurationPoint[0]) {
            return 1.0;
        } else if (contactDurationPoint[0] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[1]) {
            return (contactDurationPoint[1] - getEncounterFor(host)) / (contactDurationPoint[1] - contactDurationPoint[0]);
        } else {
            return 0;
        }
    }

    private double encounterLowFunciton(DTNHost host) {
        if (contactDurationPoint[0] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[1]) {
            return (getEncounterFor(host) - contactDurationPoint[0]) / (contactDurationPoint[1] - contactDurationPoint[0]);
        } else if (contactDurationPoint[2] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[3]) {
            return (contactDurationPoint[3] - getEncounterFor(host)) / (contactDurationPoint[3] - contactDurationPoint[2]);
        } else if (contactDurationPoint[1] <= getEncounterFor(host) && getEncounterFor(host) <= contactDurationPoint[2]) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private double encounterMediumFunciton(DTNHost host) {
        if (contactDurationPoint[2] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[3]) {
            return (getEncounterFor(host) - contactDurationPoint[2]) / (contactDurationPoint[3] - contactDurationPoint[2]);
        } else if (contactDurationPoint[4] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[5]) {
            return (contactDurationPoint[5] - getEncounterFor(host)) / (contactDurationPoint[5] - contactDurationPoint[4]);
        } else if (contactDurationPoint[3] <= getEncounterFor(host) && getEncounterFor(host) <= contactDurationPoint[4]) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private double encounterHighFunciton(DTNHost host) {
        if (contactDurationPoint[4] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[5]) {
            return (getEncounterFor(host) - contactDurationPoint[4]) / (contactDurationPoint[5] - contactDurationPoint[4]);
        } else if (contactDurationPoint[6] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[7]) {
            return (contactDurationPoint[7] - getEncounterFor(host)) / (contactDurationPoint[7] - contactDurationPoint[6]);
        } else if (contactDurationPoint[5] <= getEncounterFor(host) && getEncounterFor(host) <= contactDurationPoint[6]) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private double encounterVeryHighFunciton(DTNHost host) {
        if (contactDurationPoint[6] < getEncounterFor(host) && getEncounterFor(host) < contactDurationPoint[7]) {
            return (getEncounterFor(host) - contactDurationPoint[6]) / (contactDurationPoint[7] - contactDurationPoint[6]);
        } else if (getEncounterFor(host) >= contactDurationPoint[7]) {
            return 1;
        } else {
            return 0;
        }
    }

    private double intercontactVeryLowFunciton(DTNHost host) {
        if (getIntercontactFor(host) <= intercontactDurationPoint[0]) {
            return 1.0;
        } else if (intercontactDurationPoint[0] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[1]) {
            return (intercontactDurationPoint[1] - getIntercontactFor(host)) / (intercontactDurationPoint[1] - intercontactDurationPoint[0]);
        } else {
            return 0;
        }
    }

    private double intercontactLowFunciton(DTNHost host) {
        if (intercontactDurationPoint[0] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[1]) {
            return (getIntercontactFor(host) - intercontactDurationPoint[0]) / (intercontactDurationPoint[1] - intercontactDurationPoint[0]);
        } else if (intercontactDurationPoint[2] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[3]) {
            return (intercontactDurationPoint[3] - getIntercontactFor(host)) / (intercontactDurationPoint[3] - intercontactDurationPoint[2]);
        } else if (intercontactDurationPoint[1] <= getIntercontactFor(host) && getIntercontactFor(host) <= intercontactDurationPoint[2]) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private double intercontactMediumFunciton(DTNHost host) {
        if (intercontactDurationPoint[2] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[3]) {
            return (getIntercontactFor(host) - intercontactDurationPoint[2]) / (intercontactDurationPoint[3] - intercontactDurationPoint[2]);
        } else if (intercontactDurationPoint[4] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[5]) {
            return (intercontactDurationPoint[5] - getIntercontactFor(host)) / (intercontactDurationPoint[5] - intercontactDurationPoint[4]);
        } else if (intercontactDurationPoint[3] <= getIntercontactFor(host) && getIntercontactFor(host) <= intercontactDurationPoint[4]) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private double intercontactHighFunciton(DTNHost host) {
        if (intercontactDurationPoint[4] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[5]) {
            return (getIntercontactFor(host) - intercontactDurationPoint[4]) / (intercontactDurationPoint[5] - intercontactDurationPoint[4]);
        } else if (intercontactDurationPoint[6] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[7]) {
            return (intercontactDurationPoint[7] - getIntercontactFor(host)) / (intercontactDurationPoint[7] - intercontactDurationPoint[6]);
        } else if (intercontactDurationPoint[5] <= getIntercontactFor(host) && getIntercontactFor(host) <= intercontactDurationPoint[6]) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private double intercontactVeryHighFunciton(DTNHost host) {
        if (intercontactDurationPoint[6] < getIntercontactFor(host) && getIntercontactFor(host) < intercontactDurationPoint[7]) {
            return (getIntercontactFor(host) - intercontactDurationPoint[6]) / (intercontactDurationPoint[7] - intercontactDurationPoint[6]);
        } else if (getIntercontactFor(host) >= intercontactDurationPoint[7]) {
            return 1;
        } else {
            return 0;
        }
    }

    private double degreeOfMembershipGrand1Function(DTNHost host) {
        return Math.min(encounterVeryHighFunciton(host), intercontactVeryLowFunciton(host));//25
    }

    private double degreeOfMembershipGrand2Function(DTNHost host) {
        return Math.min(encounterVeryHighFunciton(host), intercontactLowFunciton(host));//24
    }

    private double degreeOfMembershipGrand3Function(DTNHost host) {
        return Math.min(encounterVeryHighFunciton(host), intercontactMediumFunciton(host));//23
    }

    private double degreeOfMembershipGrand4Funciton(DTNHost host) {
        return Math.min(encounterVeryHighFunciton(host), intercontactHighFunciton(host));//22
    }

    private double degreeOfMembershipGrand5Funciton(DTNHost host) {
        return Math.min(encounterVeryHighFunciton(host), intercontactVeryHighFunciton(host));//21
    }

    private double degreeOfMembershipGood1Funciton(DTNHost host) {
        return Math.min(encounterHighFunciton(host), intercontactVeryLowFunciton(host));//20
    }

    private double degreeOfMembershipGood2Funciton(DTNHost host) {
        return Math.min(encounterHighFunciton(host), intercontactLowFunciton(host));//19
    }

    private double degreeOfMembershipGood3Funciton(DTNHost host) {
        return Math.min(encounterHighFunciton(host), intercontactMediumFunciton(host));//18
    }

    private double degreeOfMembershipGood4Funciton(DTNHost host) {
        return Math.min(encounterHighFunciton(host), intercontactHighFunciton(host));//17
    }

    private double degreeOfMembershipGood5Function(DTNHost host) {
        return Math.min(encounterHighFunciton(host), intercontactVeryHighFunciton(host));//16
    }

    private double degreeOfMembershipFair1Function(DTNHost host) {
        return Math.min(encounterMediumFunciton(host), intercontactVeryLowFunciton(host));//15
    }

    private double degreeOfMembershipFair2Function(DTNHost host) {
        return Math.min(encounterMediumFunciton(host), intercontactLowFunciton(host));//14
    }

    private double degreeOfMembershipFair3Funciton(DTNHost host) {
        return Math.min(encounterMediumFunciton(host), intercontactMediumFunciton(host));//13
    }

    private double degreeOfMembershipFair4Funciton(DTNHost host) {
        return Math.min(encounterMediumFunciton(host), intercontactHighFunciton(host));//12
    }

    private double degreeOfMembershipFair5Funciton(DTNHost host) {
        return Math.min(encounterMediumFunciton(host), intercontactVeryHighFunciton(host));//11
    }

    private double degreeOfMembershipWorse1Funciton(DTNHost host) {
        return Math.min(encounterLowFunciton(host), intercontactVeryLowFunciton(host));//10
    }

    private double degreeOfMembershipWorse2Funciton(DTNHost host) {
        return Math.min(encounterLowFunciton(host), intercontactLowFunciton(host));//9
    }

    private double degreeOfMembershipWorse3Funciton(DTNHost host) {
        return Math.min(encounterLowFunciton(host), intercontactMediumFunciton(host));//8
    }

    private double degreeOfMembershipWorse4Function(DTNHost host) {
        return Math.min(encounterLowFunciton(host), intercontactHighFunciton(host));//7
    }

    private double degreeOfMembershipWorse5Function(DTNHost host) {
        return Math.min(encounterLowFunciton(host), intercontactVeryHighFunciton(host));//6
    }

    private double degreeOfMembershipWorst1Function(DTNHost host) {
        return Math.min(encounterVeryLowFunciton(host), intercontactVeryLowFunciton(host));//5
    }

    private double degreeOfMembershipWorst2Funciton(DTNHost host) {
        return Math.min(encounterVeryLowFunciton(host), intercontactLowFunciton(host));//4
    }

    private double degreeOfMembershipWorst3Funciton(DTNHost host) {
        return Math.min(encounterVeryLowFunciton(host), intercontactMediumFunciton(host));//3
    }

    private double degreeOfMembershipWorst4Funciton(DTNHost host) {
        return Math.min(encounterVeryLowFunciton(host), intercontactHighFunciton(host));//2
    }

    private double degreeOfMembershipWorst5Funciton(DTNHost host) {
        return Math.min(encounterVeryLowFunciton(host), intercontactVeryHighFunciton(host));//1
    }

}
