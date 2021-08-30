package TCYB;

import org.coinor.opents.BestEverAspirationCriteria;
import org.coinor.opents.TabuList;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class SATabu {

    // modes: GlobalSample, LocalSample
    public static void main(String argv[]) throws FileNotFoundException {
        String fileName = argv[0];
//        String j = argv[1];
//        String mode = argv[2];


        int m = 0;
        int budget = 0;
        String mode = "LocalSample";

        if (fileName.startsWith("2520")) {
            m = 18;
            budget = 20000;
        }
        else if (fileName.startsWith("5040")) {
            m = 36;
            budget = 50000;
        }
        else{
            throw new UnsupportedOperationException("unknown config");
        }

        for (int j = 0; j < 5; j++) {
            System.out.println("file-"+fileName+"-"+j+"-mode-"+mode);
            PrintStream print=new PrintStream("SA-Tabu-"+fileName+"-expr-"+j+"-mode-"+mode);  //写好输出位置文件；
            System.setOut(print);
            runWithParameters(fileName, m, 0.7, Integer.MAX_VALUE, budget,2000, mode);
        }
    }


    public static double runWithParameters(String filename, int m, double tabuSizeCoff, int timeLimit, int budgetLimit, int sampleLength,
                                           String mode){

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

        if (mode.equals("GlobalSample"))
            initialSampling(filename, slap, search, sampleLength, false);

        GCObjectiveFunction objFunc = new GCObjectiveFunction();

        Layout layout = null;

        while(layout == null){
            layout = search.init(slap);
        }

        search.LNS(layout, 256);

        int tabuSize = (int)( slap.product.values().size() * tabuSizeCoff);

        System.out.println("Setting tabu tenure to: " + tabuSize);

        GCSolution initialSolution  = new GCSolution(layout);
        GCMoveManager   moveManager = new GCMoveManager(search, 0.1, 0.05, mode);
        TabuList tabuList = new zeroTabuList( tabuSize ); // In OpenTS package

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
        ((MySingleThreadTabuSearch)tabuSearch).setBudgetLimit(budgetLimit);

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

    private static void initialSampling(String filename, SLAP slap, Search search, int sampleLength, boolean load){

        double[][] upperVar = new double[sampleLength][slap.product.size()];
        double[] obj = new double[sampleLength];

        if (load){
            readSampling(filename, upperVar, obj, sampleLength);

        }else{
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < sampleLength; i++) {
                Layout soln = search.init(slap);
                search.LNS(soln, 256);

                ArrayList<ArrayList<Layout.GROUP>> layoutCode = soln.encode(soln.row);
                double[] var = Surrogate.getUpperVar(layoutCode, slap);
                double objVal = soln.objective();
                upperVar[i] = var;
                obj[i] = objVal;
            }
            System.out.println("Sampling cost: " + Search.getElapsedTime(startTime)/1000);
            saveSampling(filename, upperVar, obj);
        }

        search.updateDataset(upperVar, obj);
    }

    private static void saveSampling(String filename, double[][] upperVar, double[] obj){
        File file_upper =new File("sample_upperVar-"+filename+".dat");
        File file_obj =new File("sample_obj-"+filename+".dat");
        FileOutputStream out;
        try {
            out = new FileOutputStream(file_upper);
            ObjectOutputStream objOut=new ObjectOutputStream(out);
            objOut.writeObject(upperVar);
            objOut.flush();
            objOut.close();
            System.out.println("write object success!");
        } catch (IOException e) {
            System.out.println("write object failed");
            e.printStackTrace();
        }
        try {
            out = new FileOutputStream(file_obj);
            ObjectOutputStream objOut=new ObjectOutputStream(out);
            objOut.writeObject(obj);
            objOut.flush();
            objOut.close();
            System.out.println("write object success!");
        } catch (IOException e) {
            System.out.println("write object failed");
            e.printStackTrace();
        }
    }

    private static void readSampling(String filename, double[][] upperVar, double[] obj, int sampleLength){
        double[][] temp_Var = null;
        File file_upper = new File("sample_upperVar-"+filename+".dat");
        FileInputStream in;
        try {
            in = new FileInputStream(file_upper);
            ObjectInputStream objIn=new ObjectInputStream(in);
            temp_Var = (double[][]) objIn.readObject();
            objIn.close();
            System.out.println("read object success!");
        } catch (IOException e) {
            System.out.println("read object failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        double[] temp_obj = null;
        File file_obj = new File("sample_obj-"+filename+".dat");
        try {
            in = new FileInputStream(file_obj);
            ObjectInputStream objIn = new ObjectInputStream(in);
            temp_obj = (double[]) objIn.readObject();
            objIn.close();
            System.out.println("read object success!");
        } catch (IOException e) {
            System.out.println("read object failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.arraycopy(temp_Var, 0, upperVar, 0, sampleLength);
        System.arraycopy(temp_obj, 0, obj, 0, sampleLength);
    }
}
