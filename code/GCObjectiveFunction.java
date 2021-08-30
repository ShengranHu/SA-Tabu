package TCYB;

import java.util.ArrayList;
import java.util.Hashtable;
import org.coinor.opents.*;

public class GCObjectiveFunction implements ObjectiveFunction{
	
	//public Search search;
	
	public GCObjectiveFunction(){
	}
	
	
	public double[] evaluate( Solution solution, Move move){
		Solution newSolution = (Solution)solution.clone();
		
		if (move != null) {
			// if move is null, calculate the objective value directly
			// Move first then calculate
			move.operateOn(newSolution);
		}
		
		double[] obj = newSolution.getObjectiveValue();
			
		try{
			return  obj;
		}catch(Exception e){
			System.out.println(e.getMessage());
			 e.printStackTrace();
			return new double[] {Double.POSITIVE_INFINITY};
		}
	}//End evaluate
	
	
}
