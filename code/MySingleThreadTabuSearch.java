/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */

package TCYB;

import javafx.util.Pair;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.coinor.opents.*;

import javax.swing.*;
import java.util.*;

import org.apache.commons.math3.util.CombinatoricsUtils;


public class MySingleThreadTabuSearch extends TabuSearchBase
{
    
    
    /** Objective function. */
    protected GCObjectiveFunction objectiveFunction;
   
    /** Move manager. */
    protected GCMoveManager moveManager;
    
    /** Tabu list. */
    protected TabuList tabuList;
    
    /** Aspiration criteria. */
    protected AspirationCriteria aspirationCriteria;
    
    /** Current solution. */
    protected GCSolution currentSolution;
    
    /** Best solution. */
    protected GCSolution bestSolution;
    
    /** Iterations to go. */
    protected int iterationsToGo;
    protected int runnedItrs;
    
    /** Maximizing: true. Minimizing: false. */
    protected boolean maximizing;
    
    /** Whether or not the the tabu search is solving. */
    protected boolean solving;
    
    /** Whether or not the tabu search should keep solving if it gets a chance to quit. */
    protected boolean keepSolving;
    
    /** Fire new current solution event at the end of the iteration. */
    protected boolean fireNewCurrentSolution;
    
    /** Fire new best solution event at the end of the iteration. */
    protected boolean fireNewBestSolution;
    
    /** Fire unimproving solution event at the end of the iteration. */
    protected boolean fireUnimprovingMoveMade;
    
    /** Fire improving solution event at the end of the iteration. */
    protected boolean fireImprovingMoveMade;
    
    /** Fire no change in value solution event at the end of the iteration. */
    protected boolean fireNoChangeInValueMoveMade;
    
       
    /** Choose first improving neighbor instead of best neighbor overall. */
    protected boolean chooseFirstImprovingMove = false;
    
    /** Print errors to this stream. */
    protected static java.io.PrintStream err = System.err;
    
    public boolean Verbose;
    
    public long StartTime;
    public double timeLimit;
    public int budgetLimit;
    
    public double recordIntervals=1;
    
    protected int itrsNoImprovToStop;
    protected int lastImprovedItrs;
    
    public void setItrsNoImproveToStop(int itrs){
    	this.itrsNoImprovToStop = itrs;
    }
    
/* ********  C O N S T R U C T O R S  ******** */
    
    
    /**
     * Constructs a <tt>SingleThreadedTabuSearch</tt> with no tabu objects set.
     *
     * @since 1.0
     */
    public MySingleThreadTabuSearch()
    {
        super();
        
    }   // end constructor
    
    public void setRecordInterval(double sec){
    	this.recordIntervals = sec;
    }
    
    /**
     * Constructs a <tt>SingleThreadedTabuSearch</tt> with all tabu objects set.
     * The initial solution is evaluated with the objective function,
     * becomes the <tt>currentSolution</tt> 
     * and a copy becomes the <tt>bestSolution</tt>.
     *
     * @param initialSolution The initial <tt>currentSolution</tt>
     * @param moveManager The move manager
     * @param objectiveFunction The objective function
     * @param tabuList The tabu list
     * @param aspirationCriteria The aspiration criteria or <tt>null</tt> if none is to be used
     * @param maximizing Whether or not the tabu search should be maximizing the objective function
     *
     * @see Solution
     * @see ObjectiveFunction
     * @see MoveManager
     * @see TabuList
     * @see AspirationCriteria
     *
     * @since 1.0
     */
    public MySingleThreadTabuSearch(
    GCSolution initialSolution,
    GCMoveManager moveManager,
    GCObjectiveFunction objectiveFunction,
    TabuList tabuList,
    AspirationCriteria aspirationCriteria,
    boolean maximizing,
    boolean Verbose)
    {
        this();
        
        // Make sure initial solution is evaluated.
        double[] val = objectiveFunction.evaluate( initialSolution, null );
        initialSolution.setObjectiveValue( val );
        
        // Set current solution to initial solution 
        // and best solution to a copy of the initial solution.
        //setCurrentSolution( initialSolution );
        //setBestSolution( (Solution) initialSolution.clone() );
        this.currentSolution = initialSolution;
        this.bestSolution = initialSolution.clone();
        
        // Set tabu objects
        this.objectiveFunction      =   objectiveFunction;
        this.moveManager            =   moveManager;
        this.tabuList               =   tabuList;
        this.aspirationCriteria     =   aspirationCriteria;
        this.maximizing             =   maximizing;
        this.Verbose				=   Verbose;
        
        
        this.StartTime = System.currentTimeMillis();
        
    }   // end constructor
    
    
    public void setTimeLimit(double sec){
    	this.timeLimit = sec;
    }

