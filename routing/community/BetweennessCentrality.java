package routing.community;
/**
 *
 * @author MSI GAMING
 * Improved By Peter.G (155314027)
 */
import java.util.*;

import core.*;

public class BetweennessCentrality implements CentralityDetection{
		
	public BetweennessCentrality(Settings s) {}
	
	public BetweennessCentrality(BetweennessCentrality proto) {}
	
	public double getCentrality(double[][] matrixEgoNetwork) {
		double[][] ones= new double[matrixEgoNetwork.length][matrixEgoNetwork.length];
		
		for(double[] ones1 : ones){
			for (int i = 0; i < ones.length; i++) {
				ones1[i]=1;
			}
		}
		
		double[][] result = matrixMultiplexing(neighboursAdjSquare(matrixEgoNetwork), matrixDecrement(ones, matrixEgoNetwork));
		
		ArrayList<Double> val= new ArrayList<>();
		for (int i = 0; i < result.length; i++) {
			for (int j = i+1; j < result.length; j++) {
				if(result[i][j]!=0){
					val.add(result[i][j]);
				}
			}
		}
		
		double betweennessVal=0;
		for (Double val1 : val) {
			betweennessVal=betweennessVal+(1/val1);
		}
		
		return betweennessVal;
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
	public CentralityDetection replicate() {
		// TODO Auto-generated method stub
		return new BetweennessCentrality(this);
	}


}
