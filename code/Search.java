/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */
package TCYB;

import javafx.util.Pair;
import smile.interpolation.RBFInterpolation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;


public class Search {

	public int evalTime = 0;
	public double bestSoFar = Double.MAX_VALUE;
	public long searchStartTime = System.currentTimeMillis();

	int mode;
	
	/**
	 * we have 4 different modes
	 * 
	 * 1. splitRandom 85%, all the others 5%
	 * 2. All 25%
	 * 3. splitRandom 100%
	 * 4. splitRandomFix 100%
	 * 5. spliMaxDiff
	 * 6. splitMinCost
	 * @param mode
	 */
	double record_intervals = 1;
	Random random;

	public RBFInterpolation surrogate;
	public ArrayList<Pair<double[], Double>> dataset = new ArrayList<>();
	MinMaxScaler minMaxScaler;
	public ArrayList<Pair<Layout, Double>> visitedSet = new ArrayList<>();
	
	public Search(int mode, Random random){
		this.mode = mode;
		this.random = random;
	}

	private boolean isDatasetContain(Pair<double[], Double> p){

		for(Pair<double[], Double> o : dataset){
			if(java.util.Arrays.equals(p.getKey(), o.getKey())){
				return true;
			}
		}

		return false;
	}

	public void updateDataset(double[][] upperVar, double[] obj) {
		for (int i = 0; i < obj.length; i++) {
			Pair<double[], Double> p = new Pair<>(upperVar[i], obj[i]);
			if (!isDatasetContain(p))
				dataset.add(p);
		}
		Collections.sort(dataset, new Comparator<Pair<double[], Double>>(){
			@Override
			public int compare(Pair<double[], Double> p1, Pair<double[], Double> p2){
				double obj1 = p1.getValue();
				double obj2 = p2.getValue();
				return Double.compare(obj1, obj2);
			}
		});
	}

	public void updateSurrogate(){
		double [][] train_var = new double[dataset.size()][];
		double []   train_tar = new double[dataset.size()];

		int idx = 0;
		for(Pair<double[], Double> p : dataset){
			train_var[idx] = p.getKey();
			train_tar[idx++] = p.getValue();
		}

		surrogate = Surrogate.getSurrogateModel(train_var, train_tar);
		minMaxScaler = new MinMaxScaler(train_tar, 1.0, 0.0);
	}

	public void resetVisitedSet(){
		;
	}


	public void setRecordIntervals(int sec){
		this.record_intervals = sec;
	}
	
	public ArrayList<Integer> splitRange(ArrayList<Item> product, int nL){
		ArrayList<Integer> res = new ArrayList<Integer>();
		res.add(Math.max(1, product.size() - nL));
		res.add(Math.min(nL+1, product.size()));
		
		return res;
	}
	
	/**
	 * Split product into two groups at point where frequency decreases most
	 * @param product
	 * @param nL
	 * @return two splits stored in ArrayList
	 */
	public ArrayList<ArrayList<Item>> splitMaxDiff(ArrayList<Item> product, int nL){
		ArrayList<ArrayList<Item>> res = new ArrayList<ArrayList<Item>>();
		if(product.size() <= 1){
			res.add(product);
		}		
		else if(product.size() == 2){
			res.add(convert(product.subList(0, 1)));
			res.add(convert(product.subList(1, 2)));
		}
		else{
			ArrayList<Integer> bound = this.splitRange(product, nL);
			int low = bound.get(0);
			int high = bound.get(1);
			
			//System.out.println("SplitMaxDiff: [" + low + "\t" + high+"] in size: " + product.size());
			
			int diff = product.get(0).freq - product.get(1).freq;
			int diffI = low;
			
			for(int i = low; i < high; i++){
				if((product.get(i-1).freq - product.get(i).freq) > diff){
					diff = product.get(i-1).freq - product.get(i).freq;
					diffI = i;
				}
			}
			
			//System.out.println("DiffI: " + diffI);
			res.add(convert(product.subList(0, diffI)));
			res.add(convert(product.subList(diffI, product.size())));
		}
		return res;
	}

	public static ArrayList convert(List grps){
		ArrayList cgrps = new ArrayList();
		for(Object i: grps){
			cgrps.add(i);
		}
		return cgrps;
	}

