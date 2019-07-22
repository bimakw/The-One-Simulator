/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import java.util.*;
import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class SimBetRouter implements RoutingDecisionEngine, SimilarityDetectionEngine{

    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";
    public static final String SIMILARITY_SETTING = "similarityAlg";
    public static final String A_SETTING = "a";

    private Map<DTNHost, Set<DTNHost>> neighboursNode; // menyimpan daftar tetangga dari ego node

    private double[][] matrixEgoNetwork; // menyimpan nilai matrix ego network
    private double[][] indirectNodeMatrix; //menyimpan matrix indirect node

    private double betweennessCentrality;// menyimpan nilai betweenness centrality

    private double a; //menyimpan konstanta untuk variabel similarity
    private double b= 1-a; //menyimpan konstanta untuk variabel betweenness

    private ArrayList<DTNHost> indirectNode, directNode; //menyimpan indirect node => m dan direct node+host => n

    private SimilarityCounter similarity;
    private CentralityDetection centrality;

    public SimBetRouter(Settings s) {

        if (s.contains(CENTRALITY_ALG_SETTING))
            this.centrality = (CentralityDetection) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        else
            this.centrality = new BetweennessCentrality(s);

        if (s.contains(SIMILARITY_SETTING))
            this.similarity = (SimilarityCounter) s.createIntializedObject(s.getSetting(SIMILARITY_SETTING));
        else
            this.similarity = new NeighbourhoodSimilarity(s);

        this.a = s.getDouble(A_SETTING);
    }

    public SimBetRouter(SimBetRouter proto) {

        neighboursNode = new HashMap<DTNHost, Set<DTNHost>>();
        indirectNode= new ArrayList<DTNHost>();
        directNode= new ArrayList<DTNHost>();
        this.a = proto.a;
        this.centrality = proto.centrality.replicate();
        this.similarity = proto.similarity.replicate();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {

        DTNHost myHost = con.getOtherNode(peer);
        SimBetRouter de = this.getOtherSimBetRouter(peer);

        if (this.neighboursNode.containsKey(peer)) { 

            de.neighboursNode.replace(myHost, this.neighboursNode.keySet());
            this.neighboursNode.replace(peer, de.neighboursNode.keySet());
        }

        else {

            de.neighboursNode.put(myHost, this.neighboursNode.keySet());
            this.neighboursNode.put(peer, de.neighboursNode.keySet());
        }

        this.updateBetweenness(myHost); 
        this.updateSimilarity(myHost); 
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
        return m.getTo() != thisHost;
    }
    
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

        SimBetRouter de = getOtherSimBetRouter(otherHost);
        DTNHost dest = m.getTo();

        if (isFinalDest(m, otherHost))
            return true;

        double  mySimbetUtil = this.countSimBetRouterUtil(de.getSimilarity(dest),de.getBetweennessCentrality(),
                this.getSimilarity(dest), this.getBetweennessCentrality());

        double peerSimBetRouterUtil = this.countSimBetRouterUtil(this.getSimilarity(dest), this.getBetweennessCentrality(),
                de.getSimilarity(dest), de.getBetweennessCentrality());

        if ( peerSimBetRouterUtil > mySimbetUtil)
            return true;
        else
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
    public double getSimilarity(DTNHost dest) {

        int index=0; 

        if (this.directNode.contains(dest)){
            for (DTNHost dtnHost : this.directNode) {

                if (dtnHost == dest) {
                    return this.similarity.countSimilarity(this.matrixEgoNetwork, null , index);
                }
                index++;
            }
        }

        if(this.indirectNode.contains(dest)){

            this.buildIndirectNodeMatrix(this.neighboursNode, dest);

            return this.similarity.countSimilarity(this.matrixEgoNetwork, this.indirectNodeMatrix , 0);

        }

        return 0;
    }

    private double getBetweennessCentrality() {
        return this.betweennessCentrality;
    }

    private void updateBetweenness(DTNHost myHost) {
        this.buildEgoNetwork(this.neighboursNode, myHost); // membangun ego network
        this.betweennessCentrality = this.centrality.getCentrality(this.matrixEgoNetwork); //menghitung nilai betweenness centrality
    }

    private void updateSimilarity(DTNHost myHost) {
        this.indirectNode.addAll(this.searchIndirectNeighbours(this.neighboursNode));
    }

    private Set<DTNHost> searchIndirectNeighbours(Map<DTNHost, Set<DTNHost>> neighboursNode) {

        Set<DTNHost> directNeighbours = neighboursNode.keySet();

        // variabel untuk menyimpan daftar node yang belum pernah ditemui secara
        // langsung
        Set<DTNHost> setOfIndirectNeighbours = new HashSet<>();

        for (DTNHost dtnHost : directNeighbours) {

            Set<DTNHost> neighboursOfpeer = neighboursNode.get(dtnHost);

            for (DTNHost dtnHost1 : neighboursOfpeer) {
                if (!directNeighbours.contains(dtnHost1)) {
                    if (setOfIndirectNeighbours.isEmpty()) {
                        setOfIndirectNeighbours.add(dtnHost1);
                    } else {
                        if (!setOfIndirectNeighbours.contains(dtnHost1)) {
                            setOfIndirectNeighbours.add(dtnHost1);
                        }
                    }
                }
            }
        }

        return setOfIndirectNeighbours;
    }

    private void buildIndirectNodeMatrix(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost dest) {
        ArrayList<DTNHost> dummyArrayN = this.directNode;


        double[][] neighboursAdj = new double[dummyArrayN.size()][1];

        for (int i = 0; i < dummyArrayN.size(); i++) {
            for (int j = 0; j < 1; j++) {
                if (i==0) {
                    neighboursAdj[i][j]=0;
                }
                else if (neighboursNode.get(dummyArrayN.get(i)).contains(dest)) {
                    neighboursAdj[i][j] = 1;

                } else {
                    neighboursAdj[i][j] = 0;

                }
            }
        }
        this.indirectNodeMatrix = neighboursAdj;
    }

    private void buildEgoNetwork(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost host) {
        ArrayList<DTNHost> dummyArray = buildDummyArray(neighboursNode, host);

        double[][] neighboursAdj = new double[dummyArray.size()][dummyArray.size()];

        for (int i = 0; i < dummyArray.size(); i++) {
            for (int j = i; j < dummyArray.size(); j++) {
                if (i == j) {
                    neighboursAdj[i][j] = 0;
                } else if (neighboursNode.get(dummyArray.get(j)).contains(dummyArray.get(i))) {
                    neighboursAdj[i][j] = 1;
                    neighboursAdj[j][i] = neighboursAdj[i][j];
                } else {
                    neighboursAdj[i][j] = 0;
                    neighboursAdj[j][i] = neighboursAdj[i][j];
                }
            }
        }

        this.matrixEgoNetwork = neighboursAdj;
    }

    private ArrayList<DTNHost> buildDummyArray(Map<DTNHost, Set<DTNHost>> neighbours, DTNHost myHost) {
        ArrayList<DTNHost> dummyArray = new ArrayList<>();
        dummyArray.add(myHost);
        dummyArray.addAll(neighbours.keySet());
        this.directNode = dummyArray; 
        return dummyArray;
    }

    private double countSimBetRouterUtil(double simPeerForDest, double betweennessPeer, double mySimForDest, double myBetweenness ){
        double simBetUtil, simUtilForDest, betUtil;

        simUtilForDest=mySimForDest/(mySimForDest+simPeerForDest);

        betUtil= myBetweenness/(myBetweenness+betweennessPeer);

        simBetUtil = (this.a*simUtilForDest) + ((1-this.a)*betUtil);

        return simBetUtil;
    }

    private SimBetRouter getOtherSimBetRouter(DTNHost otherHost) {
        MessageRouter otherRouter = otherHost.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (SimBetRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SimBetRouter(this);
    }
    
}
