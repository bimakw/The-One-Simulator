package routing.community;

/*Kelas ini dibuat oleh: Elisabeth Kusuma
 * Universitas Sanata Dharma, Yogyakarta
 * :p */

import java.util.*;

import core.*;

public class BetweennessCentralityImproved implements CentralityDetectionImproved{
	//protected Map<DTNHost, ArrayList<Double>> neighborsHistory;
	public BetweennessCentralityImproved(Settings s) {}
	
	public BetweennessCentralityImproved(BetweennessCentralityImproved proto) {}
	
	public double getCentrality(double[][] matrixEgoNetwork,  Map<DTNHost, ArrayList<Double>> neighborsHistory) {//di edit kie!
		double[][] ones= new double[matrixEgoNetwork.length][matrixEgoNetwork.length];
		//double value = countAgrIntStrength(neighborsHistory);
		for(double[] ones1 : ones){
			for (int i = 0; i < ones.length; i++) {
//				ones1[i]=1;
				ones1[i]=this.countAggrIntStrength(neighborsHistory);;//kemarin tak ubah
//				System.out.println(ones1[i]);
				//ones1[i]=value;   //
			}
		}

		double[][] result = matrixMultiplexing(neighboursAdjSquare(matrixEgoNetwork), matrixDecrement(ones, matrixEgoNetwork));
		
		ArrayList<Double> val= new ArrayList<>();
		for (int i = 0; i < result.length; i++) {
			for (int j = i+1; j < result.length; j++) {
				if(result[i][j]==0){
					val.add(result[i][j]);
				}
			}
		}
		
		double betweennessVal= 0;
		for (Double val1 : val) {
			betweennessVal=betweennessVal+(1/val1);
		}
//		System.out.println(betweennessVal);
		return betweennessVal;

	}

	protected double AgrIntStrength(double lambda, double sigma) {
		return lambda * (lambda - sigma);
	}

	public double countAggrIntStrength(Map<DTNHost, ArrayList<Double>> neighborsHist) {
		double AggreIntStrength = 0;
		double lambda = 0, sigma = 0;
		for (Map.Entry<DTNHost, ArrayList<Double>> data : neighborsHist.entrySet()) {
			lambda = data.getValue().get(0);
			sigma = data.getValue().get(1);

			AggreIntStrength = this.AgrIntStrength(lambda, sigma);
		}

		return AggreIntStrength;
	}

	public double[][] neighboursAdjSquare(double[][] neighboursAdj){

		double result[][]=new double[neighboursAdj.length][neighboursAdj[0].length];
        for(int i=0;i<result.length;i++)
        {
            for(int j=0;j<result[0].length;j++)
            {
                for(int k=0;k<neighboursAdj[0].length;k++)
                {
                    result[i][j]+=neighboursAdj[i][k]*neighboursAdj[k][j];
                }
            }
        }
        return (result);
	}
	
	public double[][] matrixDecrement(double[][] ones, double[][] neighboursAdj) {
		double[][] result= new double[ones.length][ones.length];
		
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result.length; j++) {
				result[i][j]= ones[i][j]-neighboursAdj[i][j];
			}
		}
		
		return result;
	}
	
	public double[][] matrixMultiplexing(double[][] neighboursAdjSquare, double[][] decrementMatrix) {
		double[][] result= new double[neighboursAdjSquare.length][neighboursAdjSquare.length];
		
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result.length; j++) {
				result[i][j]= neighboursAdjSquare[i][j]*decrementMatrix[i][j];
			}
		}
		
		return result;
	}
	


	@Override
	public CentralityDetectionImproved replicate() {
		// TODO Auto-generated method stub
		return new BetweennessCentralityImproved(this);
	}


}
