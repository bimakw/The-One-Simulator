package routing.community;

import java.util.*;
import core.*;
import routing.*;

public class SimBetWProphet implements RoutingDecisionEngine, SimilarityDetectionEngine {

	public static final String CENTRALITY_ALG_SETTING = "centralityAlg";
	public static final String SIMILARITY_SETTING = "similarityAlg";
	public static final String A_SETTING = "a";
	protected final static String BETA_SETTING = "beta";
	protected final static String P_INIT_SETTING = "initial_p";
	protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

	public static double alpha;
	protected static final double GAMMA = 0.92;
	protected static final double DEFAULT_P_INIT = 0.45;
	protected static final double DEFAULT_BETA = 0.45;
	protected static final int    DEFAULT_UNIT = 30;

	protected Map<DTNHost, Set<DTNHost>> neighboursNode; // menyimpan daftar tetangga dari ego node

	protected double[][] matrixEgoNetwork; // menyimpan nilai matrix ego network
	protected double[][] indirectNodeMatrix; //menyimpan matrix indirect node
	
	protected double betweennessCentrality;// menyimpan nilai betweenness centrality
	
	protected double a; //menyimpan konstanta untuk variabel similarity
	protected double b= 1-a; //menyimpan konstanta untuk variabel betweenness
	protected double beta;
	protected double pinit;
	protected double lastAgeUpdate;
	protected int 	 secondsInTimeUnit;
	
	ArrayList<DTNHost> indirectNode, directNode; //menyimpan indirect node => m dan direct node+host => n

	protected SimilarityCounter similarity;
	protected CentralityDetection centrality;
	private Map<DTNHost, Double> preds;

	public SimBetWProphet(Settings s) {

		 if (s.contains(CENTRALITY_ALG_SETTING))
			 this.centrality = (CentralityDetection) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
		 else
			 this.centrality = new BetweennessCentrality(s);

		if (s.contains(SIMILARITY_SETTING))
			this.similarity = (SimilarityCounter) s.createIntializedObject(s.getSetting(SIMILARITY_SETTING));
		else
			this.similarity = new NeighbourhoodSimilarity(s);
		
		this.a = s.getDouble(A_SETTING);

		if(s.contains(BETA_SETTING)) //Prophet
			beta = s.getDouble(BETA_SETTING);
		else
			beta = DEFAULT_BETA;

		if(s.contains(P_INIT_SETTING))
			pinit = s.getDouble(P_INIT_SETTING);
		else
			pinit = DEFAULT_P_INIT;

		if(s.contains(SECONDS_IN_UNIT_S))
			secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
		else
			secondsInTimeUnit = DEFAULT_UNIT;

		preds = new HashMap<DTNHost, Double>();
		this.lastAgeUpdate = 0.0;

	}

	public SimBetWProphet(SimBetWProphet proto) {
		
		neighboursNode = new HashMap<DTNHost, Set<DTNHost>>();
		indirectNode= new ArrayList<DTNHost>();
		directNode= new ArrayList<DTNHost>();
		this.a = proto.a;		
		this.centrality = proto.centrality.replicate();
		this.similarity = proto.similarity.replicate();

		beta = proto.beta; // Prophet Decision Engine
		pinit = proto.pinit;
		secondsInTimeUnit = proto.secondsInTimeUnit;
		preds = new HashMap<DTNHost, Double>();
		this.lastAgeUpdate = proto.lastAgeUpdate;
	}

	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {}

	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {}

	@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {

		DTNHost myHost = con.getOtherNode(peer);
		SimBetWProphet de = this.getOtherDecisionEngine(peer);

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
		}

		this.updateBetweenness(myHost); // mengupdate nilai betweenness
		this.updateSimilarity(myHost); //mengupdate indirect node

		Set<DTNHost> hostSet = new HashSet<DTNHost>(this.preds.size() //Prophet Decision Engine
				+ de.preds.size());
		hostSet.addAll(this.preds.keySet());
		hostSet.addAll(de.preds.keySet());

		this.agePreds();
		de.agePreds();

