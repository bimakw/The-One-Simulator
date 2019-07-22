package routing.community;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

import java.util.*;

public class SimBetFULLFAIR implements RoutingDecisionEngine, SimilarityDetectionEngine {

    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";
    public static final String SIMILARITY_SETTING = "similarityAlg";
    public static final String A_SETTING = "a";
    public static final String R_SIGMA = "shortTermR";
    public static final String R_LAMBDA = "longTermR";
    private static final Double R_LAMBDA_DEFAULT = 0.1;
    private static final Double R_SIGMA_DEFAULT = 0.2;


    protected Map<DTNHost, Set<DTNHost>> neighboursNode; // menyimpan daftar tetangga dari ego node
    protected Map<DTNHost, ArrayList<Double>> neighborsHistory;

    protected double[][] matrixEgoNetwork; // menyimpan nilai matrix ego network
    protected double[][] indirectNodeMatrix; //menyimpan matrix indirect node

    private double rSigma; //eksponential rate short term
    private double rLambda; //eksponential rate long term

    protected double betweennessCentrality;// menyimpan nilai betweenness centrality

    protected double a; //menyimpan konstanta untuk variabel similarity
    protected double b= 1-a; //menyimpan konstanta untuk variabel betweenness

    double util; //nilai util
    double s_ik, //interaction strength node i to k
            s_jk, //interaction strength node j to k
            lambda_ik = 0, //
            lambda_jk = 0,
            sigma_ik = 0,
            sigma_jk = 0;

    ArrayList<DTNHost> indirectNode, directNode; //menyimpan indirect node => m dan direct node+host => n

    protected SimilarityCounter similarity;
    protected CentralityDetection centrality;

    public SimBetFULLFAIR(Settings s) {

        if (s.contains(CENTRALITY_ALG_SETTING))
            this.centrality = (CentralityDetection) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        else
            this.centrality = new BetweennessCentrality(s);

        if (s.contains(SIMILARITY_SETTING))
            this.similarity = (SimilarityCounter) s.createIntializedObject(s.getSetting(SIMILARITY_SETTING));
        else
            this.similarity = new NeighbourhoodSimilarity(s);

        this.a = s.getDouble(A_SETTING);

        if (s.contains(R_SIGMA)) { //fair Route
            this.rSigma = s.getDouble(R_SIGMA);
        } else {
            this.rSigma = R_SIGMA_DEFAULT;
        }

        if (s.contains(R_LAMBDA)) {
            this.rLambda = s.getDouble(R_LAMBDA);
        } else {
            this.rLambda = R_LAMBDA_DEFAULT;
        }
    }


    public SimBetFULLFAIR(SimBetFULLFAIR proto) {

        neighboursNode = new HashMap<DTNHost, Set<DTNHost>>();
        indirectNode= new ArrayList<DTNHost>();
        directNode= new ArrayList<DTNHost>();
        this.a = proto.a;
        this.centrality = proto.centrality.replicate();
        this.similarity = proto.similarity.replicate();

        this.rLambda = proto.rLambda; //fair route
        this.rSigma = proto.rSigma;
        neighborsHistory = new HashMap<DTNHost, ArrayList<Double>>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {}

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {

        DTNHost myHost = con.getOtherNode(peer);
        SimBetFULLFAIR de = this.getOtherDecisionEngine(peer);
        double sigma = 0;
        double lambda = 0;
        double time = 0;

        if (this.neighboursNode.containsKey(peer)) { // jika node sudah pernah bertemu sebelumnya

            // daftar tetangga dari node peer akan diperbarui
            de.neighboursNode.replace(myHost, this.neighboursNode.keySet());
            this.neighboursNode.replace(peer, de.neighboursNode.keySet());
        }

        else { // jika node baru pertama kali ditemui

            // node baru akan ditambahkan ke dalam daftar tetangga
            // beserta tetangga yang sudah ditemui node peer
            de.neighboursNode.put(myHost, this.neighboursNode.keySet());
            this.neighboursNode.put(peer, de.neighboursNode.keySet());

            //Fair Route
            ArrayList<Double> nodeInformationList = new ArrayList<Double>();
            nodeInformationList.add(lambda);
            nodeInformationList.add(sigma);
            nodeInformationList.add(time);

            this.neighborsHistory.put(peer, nodeInformationList);
            de.neighborsHistory.put(myHost, nodeInformationList);
        }

        this.updateBetweenness(myHost); // mengupdate nilai betweenness
        this.updateSimilarity(myHost); //mengupdate indirect node
        this.updatePercieveInteractionStrength(peer);  //update lambda dan sigma ke semua node kontak


    }

    //update nilai lambda dan sigma
    protected void updatePercieveInteractionStrength(DTNHost peer) {
        double sigma;
        double lambda;
        double timeLastEncountered;
        double timeNew = SimClock.getTime();

        ArrayList<Double> nodeInformationList;

        for (Map.Entry<DTNHost, ArrayList<Double>> data : this.neighborsHistory.entrySet()) {

            if (data.getKey() == peer) {
                nodeInformationList = data.getValue();
                lambda = nodeInformationList.get(0);
                sigma = nodeInformationList.get(1);

                lambda++;
                sigma++;

                nodeInformationList.set(0, lambda);
                nodeInformationList.set(1, sigma);
                nodeInformationList.set(2, timeNew);

                this.neighborsHistory.replace(data.getKey(), nodeInformationList);
            } else {
                nodeInformationList = data.getValue();
                lambda = nodeInformationList.get(0);
                sigma = nodeInformationList.get(1);
                timeLastEncountered = nodeInformationList.get(2);

                lambda = lambda * (Math.pow(Math.E, (-(this.rLambda) * (timeNew - timeLastEncountered))));
                sigma = sigma * (Math.pow(Math.E, (-(this.rSigma) * (timeNew - timeLastEncountered))));

                nodeInformationList.set(0, lambda);
                nodeInformationList.set(1, sigma);

                this.neighborsHistory.replace(data.getKey(), nodeInformationList);

            }
        }
    }

    protected double AgrIntStrength(double lambda, double sigma) {
        return lambda * (lambda - sigma);
    }

    protected double countUtil(double s_jk, double s_ik) {
        return s_jk / (s_jk + s_ik);
    }

    protected double countAgrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) {
        double AggreIntStrength = 0;
        double lambda = 0, sigma = 0;
        for (Map.Entry<DTNHost, ArrayList<Double>> data : neighborsHist.entrySet()) {
            lambda = data.getValue().get(0);
            sigma = data.getValue().get(1);

            AggreIntStrength = this.AgrIntStrength(lambda, sigma);
        }

        return AggreIntStrength;

    }

