package routing.community;

/*Kelas ini dibuat oleh: Elisabeth Kusuma
 * untuk diimplementasikan dalam kelas Betweenness Centrality
 * Universitas Sanata Dharma, Yogyakarta
 * :p */

public interface CentralityDetection {
	
	public double getCentrality(double[][] matrixEgoNetwork);
	public CentralityDetection replicate();
}
