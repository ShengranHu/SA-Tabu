package TCYB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import org.coinor.opents.BestEverAspirationCriteria;
import org.coinor.opents.MoveManager;
import org.coinor.opents.ObjectiveFunction;
import org.coinor.opents.TabuList;
import org.coinor.opents.TabuSearch;


public class Run {
 	
	public static void main(String argv[]) throws FileNotFoundException {

        runWithParameters("5040-1", 36, 0.7, 5000);
	}
	
	
    public static double runWithParameters(String filename, int m, double tabuSizeCoff, int timeLimit){

    	//Set a fixed seed if needed
    	long seed = System.currentTimeMillis();
    	Random random = new Random(seed);
    	    	
    	System.out.println("Random seed: " + seed);
    	
    	SLAP slap = new SLAP(filename, m);
		long startTime = System.currentTimeMillis();
		
		System.out.println("Solving problem " + filename + " with " + slap.n + " items " + slap.nS + " rows " + slap.nL + " locations");
		System.out.println("Tabu size: " + tabuSizeCoff + " \t timelimit: " + timeLimit);
		
		//mode is set to 3 by default, use SplitRandom
		Search search = new Search(3, random);
	
    		
        GCObjectiveFunction objFunc = new GCObjectiveFunction();
        
        Layout layout = null;

        while(layout == null){
        	layout = search.init(slap);
        }

        search.LNS(layout, 256);
        
        int tabuSize = (int)( slap.product.values().size() * tabuSizeCoff);
        
        System.out.println("Setting tabu tenure to: " + tabuSize);
        
        GCSolution initialSolution  = new GCSolution(layout);
        GCMoveManager   moveManager = new GCMoveManager(search, 1, 0);
        TabuList         tabuList = new zeroTabuList( tabuSize ); // In OpenTS package
        
        ((zeroTabuList)tabuList).setTenure(tabuSize);
        
        // Create Tabu Search object
        MySingleThreadTabuSearch tabuSearch = new MySingleThreadTabuSearch(
                initialSolution,
                moveManager,
                objFunc,
                tabuList,
                new BestEverAspirationCriteria(), // In OpenTS package
                false , // maximizing = yes/no; false means minimizing
                true); //Verbose = true to print out some detail information
        
        // Show initial solution
        GCSolution ini = (GCSolution)initialSolution;
       
        // Start solving
        int iters = slap.n*slap.n;
        System.out.println("Set iteration to go as:" + iters);
        System.out.println("Initial Solution: " + ini.getObjectiveValue()[0]);
        tabuSearch.setIterationsToGo(iters);
        
        ((MySingleThreadTabuSearch)tabuSearch).setTimeLimit(timeLimit);
        
        //will get at least 100 entries, or record every 5 second.
        double interval = (double)(timeLimit)/100;
        tabuSearch.setRecordInterval(interval >= 5 ? 5 : interval);
        tabuSearch.setItrsNoImproveToStop(1000);
        tabuSearch.startSolving();

        //Show best solution
       
        GCSolution best = (GCSolution)tabuSearch.getBestSolution();
        
        if(!best.layout.isFeasible(false)){
        	best.layout.isFeasible(true); //print out the debug information
        }
        
        System.out.println( "Best Solution: \n" + ((MySingleThreadTabuSearch)tabuSearch).getElapsedTime() + 
        							"\t" + best.getObjectiveValue()[0]  + "\t" + tabuSearch.runnedItrs);
        
        best.layout.printLayout();
        
        return best.getObjectiveValue()[0] ;
    }
}