	public ArrayList<ArrayList<Item>> splitMinCost(ArrayList<Item> product, int nL){
		ArrayList<ArrayList<Item>> res = new ArrayList<ArrayList<Item>>();
		if(product.size() <= 1){
			res.add(product);
		}
		else if(product.size() == 2){
			res.add(convert(product.subList(0, 1)));
			res.add(convert(product.subList(1, 2)));
		}
		else{
			ArrayList<Integer> bound = this.splitRange(product, nL);
			int low = bound.get(0);
			int high = bound.get(1);
			
			//System.out.println("splitMinCost: [" + low + "\t" + high+"] in size: " + product.size());
			
			int value = -1;
			int point = low;
			for(int i = low; i < high; i++){
				ArrayList<List<Item>> grps = new ArrayList<List<Item>>();
				grps.add(product.subList(0, i));
				grps.add(product.subList(i, product.size()));
						
				int tValue = 0;
				for(int j = 0; j < grps.size(); j++){
					for(int k = 0; k < grps.get(j).size(); k++){
						tValue += grps.get(j).get(k).freq * (j+k+1);
					}
				}
				
				if(value == -1 || value < tValue){
					point = i;
					value = tValue;
				}
			}
			
			res.add(convert(product.subList(0, point)));
			res.add(convert(product.subList(point, product.size())));
		}
		
		return res;
	}
	
	public ArrayList<ArrayList<Item>> splitRandom(ArrayList<Item> product, int nL){
		ArrayList<ArrayList<Item>> res = new ArrayList<ArrayList<Item>>();
		if(product.size() <= 1){
			res.add(product);
		}
		else if(product.size() == 2){
			res.add(convert(product.subList(0, 1)));
			res.add(convert(product.subList(1, 2)));
		}
		else{
			ArrayList<Integer> bound = this.splitRange(product, nL);
			int low = bound.get(0);
			int high = bound.get(1);
			
			//System.out.println("splitRandom: [" + low + "\t" + high+"]");
			
			
			int point = this.random.nextInt(high - low);
			point +=low;
			
			res.add(convert(product.subList(0, point)));
			res.add(convert(product.subList(point, product.size())));
		}
		
		return res;
	}
	

	public ArrayList<ArrayList<Item>> splitSingles(ArrayList<Item> product, int nL){
		ArrayList<ArrayList<Item>> res = new ArrayList<ArrayList<Item>>();
		
		for(Item i: product){
			ArrayList<Item> grp = new ArrayList<Item>();
			grp.add(i);
			res.add(grp);
		}
		return res;
	}
	
	/**
	 * 
	 * @param product
	 * @param nL
	 * @param prefSize, default size is 0.5
	 * @return
	 */
	public ArrayList<ArrayList<Item>> splitFixed(ArrayList<Item> product, int nL, double prefSize){
		ArrayList<ArrayList<Item>> res = new ArrayList<ArrayList<Item>>();
		if(product.size() <= 1){
			res.add(product);
		}
		else if(product.size() == 2){
			res.add(convert(product.subList(0, 1)));
			res.add(convert(product.subList(1, 2)));
		}
		else{
			ArrayList<Integer> bound = this.splitRange(product, nL);
			int low = bound.get(0);
			int high = bound.get(1);
			
			//System.out.println("splitFixed: [" + low + "\t" + high+"]");
			
			int i = 0;
			if(prefSize <= 1.0 && prefSize >= 0){
				i = (int)(prefSize*(high+1-low)+low);
			}
			else{
				i= (int)prefSize;
			}
			
			i = Math.max(low, Math.min(high, i));
			
			res.add(convert(product.subList(0, i)));
			res.add(convert(product.subList(i, product.size())));
			
		}
		return res;
	}
	
	
	public ArrayList<ArrayList<Item>> splitRandomFixed(ArrayList<Item> product, int nL){
		Random r = new Random();
		return splitFixed(product, nL, 0.5);
	}

	/**
	 * Create layout based on arbitrary way of splitting products
	 * @param slap
	 * @param splitMethod
	 * @param randomOrder
	 * @return
	 */
	