    public void setBudgetLimit(int budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public double getElapsedTime(){
    	return (System.currentTimeMillis() - this.StartTime);
    }
    
    /**
     * This large method goes through one iteration. It performs these steps:
     *
     * <ul>
     *   <li>Get copies of tabu objects</li>
     *   <li>If there is no current solution, throw an exception</li>
     *   <li>If the best solution is null, set it to a copy of the current solution</li>
     *   <li>Clear event-queuing flags</li>
     *   <li>Get list of moves to try</li>
     *   <li>Find the best of those moves</li>
     *   <li>Register the best move</li>
     *   <li>Calculate new solution value using previously evaluated move's contribution</li>
     *   <li>Determine if move is unimproving</li>
     *   <li>Determine if new solution will be new best</li>
     *   <li>Operate on the current solution, making the new solution</li>
     *   <li>Set the new solution's value</li>
     *   <li>If new best, clone the solution</li>
     *   <li>Fire queued events</li>
     * </ul>
     *
     * @see #getBestMove
     * @see #isFirstBetterThanSecond
     * @see #fireQueuedEvents
     * @since 1.0
     */
    protected void performOneIteration() throws NoMovesGeneratedException, NoCurrentSolutionException
    {
        // Grab local copies of the problem.
        final   TabuList                tabuList            = getTabuList();
        final   GCMoveManager             moveManager         = getMoveManager();
        final   GCObjectiveFunction       objectiveFunction   = getObjectiveFunction();
        final   AspirationCriteria      aspirationCriteria  = getAspirationCriteria();
        final   GCSolution                currentSolution     = getCurrentSolution();
                GCSolution                bestSolution        = getBestSolution();
        final   boolean                 chooseFirstImproving = isChooseFirstImprovingMove();
        final   boolean                 maximizing          = isMaximizing();
          
        // Check for null solutions
        if( currentSolution == null )
            throw new NoCurrentSolutionException();
        
        if( bestSolution == null )
        {   
            bestSolution =  currentSolution.clone();
            if(bestSolution.getObjectiveValue()[0] != currentSolution.getObjectiveValue()[0])
            	System.out.println("!!!!!" + bestSolution.getObjectiveValue()[0] + "\t" + currentSolution.getObjectiveValue()[0]);
            
            internalSetBestSolution( bestSolution );
        }   // end if: null best solution
        
        // Clear event-queuing flags.
        this.fireNewCurrentSolution      = false;
        this.fireNewBestSolution         = false;
        this.fireUnimprovingMoveMade     = false;
        this.fireImprovingMoveMade       = false;       
        this.fireNoChangeInValueMoveMade = false;
        
        // Get list of moves to try
        final GCMove[] moves = moveManager.getAllMoves( currentSolution );
        if( moves == null || moves.length == 0 )
            throw new NoMovesGeneratedException();

        final GCMove[] allMoves = moveManager.getAllMoves(currentSolution, 1.0);
        
        
        // Get best move. 
        // Returns an array where the first element is the best move,
        // the second element is the resulting objective function value,
        // and the third element is the tabu status.
        // This way we don't evaluate the move twice.
        final Object[] bestMoveArr = getBestMove( 
            currentSolution, moves, allMoves, objectiveFunction, tabuList,
            aspirationCriteria, maximizing, chooseFirstImproving );
        final GCMove bestMove = (GCMove)bestMoveArr[0];
        final double[] bestMoveVal = (double[])bestMoveArr[1];
        final boolean bestMoveTabu = ((Boolean)bestMoveArr[2]).booleanValue();
       
        
        // Register the move that's about to be made with the tabu list.
        tabuList.setTabu( currentSolution, bestMove );
        
        
        // Get the old and new solution value
        final double[] oldVal = currentSolution.getObjectiveValue();
            
        
        // Determine if the new value is better or worse (equal is neither)
        if( isFirstBetterThanSecond( oldVal, bestMoveVal, maximizing ) )
            this.fireUnimprovingMoveMade = true;
        else if( isFirstBetterThanSecond( bestMoveVal, oldVal, maximizing ) )
            this.fireImprovingMoveMade = true;
        else this.fireNoChangeInValueMoveMade = true;
        
        // If the new value is improving, see if it's a new best solution
        boolean newBestSoln = false;
        if( this.fireImprovingMoveMade )
            if( isFirstBetterThanSecond( bestMoveVal, bestSolution.getObjectiveValue(), maximizing ) )
                newBestSoln = true;
        
        
//        // Operate on the solution
        try{
        	bestMove.operateOn( currentSolution );
        }catch(Exception e ){
            System.err.println( "Error with " + bestMove + " on " + currentSolution );
        }
        
        // Set the new solution value       
        // Update the best solution, too?
        if( newBestSoln )
        {   GCSolution newBest = currentSolution.clone();
            internalSetBestSolution( newBest );
        }   // end if: new best soln
        
        // Update current solution
        internalSetCurrentSolution( currentSolution );
        
        
        // Fire relevant events
        fireQueuedEvents();
        
    }   // end performOneIteration
    
         
    /**
     * Gets the best move--one that should be used for this iteration.
     * By setting <var>chooseFirstImprovingMove</var> to <tt>true</tt>
     * you tell the tabu search to return the first move it encounters
     * that is improving and non-tabu rather than search through all of
     * the moves.
     *
     * It's not static so that when the MultiThreadedTabuSearch invokes
     * the performOneIteration method the proper method is invoked.
     * Java's weird about overriding static methods...
     *
     * @since 1.0
     */
    protected  Object[] getBestMove( 
    final GCSolution soln, 
    final GCMove[] moves,
    final GCMove[] allMoves,
    final GCObjectiveFunction objectiveFunction, 
    final TabuList tabuList, 
    final AspirationCriteria aspirationCriteria, 
    final boolean maximizing, 
    final boolean chooseFirstImprovingMove )
    {
        return MySingleThreadTabuSearch.getBestMove( soln, moves, allMoves, objectiveFunction,
                            tabuList, aspirationCriteria,
                            maximizing, chooseFirstImprovingMove, this );
    }   // end getBestMove
    
    
    /**
     * The static method that actually does the work. It's static so that
     * the NeighborhoodHelper in the MultiThreadedTabuSearch can
     * use the same code.
     *
     * @since 1.0
     */
    protected static Object[] getBestMove(
    final GCSolution soln,
    final GCMove[] moves,
    final GCObjectiveFunction objectiveFunction,
    final TabuList tabuList,
    final AspirationCriteria aspirationCriteria,
    final boolean maximizing,
    final boolean chooseFirstImprovingMove,
    final TabuSearch This )
    {
        // Set up variables
        Move bestMove = moves[0];
        double[] bestMoveVal = {};
        boolean bestMoveTabu = false;

        GCSolution tmp = soln.clone();

//        // Set up first move
//        bestMoveVal = objectiveFunction.evaluate( tmp, bestMove );
//        bestMoveTabu = moves.length == 0 ?
//            false : // Don't bother calling the tabu list if there's only one move.
//            isTabu( soln, bestMove, bestMoveVal, tabuList, aspirationCriteria, This );

        for(int i = 0; i < moves.length; i++ )
        {
            Move move = moves[i];

            // Since the tabu status has not yet been determined, do the
            // objective value comparisons first. Reasoning: comparing a handful
            // of doubles is likely to be faster than whatever kind of tabu
            // list the user has set up.
            tmp = soln.clone();
            double[] newObjVal = objectiveFunction.evaluate( soln, move );
            if(i==0){
                bestMoveVal = newObjVal;
                bestMoveTabu = isTabu( soln, bestMove, bestMoveVal, tabuList, aspirationCriteria, This );
            }
            if( isFirstBetterThanSecond( newObjVal, bestMoveVal, maximizing ) )
            {
                // New one has a better objective value.
                // Check the tabu status of both.
                // Do not switch over only if the new one is tabu, but the old one isn't.
                boolean newIsTabu = isTabu( soln, move, newObjVal, tabuList, aspirationCriteria, This );

                if( !(!bestMoveTabu && newIsTabu) )
                {   bestMove = move;
                    bestMoveVal = newObjVal;
                    bestMoveTabu = newIsTabu;
                }   // end if: switch over
            }   // end if: new one has better objective value
            else
            {   // New one does not have better objective value, but see if it
                // has a better tabu status.
                boolean newIsTabu;
                if( bestMoveTabu && !isTabu( soln, move, newObjVal, tabuList, aspirationCriteria, This ) )
                {   bestMove = move;
                    bestMoveVal = newObjVal;
                    bestMoveTabu = false;
                }   // end if: old was tabu, new one isn't.
            }   // end else: new one does not have better objective value
        }   // end for: through remaining moves


        /**
         * 15/07/2016
         * The evaluation used by our tabu is a non-fixed local search method, hence every evaluation is a bit different
         * We return a copy of the solution
         */
        return new Object[]{ bestMove, bestMoveVal, new Boolean( bestMoveTabu)};
    }   // end getBestMove

    protected static Object[] getBestMove(
            final GCSolution soln,
            final GCMove[] moves,
            final GCMove[] allMoves,
            final GCObjectiveFunction objectiveFunction,
            final TabuList tabuList,
            final AspirationCriteria aspirationCriteria,
            final boolean maximizing,
            final boolean chooseFirstImprovingMove,
            final TabuSearch This )
    {
        // Set up variables
        GCObjectiveSurrogate objectiveSurrogate = new GCObjectiveSurrogate();
        Search search = ((GCMoveManager) This.getMoveManager()).search;

        // Split Neigh
        int evaluateLength = (int) Math.ceil((double) allMoves.length * ((GCMoveManager) This.getMoveManager()).threshold);
        int predictLength = allMoves.length - evaluateLength;

        GCMove[] evaluateMove = new GCMove[evaluateLength];
        GCMove[] predictMove = new GCMove[predictLength];

        System.arraycopy(allMoves, 0, evaluateMove, 0, evaluateLength);
        System.arraycopy(allMoves, evaluateLength, predictMove, 0, predictLength);

        Move bestMove = evaluateMove[0];
        double[] bestMoveVal = {};
        boolean bestMoveTabu = false;

        ArrayList<Pair<Move, double[]>> evaluatedSet = new ArrayList<>();
        ArrayList<Pair<Move, double[]>> predictedSet = new ArrayList<>();

        // real evaluate move in moves
        for(int i = 0; i < evaluateMove.length; i++ )
        {
            Move move = evaluateMove[i];

            GCSolution tmp = soln.clone();
            double[] newObjVal = objectiveFunction.evaluate( soln, move );
            evaluatedSet.add(new Pair<>(move, newObjVal));
        }


        // use these to update surrogate

//        long startTime = System.currentTimeMillis();
        if (((GCMoveManager) This.getMoveManager()).mode.equals("LocalSample"))
            search.dataset = new ArrayList<>();
        updateSurrogate(search, evaluatedSet, soln.layout.slap);
//        System.out.println("update surrogate search: " + Search.getElapsedTime(startTime)/1000);


        // use surrogate to predict un-evaluated move
//        startTime = System.currentTimeMillis();

        for (int i = 0; i < predictMove.length; i++) {
            Move move = predictMove[i];

            double[] newObjVal = objectiveSurrogate.evaluate(soln, move);

            predictedSet.add(new Pair<>(move, newObjVal));
        }

//        System.out.println("use surrogate predict: " + Search.getElapsedTime(startTime)/1000);

        // sort predictedSet

        Comparator<Pair<Move, double[]>> cmp = new Comparator<Pair<Move, double[]>>(){
            @Override
            public int compare(Pair<Move, double[]> p1, Pair<Move, double[]> p2){

//                Returns:
//                a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.

                double p1Val = p1.getValue()[0];
                double p2Val = p2.getValue()[0];

                boolean p1IsTabu = isTabu( soln, p1.getKey(), p1.getValue(), tabuList, aspirationCriteria, This );
                boolean p2IsTabu = isTabu( soln, p2.getKey(), p2.getValue(), tabuList, aspirationCriteria, This );

                if ( (!p1IsTabu && !p2IsTabu) || (p1IsTabu && p2IsTabu) ){
                    return Double.compare(p1Val, p2Val);
                }else if(!p1IsTabu){
                    return -1;
                }else{
                    return 1;
                }

            }
        };

        Collections.sort(predictedSet, cmp);

        // real evaluate % of predictedSet
        int reEvaluateLength = (int) Math.ceil((double) allMoves.length *
                ((GCMoveManager) This.getMoveManager()).evaluatePredictionThreshold);

        for (int i = 0; i < reEvaluateLength; i++) {
            Move move = predictMove[i];

            GCSolution tmp = soln.clone();
            double[] newObjVal = objectiveFunction.evaluate( soln, move );
            evaluatedSet.add(new Pair<>(move, newObjVal));
        }

        Collections.sort(evaluatedSet, cmp);

        bestMove = evaluatedSet.get(0).getKey();
        bestMoveVal = evaluatedSet.get(0).getValue();


        /**
         * 15/07/2016
         * The evaluation used by our tabu is a non-fixed local search method, hence every evaluation is a bit different
         * We return a copy of the solution
         */
        return new Object[]{ bestMove, bestMoveVal, new Boolean( bestMoveTabu)};
    }   // end getBestMove

//    protected static Object[] getBestMove(
//            final GCSolution soln,
//            final GCMove[] moves,
//            final GCMove[] allMoves,
//            final GCObjectiveFunction objectiveFunction,
//            final TabuList tabuList,
//            final AspirationCriteria aspirationCriteria,
//            final boolean maximizing,
//            final boolean chooseFirstImprovingMove,
//            final TabuSearch This )
//    {
//        // Set up variables
//        GCObjectiveSurrogate objectiveSurrogate = new GCObjectiveSurrogate();
//        Search search = ((GCMoveManager) This.getMoveManager()).search;
//
//        // Split Neigh
//        int evaluateLength = (int) Math.ceil((double) allMoves.length * ((GCMoveManager) This.getMoveManager()).threshold);
//        int predictLength = allMoves.length - evaluateLength;
//
//        GCMove[] evaluateMove = new GCMove[evaluateLength];
//        GCMove[] predictMove = new GCMove[predictLength];
//
//        System.arraycopy(allMoves, 0, evaluateMove, 0, evaluateLength);
//        System.arraycopy(allMoves, evaluateLength, predictMove, 0, predictLength);
//
//        Move bestMove = evaluateMove[0];
//        double[] bestMoveVal = {};
//        boolean bestMoveTabu = false;
//
//        ArrayList<Pair<Move, double[]>> evaluatedSet = new ArrayList<>();
//        ArrayList<Pair<Move, double[]>> predictedSet = new ArrayList<>();
//
//        ArrayList<Pair<Move, double[]>> allTrueSet = new ArrayList<>();
//        ArrayList<Pair<Move, double[]>> predictTrueSet = new ArrayList<>();
//
//        // real evaluate move in moves
//        for(int i = 0; i < allMoves.length; i++ )
//        {
//            Move move = allMoves[i];
//
//            GCSolution tmp = soln.clone();
//            double[] newObjVal = objectiveFunction.evaluate( soln, move );
//            allTrueSet.add(new Pair<>(move, newObjVal));
//        }
//
//        for(int i = 0; i < evaluateMove.length; i++ )
//        {
//            evaluatedSet.add(allTrueSet.get(i));
//        }
//
//        // use these to update surrogate
//
//        long startTime = System.currentTimeMillis();
//        if (((GCMoveManager) This.getMoveManager()).mode.equals("LocalSample"))
//            search.dataset = new ArrayList<>();
//        updateSurrogate(search, evaluatedSet, soln.layout.slap);
//        System.out.println("update surrogate search: " + Search.getElapsedTime(startTime)/1000);
//
//
//        // use surrogate to predict un-evaluated move
//        startTime = System.currentTimeMillis();
//
//        for (int i = 0; i < predictMove.length; i++) {
//            Move move = predictMove[i];
//
//            double[] newObjVal = objectiveSurrogate.evaluate(soln, move);
//
//            predictedSet.add(new Pair<>(move, newObjVal));
//            predictTrueSet.add(allTrueSet.get(i+evaluateLength));
//        }
//
//        System.out.println("use surrogate predict: " + Search.getElapsedTime(startTime)/1000);
//
//        // sort predictedSet
//
//        Comparator<Pair<Move, double[]>> cmp = new Comparator<Pair<Move, double[]>>(){
//            @Override
//            public int compare(Pair<Move, double[]> p1, Pair<Move, double[]> p2){
//
////                Returns:
////                a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
//
//                double p1Val = p1.getValue()[0];
//                double p2Val = p2.getValue()[0];
//
//                boolean p1IsTabu = isTabu( soln, p1.getKey(), p1.getValue(), tabuList, aspirationCriteria, This );
//                boolean p2IsTabu = isTabu( soln, p2.getKey(), p2.getValue(), tabuList, aspirationCriteria, This );
//
//                if ( (!p1IsTabu && !p2IsTabu) || (p1IsTabu && p2IsTabu) ){
//                    return Double.compare(p1Val, p2Val);
//                }else if(!p1IsTabu){
//                    return -1;
//                }else{
//                    return 1;
//                }
//
//            }
//        };
//
////        Collections.sort(predictedSet, cmp);
//        concurrentSort(predictedSet, cmp, predictedSet, predictTrueSet);
//
//
//
//        // real evaluate % of predictedSet
//
//        int reEvaluateLength = (int) Math.ceil((double) allMoves.length *
//                ((GCMoveManager) This.getMoveManager()).evaluatePredictionThreshold);
//        for (int i = 0; i < reEvaluateLength; i++) {
//            evaluatedSet.add(predictTrueSet.get(i));
//        }
//
//        Collections.sort(evaluatedSet, cmp);
//        Collections.sort(allTrueSet, cmp);
//
//        bestMove = evaluatedSet.get(0).getKey();
//        bestMoveVal = evaluatedSet.get(0).getValue();
//
//        double[] pred_tar = new double[predictLength];
//        double[] test_tar = new double[predictLength];
//        for (int i = 0; i < predictLength; i++) {
//            pred_tar[i] = predictedSet.get(i).getValue()[0];
//            test_tar[i] = predictTrueSet.get(i).getValue()[0];
//        }
//
//        KendallsCorrelation kendallsCorrelation = new KendallsCorrelation();
//        double tau = kendallsCorrelation.correlation(pred_tar, test_tar);
//        System.out.println("RBF kendalls correlation: " + tau);
//        SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();
//        double rho = spearmansCorrelation.correlation(pred_tar, test_tar);
//        System.out.println("RBF spearmans correlation: " + rho);
//
//
//        /**
//         * 15/07/2016
//         * The evaluation used by our tabu is a non-fixed local search method, hence every evaluation is a bit different
//         * We return a copy of the solution
//         */
//        return new Object[]{ bestMove, bestMoveVal, false};
//    }   // end getBestMove



        public static <T> void concurrentSort(
            final List< T > key, Comparator<T> cmp, List< ? >... lists){
        // Create a List of indices
        List<Integer> indices = new ArrayList<Integer>();
        for(int i = 0; i < key.size(); i++)
            indices.add(i);

        // Sort the indices list based on the key
        Collections.sort(indices, new Comparator<Integer>(){
            @Override public int compare(Integer i, Integer j) {
                return cmp.compare(key.get(i), key.get(j));
            }
        });


        // Create a mapping that allows sorting of the List by N swaps.
        // Only swaps can be used since we do not know the type of the lists
        Map<Integer,Integer> swapMap = new HashMap<Integer, Integer>(indices.size());
        List<Integer> swapFrom = new ArrayList<Integer>(indices.size()),
                swapTo   = new ArrayList<Integer>(indices.size());
        for(int i = 0; i < key.size(); i++){
            int k = indices.get(i);
            while(i != k && swapMap.containsKey(k))
                k = swapMap.get(k);

            swapFrom.add(i);
            swapTo.add(k);
            swapMap.put(i, k);
        }

        // use the swap order to sort each list by swapping elements
        for(List< ? > list : lists)
            for(int i = 0; i < list.size(); i++)
                Collections.swap(list, swapFrom.get(i), swapTo.get(i));
    }
    
    static void updateSurrogate(Search search, ArrayList<Pair<Move, double[]>> evaluatedSet, SLAP slap){
        double[][] vars = new double[evaluatedSet.size()][];
        double[] objs = new double[evaluatedSet.size()];

        int idx = 0;
        for (Pair<Move, double[]> p : evaluatedSet){
            Move move = p.getKey();
            double obj = p.getValue()[0];

            vars[idx] = Surrogate.getUpperVar(((GCMove)move).layoutCode, slap);
            objs[idx++] = obj;
        }

        search.updateDataset(vars, objs);
        search.updateSurrogate();
    }
    
    /**
     * Determine if the move is tabu and consider whether or not it
     * satisfies the aspiration criteria.
     *
     * @since 1.0
     */
    protected static boolean isTabu( 
    final Solution soln, 
    final Move move, 
    final double[] val, 
    final TabuList tabuList,
    final AspirationCriteria aspirationCriteria, 
    final TabuSearch This )
    {
        boolean tabu = false;
        
        // See if move is tabu
        if( tabuList.isTabu( soln, move ) )
        {   // It is tabu.
            tabu = true;
            if( aspirationCriteria != null )
            {
                // ASPIRATION CRITERIA
                // If this is better than the best, make it NOT tabu
                if( aspirationCriteria.overrideTabu( soln, move, val, This ) )
                    tabu = false;
            }   // end aspiration
        }   // end if: move was tabu
        
        return tabu;
    }   // end isTabu
    
    
    
    /**
     * Deprecated and renamed to {@link #isFirstBetterThanSecond}
     * to be named more consistently. This method still works. It simply
     * calls the newly-named version.
     *
     * @param first The first array of <tt>double</tt>s
     * @param second The second array of <tt>double</tt>s
     * @param maximizing Whether or not the tabu search should be maximizing
     * @return <tt>true</tt> if the first array of numbers is better than the second
     * @since 1.0
     * @version 1.0a
     * @deprecated
     */
    public static boolean firstIsBetterThanSecond( 
    final double[] first, final double[] second, final boolean maximizing )
    {   return isFirstBetterThanSecond( first, second, maximizing );
    }   // end firstIsBetterThanSecond
        
        

    /**
     * This single method is called many times to compare solutions.
     * Although all data is stored as doubles, they are cast to floats
     * before they are compared. This ensures that the inevitable
     * errors associated with all floating point numbers do not affect
     * the likely intent of the numbers.
     *
     * @param first The first array of <tt>double</tt>s
     * @param second The second array of <tt>double</tt>s
     * @param maximizing Whether or not the tabu search should be maximizing
     * @return <tt>true</tt> if the first array of numbers is better than the second
     * @since 1.0
     * @version 1.0a
     */
    public static boolean isFirstBetterThanSecond( 
    final double[] first, final double[] second, final boolean maximizing )
    {
        int i=0; // Put at the beginning for possible speed boost
        final int valLength = first.length;
        float first_f, second_f;
        
        for( i = 0; i < valLength; i++ )
        {   
            first_f = (float)first[i];
            second_f = (float)second[i];
            
            if( first_f > second_f )
                return maximizing ? true : false;
            
            else if( first_f < second_f )
                return maximizing ? false : true;
                
        }   // end for: through each value
        
        // If we get this far, then they're equal.
        return false;
    }   // end firstIsBetterThanSecond
    
    
    
    /**
     * Fires events that are queued for firing at the end of an iteration.
     *
     * @since 1.0
     */
    protected void fireQueuedEvents()
    {
        if( this.fireNewCurrentSolution )
        {   
            this.fireNewCurrentSolution = false;
            fireNewCurrentSolution();
        }   // end if
        
        if( this.fireNewBestSolution )
        {   
            this.fireNewBestSolution = false;
            fireNewBestSolution();
        }   // end if
        
        if( this.fireUnimprovingMoveMade )
        {   
            this.fireUnimprovingMoveMade = false;
            fireUnimprovingMoveMade();
        }   // end if: unimproving move
        else if( this.fireImprovingMoveMade )
        {   
            this.fireImprovingMoveMade = false;
            fireImprovingMoveMade();
        }   // end else if: improving move
        else
        {
            this.fireNoChangeInValueMoveMade = false;
            fireNoChangeInValueMoveMade();
        }   // end else: no change in value
        
        // If changes were made by listeners, we may need to fire the event again.
        if( this.fireNewCurrentSolution || this.fireNewBestSolution || 
            this.fireUnimprovingMoveMade || this.fireImprovingMoveMade ||
            this.fireNoChangeInValueMoveMade )
            fireQueuedEvents();
    }   // end fireQueuedEvents
    
    
    
    
    /**
     * Set the current solution and prepare to fire an event.
     *
     * @param solution The new current solution
     * @since 1.0
     */
    protected void internalSetCurrentSolution( GCSolution solution )
    {   
        this.currentSolution = solution;
        
        this.fireNewCurrentSolution = true;
        if( getCurrentSolution() == null )
            internalSetCurrentSolution(solution.clone() );
    }   // end internalSetCurrentSolution
    
    
    
    
    /**
     * Set the best solution and prepare to fire an event.
     *
     * @param solution The new best solution
     * @since 1.0
     */
    protected void internalSetBestSolution( GCSolution solution )
    {   
        this.bestSolution = solution;
        this.fireNewBestSolution = true;
        
        //if(this.bestSolution.getObjectiveValue()[0] != this.bestSolution.layout.objective())
        //	System.err.println("Illegal objective");
        
        this.lastImprovedItrs = this.runnedItrs;
        
        System.out.println( this.getElapsedTime() + "\t"+this.currentSolution.getObjectiveValue()[0]+"\t" + this.bestSolution.getObjectiveValue()[0] +"\t" + this.runnedItrs+ "\tNew");
    }   // end internalSetBestSolution
    
    
    /**
     * Sets the status of either solving or not solving. This does
     * not start and stop the solver--it only sets the reporting flag.
     *
     * @param solving Whether or not the tabu search should be marked as solving or not.
     * @since 1.0
     */
    protected void setSolving( boolean solving )
    {   
        this.solving = solving;
    }   // end setSolving
    
    
    /**
     * Tells the tabu search internally whether or not to keep solving
     * the next chance it gets to quit, like at the start of a new iteration.
     *
     * @param keepSolving Whether or not to keep solving
     * @since 1.0
     */
    protected void setKeepSolving( boolean keepSolving )
    {   
        this.keepSolving = keepSolving;
    }   // end setKeepSolving
    
    
    
    /**
     * Returns whether or not the tabu search should keep solving
     * the next chance it gets to quit, like at the start of a new iteration.
     *
     * @return Whether or not to keep solving
     * @since 1.0
     */
    protected boolean isKeepSolving()
    {   
        return this.keepSolving;
    }   // end isKeepSolving
    
    
    
    
    /**
     * Internally set whether or not a new current solution
     * {@link TabuSearchEvent} should be fired at the end of the iteration.
     *
     * @param b Whether or not to fire a new current solution event.
     * @since 1.0
     */
    protected void setFireNewCurrentSolution( boolean b )
    {   
        this.fireNewCurrentSolution = b;
    }   // end setFireNewCurrentSolution
    
    
    /**
     * Internally set whether or not a new best solution
     * {@link TabuSearchEvent} should be fired at the end of the iteration.
     *
     * @param b Whether or not to fire a new best solution event.
     * @since 1.0
     */
    protected void setFireNewBestSolution( boolean b )
    {   
        this.fireNewBestSolution = b;
    }   // end setFireNewBestSolution
    
    
    /**
     * Internally set whether or not an unimproving move made
     * {@link TabuSearchEvent} should be fired at the end of the iteration.
     *
     * @param b Whether or not to fire an unimproving move made event.
     * @since 1.0
     */
    protected void setFireUnimprovingMoveMade( boolean b )
    {   
        this.fireUnimprovingMoveMade = b;
    }   // end setFireUnimprovingMoveMade
    
    
    /**
     * Internally set whether or not an improving move made
     * {@link TabuSearchEvent} should be fired at the end of the iteration.
     *
     * @param b Whether or not to fire an improving move made event.
     * @since 1.0-exp7
     */
    protected void setFireImprovingMoveMade( boolean b )
    {   
        this.fireImprovingMoveMade = b;
    }   // end setFireImprovingMoveMade
    
    
    /**
     * Internally set whether or not a no change in value move made
     * {@link TabuSearchEvent} should be fired at the end of the iteration.
     *
     * @param b Whether or not to fire a no change in value move made event.
     * @since 1.0-exp7
     */
    protected void setFireNoChangeInValueMoveMade( boolean b )
    {   
        this.fireNoChangeInValueMoveMade = b;
    }   // end setFireNoChangeInValueMoveMade
    
    /**
     * Returns whether or not the tabu search plans to fire a
     * new current solution {@link TabuSearchEvent} 
     * at the end of the iteration.
     *
     * @return whether or not the tabu search plans to fire a
     *         new current solution event
     * @since 1.0
     */
    protected boolean isFireNewCurrentSolution()
    {   
        return this.fireNewCurrentSolution;
    }   // end isFireNewCurrentSolution
    
    
    /**
     * Returns whether or not the tabu search plans to fire a
     * new best solution {@link TabuSearchEvent} 
     * at the end of the iteration.
     *
     * @return whether or not the tabu search plans to fire a
     *         new best solution event
     * @since 1.0
     */
    protected boolean isFireNewBestSolution()
    {   
        return this.fireNewBestSolution;
    }   // end isFireNewBestSolution
    
    
    /**
     * Returns whether or not the tabu search plans to fire an
     * unimproving move made {@link TabuSearchEvent} 
     * at the end of the iteration.
     *
     * @return whether or not the tabu search plans to fire an
     *         unimproving move made event
     * @since 1.0
     */
    protected boolean isFireUnimprovingMoveMade()
    {   
        return this.fireUnimprovingMoveMade;
    }   // end isFireUnimprovingMoveMade
    
    /**
     * Returns whether or not the tabu search plans to fire an
     * improving move made {@link TabuSearchEvent} 
     * at the end of the iteration.
     *
     * @return whether or not the tabu search plans to fire an
     *         improving move made event
     * @since 1.0-exp7
     */
    protected boolean isFireImprovingMoveMade()
    {   
        return this.fireImprovingMoveMade;
    }   // end isFireImprovingMoveMade
    
    
    /**
     * Returns whether or not the tabu search plans to fire a
     * no change in value move made {@link TabuSearchEvent} 
     * at the end of the iteration.
     *
     * @return whether or not the tabu search plans to fire a
     *         no change in value move made event
     * @since 1.0-exp7
     */
    protected boolean isFireNoChangeInValueMoveMade()
    {   
        return this.fireNoChangeInValueMoveMade;
    }   // end isFireNoChangeInValueMoveMade
    
/* ********  T A B U S E A R C H   M E T H O D S  ******** */    
    
    /**
     * Starts the tabu search solving in the current thread, blocking
     * until the <tt>iterationsToGo</tt> property is zero.
     *
     * @since 1.0c
     */
    public void startSolving()
    {   
        // v1.0c: Clear internal flag that might otherwise say "stop"
        setKeepSolving( true );
        
        setSolving( true ); // Set the solving flag.
        
        // Make sure there are iterations requested.
        this.runnedItrs = 0;
        if( iterationsToGo > 0 )
            fireTabuSearchStarted();

        long itStartTime =  System.currentTimeMillis();
        
        System.out.println( "Time(ms)\tCurrent\tBest-so-far\tItrs");
        
        // While not canceled and iterations left to go
        while( keepSolving && ( iterationsToGo > 0 ) )
        {   
        	
            Thread.yield();       
            synchronized( this )
            {
            	this.runnedItrs++;
            	
                iterationsToGo--;

                
                try
                {   performOneIteration();
                }   // end try
                catch( NoMovesGeneratedException e )
                {   if( err != null )
                        err.println( e );
                }   // end catch
                catch( NoCurrentSolutionException e )
                {   if( err != null )
                        err.println( e );
                }   // end catch
                incrementIterationsCompleted();
                
                if(this.Verbose){
                	//Set a minimum recording time 
                	if((this.runnedItrs - this.lastImprovedItrs) > this.itrsNoImprovToStop){
                    	System.out.println( this.getElapsedTime() + "\t " + ((GCSolution)this.currentSolution).getObjectiveValue()[0] + 
        						"\t" + ((GCSolution)this.bestSolution).getObjectiveValue()[0] + "\t" + this.runnedItrs);
                    	System.out.println("Did not improve in " + this.itrsNoImprovToStop + " iterations, finish...");
                    	
                    	this.setKeepSolving(false);
                    }
                	else if((System.currentTimeMillis() - itStartTime) > this.recordIntervals * 1000){
                		if(this.runnedItrs != this.lastImprovedItrs){
                			//if last improve euqals to runneditrs, this line has already been printed.
                			
                			System.out.println( this.getElapsedTime() + "\t " + ((GCSolution)this.currentSolution).getObjectiveValue()[0] + 
            						"\t" + ((GCSolution)this.bestSolution).getObjectiveValue()[0] + "\t" + this.runnedItrs);
                		}
                    	itStartTime = System.currentTimeMillis();
                	}
                }
            }   // end sync: this

            System.out.flush();
//            if( this.getElapsedTime() > this.timeLimit*1000){
//            	System.out.println("Reach time Limit: " + this.timeLimit);
//            	stopSolving();
//            }

            if( moveManager.search.evalTime >= this.budgetLimit){
                System.out.println("Reach budget limit: " + this.timeLimit);
                stopSolving();
            }
            
        }   // end while: iters left
        
        setSolving( false );
        fireTabuSearchStopped();
        
    }   // end startSolving
    
    
    /**
     * Stops the tabu search and preserves the number of
     * iterations remaining.
     *
     * @since 1.0
     */
    public synchronized void stopSolving()
    {   setKeepSolving( false );
    }   // end stopSolving
    
    
    /**
     * Returns whether or not the tabu search is currently solving.
     *
     * @since 1.0
     */
    public synchronized boolean isSolving()
    {   return solving;
    }   // end isSolving
    
    
    
    /**
     * Sets the objective function, effective at the start of the next iteration,
     * and re-evaluates the current and best solution values.
     *
     * @param function The new objective function.
     * @see ObjectiveFunction
     * @since 1.0
     */
    public synchronized void setObjectiveFunction( ObjectiveFunction function )
    {   this.objectiveFunction = (GCObjectiveFunction) function;
        
        if( this.currentSolution != null )
            this.currentSolution.setObjectiveValue( function.evaluate( this.currentSolution, null ) );
    
        if( this.bestSolution != null )
            this.bestSolution.setObjectiveValue( function.evaluate( this.bestSolution, null ) );
    
    }   // end setObjectiveFunction
    
    
    /**
     * Sets the move manager, effective at the start of the next iteration.
     *
     * @param moveManager The new move manager.
     * @see MoveManager
     * @since 1.0
     */
    public synchronized void setMoveManager( MoveManager moveManager )
    {   this.moveManager = (GCMoveManager)moveManager;
    }   // end setMoveManager
    
    
    
    /**
     * Sets the tabu list, effective at the start of the next iteration.
     *
     * @param tabuList The new tabu list.
     * @see TabuList
     * @since 1.0
     */
    public synchronized void setTabuList( TabuList tabuList )
    {   this.tabuList = tabuList;
    }   // end setTabuList
    
    
    
    /**
     * Sets the aspiration criteria, effective at the start of the next iteration.
     * A <tt>null</tt> value means there is no aspiration criteria.
     *
     * @param aspirationCriteria The new aspiration criteria
     * @see AspirationCriteria
     * @since 1.0
     */
    public synchronized void setAspirationCriteria( AspirationCriteria aspirationCriteria )
    {   this.aspirationCriteria = aspirationCriteria;
    }   // end setAspirationCriteria
    
    
    
    /**
     * Sets the best solution, effective at the start of the next iteration,
     * and fires an event to registered {@link TabuSearchListener}s.
     *
     * @param solution The new best solution.
     * @see Solution
     * @since 1.0
     */
    public synchronized void setBestSolution( Solution solution )
    {   
        internalSetBestSolution( (GCSolution)solution );
    }   // end setBestSolution
    
    
    
    /**
     * Sets the current solution, effective at the start of the next iteration,
     * and fires an event to registered {@link TabuSearchListener}s.
     *
     * @param solution The new current solution.
     * @see Solution
     * @since 1.0
     */
    public synchronized void setCurrentSolution( Solution solution )
    {   
        internalSetCurrentSolution( (GCSolution)solution );
    }   // end setCurrentSolution
    
    
    
    
    /**
     * Sets the number of iterations that the tabu search
     * has left to go. If the tabu search was previously idle,
     * that is <tt>iterationsToGo</tt> was less than or equal
     * to zero, the tabu search will not automatically begin again.
     * In this case the tabu search will not begin again until
     * {@link #startSolving} is called.
     *
     * @param iterations The number of iterations left for the tabu earch to execute.
     * @see #startSolving
     * @since 1.0
     */
    public synchronized void setIterationsToGo( int iterations )
    {   this.iterationsToGo = iterations;
    }   // end setIterationsToGo
    
    
    
    
    /**
     * Sets whether the tabu search should be maximizing or minimizing
     * the objective function.
     * A value of <tt>true</tt> means <em>maximize</em>.
     * A value of <tt>false</tt> means <em>minimize</em>.
     *
     * @param maximizing Whether or not the tabu search should be maximizing the objective function.
     * @since 1.0
     */
    public synchronized void setMaximizing( boolean maximizing )
    {   this.maximizing = maximizing;
    }   // end setMaximizing
    
    
    /**
     * Setting this to <tt>true</tt> will cause the search to go faster
     * by not necessarily evaluating all of the moves in a neighborhood
     * for each iteration. Instead of evaluating all of the moves and
     * selecting the best one for execution, setting this will cause
     * the tabu search engine to select the first move that it encounters
     * that causes an improvement to the current solution.
     * The default value is <tt>false</tt>.
     *
     * @param choose Whether or not the first improving move will be chosen
     * @since 1.0.1
     */
    public synchronized void setChooseFirstImprovingMove( boolean choose )
    {   this.chooseFirstImprovingMove = choose;
    }   // end setChooseFirstImprovingMove
    
    
    /**
     * Returns the objective function being used by the tabu search.
     *
     * @return The objective function being used by the tabu search.
     * @see ObjectiveFunction
     * @since 1.0
     */
    public synchronized GCObjectiveFunction getObjectiveFunction()
    {   return objectiveFunction;
    }   // end getObjectiveFunction
    
    
    
    
    
    /**
     * Returns the move manager being used by the tabu search.
     *
     * @return The move manager being used by the tabu search.
     * @see MoveManager
     * @since 1.0
     */
    public synchronized GCMoveManager getMoveManager()
    {   return moveManager;
    }   // end getMoveManager
    
    
    
    
    
    /**
     * Returns the tabu list being used by the tabu search.
     *
     * @return The tabu list being used by the tabu search.
     * @see TabuList
     * @since 1.0
     */
    public synchronized TabuList getTabuList()
    {   return tabuList;
    }   // end getTabuList
    
    
    
    
    /**
     * Returns the aspiration criteria.
     * A <tt>null</tt> value means there is no aspiration criteria.
     *
     * @return The aspiration criteria
     * @see AspirationCriteria
     * @since 1.0
     */
    public synchronized AspirationCriteria getAspirationCriteria()
    {   return aspirationCriteria;
    }   // end getAspirationCriteria
    
    
    
    
    /**
     * Returns the best solution found by the tabu search.
     *
     * @return The best solution found by the tabu search.
     * @see Solution
     * @since 1.0
     */
    public synchronized GCSolution getBestSolution()
    {   return bestSolution;
    }   // end getBestSolution
    
    
    
    
    /**
     * Returns the current solution being used by the tabu search.
     *
     * @return The current solution being used by the tabu search.
     * @see Solution
     * @since 1.0
     */
    public synchronized GCSolution getCurrentSolution()
    {   return currentSolution;
    }   // end getCurrentSolution
    
    
    
    
    /**
     * Returns the number of iterations left for the tabu search to execute.
     *
     * @return The number of iterations left for the tabu search to execute.
     * @since 1.0
     */
    public synchronized int getIterationsToGo()
    {   return iterationsToGo;
    }   // end getIterationsToGo
    
    
    
    
    /**
     * Returns whether or not the tabu search should be maximizing the objective function.
     *
     * @return Whether or not the tabu search should be maximizing the objective function.
     * @since 1.0
     */
    public synchronized boolean isMaximizing()
    {   return maximizing;
    }   // end isMaximizing
    
    
    /**
     * Returns whether or not the tabu search engine will choose the
     * first improving move it encounters at each iteration (<tt>true</tt>)
     * or the best move (<tt>false</tt>).
     *
     * @since 1.0.1
     */
    public synchronized boolean isChooseFirstImprovingMove()
    {   return chooseFirstImprovingMove;
    }   // end isChooseFirstImprovingMove
    
    
    
}   // end class SingleThreadedTabuSearch
