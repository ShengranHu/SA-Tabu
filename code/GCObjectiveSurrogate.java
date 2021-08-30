package TCYB;

import org.coinor.opents.Move;
import org.coinor.opents.ObjectiveFunction;
import org.coinor.opents.Solution;

public class GCObjectiveSurrogate implements ObjectiveFunction {

    //public Search search;

    public GCObjectiveSurrogate(){
    }


    public double[] evaluate(Solution solution, Move move){
        Solution newSolution = (Solution)solution.clone();


        if (move != null) {
            // if move is null, calculate the objective value directly
            // Move first then calculate
            ((GCSolution)solution).layout.predicted = true;
            ((GCMove) move).surrogateOperateOn(newSolution);
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