    protected double countSumOfAgrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) {
        double sumOfIntStrength = 0;
        double lambda = 0, sigma = 0;
        for (Map.Entry<DTNHost, ArrayList<Double>> data : neighborsHist.entrySet()) {
            lambda = data.getValue().get(0);
            sigma = data.getValue().get(1);

            sumOfIntStrength = sumOfIntStrength + this.AgrIntStrength(lambda, sigma);
        }


        return sumOfIntStrength;

    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

        SimBetFULLFAIR de = getOtherDecisionEngine(otherHost);
        DTNHost dest = m.getTo();

        if (isFinalDest(m, otherHost)) {
            return true;
        }else{
            double  mySimbetUtil = this.countSimBetUtil(de.getSimilarity(dest),de.getBetweennessCentrality(),
                    this.getSimilarity(dest), this.getBetweennessCentrality());

            //hitung nilai simbet util teman saya
            double peerSimBetUtil = this.countSimBetUtil(this.getSimilarity(dest), this.getBetweennessCentrality(),
                    de.getSimilarity(dest), de.getBetweennessCentrality());
            System.out.println(peerSimBetUtil);
            /*routing dengan kombinasi similarity & betweenness*/
            if ( peerSimBetUtil > mySimbetUtil)
                return true;
            else
            //cek apakah this host pernah bertemu dest
            if (this.neighborsHistory.containsKey(dest)) {
                lambda_ik = this.neighborsHistory.get(dest).get(0);
                sigma_ik = this.neighborsHistory.get(dest).get(1);
            }

            //cek apakah peer pernah bertemu dest
            if (de.neighborsHistory.containsKey(dest)) {
                lambda_jk = de.neighborsHistory.get(dest).get(0);
                sigma_jk = de.neighborsHistory.get(dest).get(1);
            }

            if ((lambda_ik + lambda_jk) > 0) {
                s_jk = this.AgrIntStrength(lambda_jk, sigma_jk);
                s_ik = this.AgrIntStrength(lambda_ik, sigma_ik);

                //hitung u_ijk
                util = this.countUtil(s_jk, s_ik);

            } else {

                double sumOf_s_jk = this.countSumOfAgrIntStrength(de.neighborsHistory);
                double sumOf_s_ik = this.countSumOfAgrIntStrength(this.neighborsHistory);

                //hitung u_ij
                util = this.countUtil(sumOf_s_jk, sumOf_s_ik);
            }

            if (util > 0.5) {
                return true;
            }
        }
        //hitung nilai simbet util saya

            return false;
    }


    // ambil nilai similarity ke node dest

    @Override
    public double getSimilarity(DTNHost dest) {

        int index=0; //digunakan untuk membantu penghitungan index

        //cek apakah node dest merupakan direct node
        if (this.directNode.contains(dest)){
            for (DTNHost dtnHost : this.directNode) {

                if (dtnHost == dest) {
                    return this.similarity.countSimilarity(this.matrixEgoNetwork, null , index);
                }
                index++;
            }
        }

        //cek apakah node dest merupakan indirect node
        if(this.indirectNode.contains(dest)){

            //bangun matrix adjacency indirect node
            this.buildIndirectNodeMatrix(this.neighboursNode, dest);

            //hitung nilai similarity
            return this.similarity.countSimilarity(this.matrixEgoNetwork, this.indirectNodeMatrix , 0);

        }

        return 0;
    }

    // mengambil nilai betweenness yang sudah dihitung
    public double getBetweennessCentrality() {
        return this.betweennessCentrality;
    }

    // mengupdate nilai betweenness centrality
    protected void updateBetweenness(DTNHost myHost) {
        this.buildEgoNetwork(this.neighboursNode, myHost); // membangun ego network
        this.betweennessCentrality = this.centrality.getCentrality(this.matrixEgoNetwork); //menghitung nilai betweenness centrality
    }

    protected void updateSimilarity(DTNHost myHost) {

        //simpan data indirect node
        this.indirectNode.addAll(this.searchIndirectNeighbours(this.neighboursNode));

    }

    protected Set<DTNHost> searchIndirectNeighbours(Map<DTNHost, Set<DTNHost>> neighboursNode) {

        // mengambil daftar tetangga yang sudah ditemui secara langsung
        Set<DTNHost> directNeighbours = neighboursNode.keySet();

        // variabel untuk menyimpan daftar node yang belum pernah ditemui secara
        // langsung
        Set<DTNHost> setOfIndirectNeighbours = new HashSet<>();

        for (DTNHost dtnHost : directNeighbours) {

            // mengambil daftar tetangga dari peer yang sudah ditemui langsung
            Set<DTNHost> neighboursOfpeer = neighboursNode.get(dtnHost);

            for (DTNHost dtnHost1 : neighboursOfpeer) {

                // jika dtnHost1 belum pernah ditemui secara langsung
                if (!directNeighbours.contains(dtnHost1)) {

                    // cek apakah listOfUndirectNeighbours masih kosong
                    if (setOfIndirectNeighbours.isEmpty()) {

                        // jika masih kosong masukkan langsung dtnHost1 ke dalam
                        // listOfIndirectNeighbours
                        setOfIndirectNeighbours.add(dtnHost1);

                    } else {// jika listOfUndirectNeighbours tidak kosong

                        // cek apakah dtnHost1 sudah pernah dicatat ke dalam
                        // listOfindirectNeighbours
                        if (!setOfIndirectNeighbours.contains(dtnHost1)) {
                            setOfIndirectNeighbours.add(dtnHost1);
                        }
                    }
                }
            }
        }

        return setOfIndirectNeighbours;
    }

    // method yang digunakan untuk membangun matriks ego network
    public void buildIndirectNodeMatrix(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost dest) {
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


    // method yang digunakan untuk membangun matriks ego network
    protected void buildEgoNetwork(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost host) {
        ArrayList<DTNHost> dummyArray = buildDummyArray(neighboursNode, host);
        SimBetFULLFAIR hostDE = getOtherDecisionEngine(host);
        double[][] neighboursAdj = new double[dummyArray.size()][dummyArray.size()];

        for (int i = 0; i < dummyArray.size(); i++) {
            for (int j = i; j < dummyArray.size(); j++) {
                if (i == j) {
                    neighboursAdj[i][j] = 0;
                } else if (neighboursNode.get(dummyArray.get(j)).contains(dummyArray.get(i))) {
                    neighboursAdj[i][j] = this.countAgrIntStrength(this.neighborsHistory);
                   neighboursAdj[j][i] = neighboursAdj[i][j];
//                   System.out.println(value);
                } else {
                    neighboursAdj[i][j] = 0;
                    neighboursAdj[j][i] = neighboursAdj[i][j];
                }
            }
        }

        this.matrixEgoNetwork = neighboursAdj;

    }


    protected ArrayList<DTNHost> buildDummyArray(Map<DTNHost, Set<DTNHost>> neighbours, DTNHost myHost) {
        ArrayList<DTNHost> dummyArray = new ArrayList<>();
        dummyArray.add(myHost);
        dummyArray.addAll(neighbours.keySet());
        this.directNode = dummyArray; //mengisi himpunan n pada matrix
        return dummyArray;
    }

    protected double countSimBetUtil(double simPeerForDest, double betweennessPeer, double mySimForDest, double myBetweenness ){
        double simBetUtil, simUtilForDest, betUtil;

        simUtilForDest=mySimForDest/(mySimForDest+simPeerForDest);

        betUtil= myBetweenness/(myBetweenness+betweennessPeer);

        simBetUtil = (this.a*simUtilForDest) + ((1-this.a)*betUtil);

        return simBetUtil;
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
        // TODO Auto-generated method stub
        return m.getTo() != thisHost;
    }

    private SimBetFULLFAIR getOtherDecisionEngine(DTNHost otherHost) {
        MessageRouter otherRouter = otherHost.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (SimBetFULLFAIR) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
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
        return new SimBetFULLFAIR(this);
    }

}