	private void printGroups(ArrayList<ArrayList<Item>> groups){
		for(ArrayList<Item> grp: groups){
			System.out.println(grp);
		}
		System.out.println();
	}

	
 	public Layout creatLayout(SLAP slap, String splitMethod, boolean randomOrder){
		int ntries = 10;
		
		for(int atmps = 0; atmps < ntries; atmps++){
			Layout layout = new Layout(slap, "Init", this.random);
			ArrayList<ArrayList<Item>> groups = new ArrayList<ArrayList<Item>>();
			
			for(ArrayList<Item> p : slap.product.values()){
				//int parameter
				Class[] param = new Class[2];	
				param[1] = Integer.TYPE;
				param[0] = ArrayList.class;
				
				try{
					Method method = getClass().getDeclaredMethod(splitMethod, param);
				    Object objGrps =  method.invoke(this,p, slap.nL);
				    
				    ArrayList<ArrayList<Item>> grps = (ArrayList<ArrayList<Item>>)objGrps;
					for(ArrayList<Item> grp: grps){
						groups.add(grp);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}
			
			if(randomOrder){
	            // sort with perturbation by length & total freq
				
				ArrayList<sum_len> slGrps = new ArrayList<sum_len>();
				for(ArrayList<Item> grp: groups){
					slGrps.add(new sum_len(grp, -sum(grp)*(this.random.nextDouble()*1.5 + 0.5), -grp.size()*(this.random.nextInt(2)-1)));
				}
				
				
				Collections.sort(slGrps, new Comparator<sum_len>(){
											@Override
											public int compare(sum_len l1, sum_len l2){
												return Double.compare(l1.sum, l2.sum);
											}
										});				
				Collections.sort(slGrps, new Comparator<sum_len>() {
											@Override
											public int compare(sum_len l1, sum_len l2) {
										        return Integer.compare(l1.len, l2.len);  
											}
										});
				//update groups based on slGrps
				groups = new ArrayList<ArrayList<Item>>();
				for(sum_len s: slGrps){
					groups.add(s.grp);
				}
			}
			else{
				
				Collections.sort(groups, new Comparator<ArrayList<Item>>(){
											@Override
											public int compare(ArrayList<Item> l1, ArrayList<Item> l2){
												return Integer.compare(-sum(l1), -sum(l2));
											}
										});
				
				//	            groups.sort(key = lambda x: (-len(x), -sum(xx.freq for xx in x))) # sort by length then frequency						
				Collections.sort(groups, new Comparator<ArrayList<Item>>() {
												@Override
												public int compare(ArrayList<Item> l1, ArrayList<Item> l2) {
											        return Integer.compare(  -l1.size(), -l2.size() );  
												}
											});
			}
			
			//printGroups(groups);
			
			for(ArrayList<Item> grp: groups){
				ArrayList<Integer> rowLength = new ArrayList<Integer>();
				for(int r: slap.S){
					 rowLength.add( Layout.countARow(layout.row.get(r)));
				}
				
				if(slap.nL -  Collections.min(rowLength)  < grp.size()){
	                layout = null;
	                break; // give up
				}
				else
				{
					 // look for closest location where item will fit
		                int cnt = Integer.MAX_VALUE;
		                int ridx = 0;
		                for(int r: slap.S){
		                	if(rowLength.get(r) + grp.size() <= slap.nL){
		                		if(rowLength.get(r) + r < cnt){
		                			ridx = r;
		                			cnt = rowLength.get(r) + r;
		                		}
		                	}
		                }
		                layout.row.get(ridx).add(grp);
				}
				
	           
			}
			 if (layout != null) {
				 if(!layout.isFeasible(false)){
					 System.err.println("Get an infeasible solution when initialzing the solution");
				 }
				 
				 return layout;
			 }
		}
		
		//System.out.println(splitMethod);
	    System.out.println( "ERROR: cannot find feasible solution in "+ ntries +" attempts");
	    return null;
	}
	
	class sum_len{
		ArrayList<Item> grp;
		double sum;
		int len;
		
		public sum_len(ArrayList<Item> grp, double sum, int len){
			this.grp = grp;
			this.sum = sum;
			this.len = len;
		}
	}
	
	public int sum(ArrayList<Item> grp){
		int sum = 0;
		for(Item i: grp){
			sum += i.freq;
		}
		return sum;
	}
	
	public Layout init(SLAP slap){
		String sMeth = pickSplitMth();
		Layout	soln = this.creatLayout(slap, sMeth, false);	
		return soln;
	}
	
	
	//add a function that can specify the init method
	
	public Layout init(SLAP slap, String meth){
		Layout	soln = this.creatLayout(slap, meth, false);	
		return soln;
	}
	
	private String pickSplitMth(){
		String[] splits = new String[]{"splitRandomFixed", "splitMaxDiff", "splitMinCost"  , "splitRandom"};
		
		/**
		 * we have 4 different modes
		 * 
		 * 1. splitRandom 85%, all the others 5%
		 * 2. All 25%
		 * 3. splitRandom 100%
		 * 4. splitRandomFix 100%
		 * @param mode
		 */
		Double[] splitProb = new Double[]{0.05,0.05,0.05,1.0};;
		
		if(this.mode == 2){
			splitProb = new Double[]{0.25,0.25,0.25,1.0};
		}
		else if(this.mode == 3){
			splitProb = new Double[]{0.0,0.0,0.0,1.0};
		}else if(this.mode == 4){
			splitProb = new Double[]{0.0,1.0,0.0,1.0};
		}else if(this.mode == 5){
			splitProb = new Double[]{0.0,0.0,1.0,1.0};
		}else if(this.mode == 6){
			splitProb = new Double[]{1.0,0.0,0.0,1.0};
		}

	
		double prob = this.random.nextDouble();
		String sMeth = "";
		for(int i = 0; i < splits.length; i++){
			sMeth = splits[i];
			prob -= splitProb[i];
			if(prob <= 0) break;
		}
		//System.out.println("Using: " + sMeth);
		return sMeth;
	}
	
	/**
	 * This method is used to do local search on a solution given
	 * @param layout
	 * @param it
	 */
	public void LNS(Layout soln, int it){
		ArrayList<String> methods = new ArrayList<String>();
		methods.add("itemSort");
		methods.add("allRowSort");
		methods.add("rowSort");
		methods.add("groupSort");

		soln.obj = 0;
		int prevObj = Integer.MAX_VALUE;
		int loop = 0;
		while((soln.objective() < prevObj) && (loop < it)){
			prevObj = soln.objective();

			Collections.shuffle(methods, this.random);
			for(String m : methods){
				call(m,soln);
			}
			loop++;
		}

		if(!soln.isFeasible(false)){
			System.err.println("Get infeasible solution after fo sort");
			soln.isFeasible(true);
		}
		else if (soln.objective() < bestSoFar){
			bestSoFar = soln.objective();
		}

		if (evalTime % 100 == 0) {
			System.out.println("statTitle: nbEvaluations Best-So-Far timeCost");
			System.out.println("stat " + evalTime + " " + bestSoFar + " " + getElapsedTime(searchStartTime)/1000 + " ");
			System.out.flush();
		}

		evalTime++;
	}

	public void LNS_pred(Layout soln, int it){
		ArrayList<ArrayList<Layout.GROUP>> layoutCode = soln.encode(soln.row);
		double[] var = Surrogate.getUpperVar(layoutCode, soln.slap);
		double pred = surrogate.interpolate(var);
		double revTransformed = minMaxScaler.rev_transform(pred);
		soln.predObj = revTransformed;
		soln.predicted = true;
	}
	
	
	public Layout proportionalLNS(SLAP slap, double time){
		long startTime = System.currentTimeMillis();
		
		Layout soln  = null;
		
		while(soln == null){
			soln = init(slap);
		}
		
		ArrayList<String> methods = new ArrayList<String>();
		methods.add("itemSort");
		methods.add("allRowSort");
		methods.add("rowSort");
		methods.add("groupSort");
		
		//do LNS until no improvement
		int prevObj = Integer.MAX_VALUE;
		Layout bestSoln = null;
		int bestObj = Integer.MAX_VALUE;
		
		while(true){
			
			if(this.getElapsedTime(startTime) > time){
				break;
			}
			while(soln.objective() < prevObj){
				prevObj = soln.objective();
				
				if(bestObj > soln.objective()){
					bestObj = soln.objective();
					bestSoln = (Layout)soln.clone();
					System.out.println("Run "+this.getElapsedTime(startTime)+", found " + bestObj);
				}
				 
				 Collections.shuffle(methods);
				 for(String m : methods){
					 call(m,soln);
				 }   
			}

			//change some of the split in soln
			soln=proportionalInit(slap, soln, 0.2);
			prevObj = Integer.MAX_VALUE;
		}
		
		return bestSoln;
	}
	
	private ArrayList<ArrayList<Item>> split(String splitMethod, ArrayList<Item> p, SLAP slap){
		Class[] param = new Class[2];	
		param[1] = Integer.TYPE;
		param[0] = ArrayList.class;
		
		try{
			Method method = getClass().getDeclaredMethod(splitMethod, param);
		    Object objGrps =  method.invoke(this, p, slap.nL);
		    
		    ArrayList<ArrayList<Item>> grps = (ArrayList<ArrayList<Item>>)objGrps;
		    return grps;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	private Layout proportionalInit(SLAP slap, Layout soln, double threshold){
		int ntries = 10;
		//get a list of product name that need to re-split
		Random ifNewSplit = new Random();
		
		for(int atmps = 0; atmps < ntries; atmps++){
			
			Layout layout = new Layout(slap, "init", this.random);
			ArrayList<ArrayList<Item>> groups = new ArrayList<ArrayList<Item>>();
			
			for(ArrayList<Item> Items: slap.product.values()){
				String name = Items.get(0).product;
				double prob = ifNewSplit.nextDouble();
				
				if(Items.size() == 1){
					groups.add(Items);
				}
				else{
					//lower the possibility when the size of the product is bigger so that it get more chance to be resized, for those with size one, no need to resplit.

					if(prob >= threshold){
						//this product should be resplit
						ArrayList<Item> p = slap.product.get(name);
						
						//System.out.println("Changing : " + name);
						String sMeth = pickSplitMth();
						ArrayList<ArrayList<Item>> splitted = split(sMeth, p, slap);
						
						for(ArrayList<Item> grp: splitted){
							groups.add(grp);
						}
					}
					else{
						//find the split in the soln and add to groups.
						ArrayList<ArrayList<Item>> splitted = soln.getGroupsByName(name);
						for(ArrayList<Item> grp: splitted){
							groups.add(grp);
						}
					}
				}
			}

//			soln.printLayout();
//			System.out.println("####################");
//			printGroups(groups);
//			System.exit(0);
			//sort
			Collections.sort(groups, new Comparator<ArrayList<Item>>(){
				@Override
				public int compare(ArrayList<Item> l1, ArrayList<Item> l2){
					return Integer.compare(-sum(l1), -sum(l2));
				}
			});
			Collections.sort(groups, new Comparator<ArrayList<Item>>() {
				@Override
				public int compare(ArrayList<Item> l1, ArrayList<Item> l2) {
			        return Integer.compare(  -l1.size(), -l2.size() );  
				}
			});
			
			
			for(ArrayList<Item> grp: groups){
				ArrayList<Integer> rowLength = new ArrayList<Integer>();
				for(int r: slap.S){
					 rowLength.add( Layout.countARow(layout.row.get(r)));
				}
				
				if(slap.nL -  Collections.min(rowLength)  < grp.size()){
	                layout = null;
	                break; // give up
				}
				else
				{
					 // look for closest location where item will fit
		                int cnt = Integer.MAX_VALUE;
		                int ridx = 0;
		                for(int r: slap.S){
		                	if(rowLength.get(r) + grp.size() <= slap.nL){
		                		if(rowLength.get(r) + r < cnt){
		                			ridx = r;
		                			cnt = rowLength.get(r) + r;
		                		}
		                	}
		                }
		                layout.row.get(ridx).add(grp);
				}
				
	           
			}
			 if (layout != null) {
				 if(!layout.isFeasible(false)){
					 System.err.println("Get an infeasible solution when initialzing the solution");
				 }
				 
				 return layout;
			 }
		}
		
		return null;
	}
	
	//cpu is in second
	public Layout multiStartRandomSearch(SLAP slap, int lb, long cpu, boolean ifstopnoimprove, int[] itrs, int itrToStop){
		
		this.record_intervals = ((double) cpu/1000 > 5? 5 : (double) cpu/100)*1000;
		
		
		long startTime = System.currentTimeMillis();
		
		Layout best = null;
		int it = 0;
		int lastimproveIteration = 0;
		
		long last = System.currentTimeMillis();
		
		while(best == null || (best.objective() > lb)){		
			
			if(this.getElapsedTime(startTime) >= cpu*1000){
				System.out.println("Reach time limit " + cpu + " s");
				itrs[0] = it;
				return best;
			}
			
			Layout soln = init(slap);

			if(soln == null){
				continue;
			}
			
			it++;
			
			if(ifstopnoimprove && ((it - lastimproveIteration )> itrToStop)){
				System.out.println(  this.getElapsedTime( startTime) + "\t"+ soln.objective()+"\t" + best.objective() + "\t" + it);
				System.out.println("Did not improve in " + itrToStop+ " iterations, finish...");
				itrs[0] = it;
				return best;
			}
			
			LNS(soln, 256);
			
			if(best == null || soln.objective() < best.objective()){
				best = soln;
				lastimproveIteration = it;
				
				System.out.println( this.getElapsedTime( startTime) + "\t"+soln.objective()+"\t" + best.objective() + "\t" + it + "\tNew");
				 if( best.objective() < lb+0.5){
					 System.out.println("Get OPTIMAL: " + best.objective());
					 break;
				 }
			}
			else{
				//By default, output every 1 sec, or can set during the function
				if(this.getElapsedTime(last) >= this.record_intervals){
					System.out.println(  this.getElapsedTime( startTime) + "\t"+ soln.objective()+"\t" + best.objective() + "\t" + it);
					last = System.currentTimeMillis();
				}
			}
			
			itrs[0] = it;
		}
		return best;
	}
	
	public static double getElapsedTime(long startTime){
		return (System.currentTimeMillis() - startTime);
	}
	
	public void call(String method, Layout soln){
		if(method.equals("itemSort")){
			soln.itemSort();
		}
		else if(method.equals("allRowSort")){
			soln.allRowSort();
		}
		else if(method.equals("rowSort")){
			soln.rowSort(null);
		}
		else if(method.equals("groupSort")){
			//	System.out.println("Calling");
			soln.groupSort(-1);
		}
	}

	public static double runbyfile(String argv[]){
		long seed = System.currentTimeMillis();
		//long seed = Long.parseLong("1468571052499");
		Random random = new Random(seed);
		System.out.println("random seed:" + seed);
		
		String filename = argv[0];
		int m = Integer.parseInt(argv[1]);
		int timeLimit = Integer.parseInt(argv[2]);
		
		SLAP slap = new SLAP(filename, m);
		Search search = new Search(3, random); //simply use SplitRandom in the init method
		//search.setRecordIntervals(10);
		
		Layout lbLayout = search.creatLayout(slap, "splitSingles", true);
		lbLayout.groupSort(1);
		System.out.println("The lb for the problem is: " + lbLayout.objective());
		
		int cpuTime = timeLimit;
		System.out.println("Set time limit: " + cpuTime + " s");
			
		System.out.println("Solving problem " + filename + " with " + slap.nS + " rows " + slap.nL + " locations");
		long startTime = System.currentTimeMillis();
		
		System.out.println( "Time(ms)\tCurrent\tBest\tit");
		
		int[] totalItrs = new int[1];
		
		//20000 is the number of itrs to stop when there is no improvement
		Layout soln= search.multiStartRandomSearch(slap, lbLayout.objective(), cpuTime, true, totalItrs, 20000);
		double time = getElapsedTime(startTime) ;
		System.out.println("Found best: \n" + time + "\t-\t" + soln.objective() + "\t" + totalItrs[0]);
			
		return soln.objective();
		
	}
	
	public static void main(String argv[]) throws FileNotFoundException {
		for (int i = 1; i <= 2; i++) {
			for (int j = 0; j < 10; j++) {
				PrintStream print=new PrintStream("5040sample-MSRS-"+i+"-expr-"+j);  //写好输出位置文件；
				System.setOut(print);
				String[] config = {"5040-"+i,"36","5000"};
				runbyfile(config);
			}
		}

	}

	
}