		// Update preds for this connection
		double myOldValue = this.getPredFor(peer),
				peerOldValue = de.getPredFor(myHost),
				myPforHost = myOldValue + (1 - myOldValue) * pinit,
				peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;
		preds.put(peer, myPforHost);
		de.preds.put(myHost, peerPforMe);

		// Update transistivities
		for(DTNHost h : hostSet)
		{
			myOldValue = 0.0;
			peerOldValue = 0.0;

			if(preds.containsKey(h)) myOldValue = preds.get(h);
			if(de.preds.containsKey(h)) peerOldValue = de.preds.get(h);

			if(h != myHost)
				preds.put(h, myOldValue + ( 1 - myOldValue) * myPforHost * peerOldValue * beta);
			if(h != peer)
				de.preds.put(h, peerOldValue + ( 1 - peerOldValue) * peerPforMe * myOldValue * beta);
		}
	}

	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
		
		SimBetWProphet de = getOtherDecisionEngine(otherHost);
		DTNHost dest = m.getTo();

		if (isFinalDest(m, otherHost))
			return true;
		//hitung nilai simbet util saya
		double  mySimbetUtil = this.countSimBetUtil(de.getSimilarity(dest),de.getBetweennessCentrality(), 
				this.getSimilarity(dest), this.getBetweennessCentrality());
		
		//hitung nilai simbet util teman saya
		double peerSimBetUtil = this.countSimBetUtil(this.getSimilarity(dest), this.getBetweennessCentrality(), 
				de.getSimilarity(dest), de.getBetweennessCentrality());
		//hitung deliv pred

		/*routing dengan kombinasi similarity & betweenness*/
		if ( peerSimBetUtil > mySimbetUtil)
			 return true;
		else
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
		SimBetWProphet hostDDE = getOtherDecisionEngine(dest);
			
			double[][] neighboursAdj = new double[dummyArrayN.size()][1];

			for (int i = 0; i < dummyArrayN.size(); i++) {
				for (int j = 0; j < 1; j++) {
					if (i==0) {
						neighboursAdj[i][j]=0;
					}
					else if (neighboursNode.get(dummyArrayN.get(i)).contains(dest)) {
//						neighboursAdj[i][j] = 1;
						double value = hostDDE.getPredFor(dummyArrayN.get(i));
						neighboursAdj[i][j] = value;
//
					} else {
						neighboursAdj[i][j] = 0;
						
					}
				}
			}

			this.indirectNodeMatrix = neighboursAdj;
		}

	// method yang digunakan untuk membangun matriks ego network
	private void buildEgoNetwork(Map<DTNHost, Set<DTNHost>> neighboursNode, DTNHost host) {
		ArrayList<DTNHost> dummyArray = buildDummyArray(neighboursNode, host);
		SimBetWProphet hostDE = getOtherDecisionEngine(host);

		double[][] neighboursAdj = new double[dummyArray.size()][dummyArray.size()];
		for (int i = 0; i < dummyArray.size(); i++) {
			for (int j = i; j < dummyArray.size(); j++) {
				if (i == j) {
					neighboursAdj[i][j] = 0;
				} else if (neighboursNode.get(dummyArray.get(j)).contains(dummyArray.get(i))) {
//					neighboursAdj[i][j] = 1;

					double value = hostDE.getPredFor(dummyArray.get(i));
					if (value >= 1)
						value  = 1.0;
					neighboursAdj[i][j] = value;
//
					neighboursAdj[j][i] = neighboursAdj[i][j];
					System.out.println(value);
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
	
	private double countSimBetUtil(double simPeerForDest, double betweennessPeer, double mySimForDest, double myBetweenness ){
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

	private SimBetWProphet getOtherDecisionEngine(DTNHost otherHost) {
		MessageRouter otherRouter = otherHost.getRouter();
		assert otherRouter instanceof DecisionEngineRouter : "This router only works "
				+ " with other routers of same type";

		return (SimBetWProphet) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
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
		return new SimBetWProphet(this);
	}

	private void agePreds()//Prophet
	{
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
				secondsInTimeUnit;

		if (timeDiff == 0) {

			return;
		}

		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet())
		{
			e.setValue(e.getValue()*mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	public double getPredFor(DTNHost host) { //Prophet
		agePreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}

}
