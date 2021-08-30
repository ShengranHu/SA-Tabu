/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */

package TCYB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Layout {
	public final SLAP slap;
	public ArrayList<ArrayList<ArrayList<Item>>> row; //each product is an array list, each row is a list of product, the warehouse has list of lists
	public int tardiness;
	public int obj;
	public double predObj;
	public boolean predicted;
	public String iniMth;
	public Random random;
	
	public Object clone(){
		Layout copy = new Layout(slap, "Clone", this.random);
		copy.row = this.deepcopy3(this.row);
		copy.obj = this.obj;
		return copy;
	}
	
	public Layout(SLAP slap, String iniMth, Random random){
		this.slap = slap;
		this.row = new ArrayList<ArrayList<ArrayList<Item>>>(); //for each shelf have list of lists of items
		for(int i: slap.S){
			this.row.add(new ArrayList<ArrayList<Item>>()); //add rows
		}
		this.tardiness = 100;
		this.iniMth = iniMth;
		this.random = random;
		predicted = false;
	}
	
	public void _setRows_(ArrayList<ArrayList<ArrayList<Item>>> row){
		this.row = row;
	}
	
	public ArrayList<ArrayList<Item>> getGroupsByName(String name){
		ArrayList<ArrayList<Item>> groups = new ArrayList<ArrayList<Item>>();
		
		for(ArrayList<ArrayList<Item>> shelf: row){
			for(ArrayList<Item> grp: shelf){
				if(grp.get(0).product.equals(name)){
					groups.add(grp);
				}
			}
		}
		return groups;
	}
	
	/**
	 * @return objective of current layout
	 */
	public int objective(){
		int obj = 0;
		for(int h = 0; h < this.row.size(); h++){
			ArrayList<ArrayList<Item>> curRow = this.row.get(h);
			int cnt = 0;
			for(ArrayList<Item> grp: curRow){
				for(Item itm: grp){
					cnt += 1;
					obj += itm.freq * ( h + cnt + (cnt > this.slap.nL?this.tardiness:0));
				}
			}
		}
		this.obj = obj;
		assert !predicted;
		return obj;
	}
	
	/**
	 * 
	 * @return list of shelf/row numbers that are not correct length
	 */
	public ArrayList<Integer> infoRows(boolean verbose){
		
		ArrayList<Integer> rows = new ArrayList<Integer>();
		
		for(int i = 0; i < this.row.size(); i++){
			ArrayList<ArrayList<Item>> curRow = this.row.get(i);
			//sum number of items in row
			if(countARow(curRow) != this.slap.nL){
				System.out.println(countARow(curRow));
				rows.add(i);
			}
		}
		
		if(verbose && (rows.size() > 0)){
			String debug = "Get shelves with incorrect number of items: ";
			for(int i : rows){
				debug +=  i + "\t";
			}
			System.out.println(debug);
		}
		return rows;
	}
	
	/**
	 * @param count, default value is 2
	 * @param rows, default value is null
	 * @param verbose, default value is false
	 * @return List of infeasible product names (split in more than 2 groups)
	 */
	
	public Hashtable dic(Hashtable table, int value){
		Hashtable cnt = new Hashtable();
		Iterator keys = (Iterator)table.keys();
		while(keys.hasNext()){
			cnt.put(keys.next(), value);
		}
		return cnt;
	}
	
 	public ArrayList<String> infGroups(int count, ArrayList<ArrayList<ArrayList<Item>>> rowsTBP, boolean verbose){
		Hashtable<String, Integer> cnt = (Hashtable<String, Integer>) dic(this.slap.product, 0);
		
		ArrayList<ArrayList<ArrayList<Item>>> rows;
		if(rowsTBP != null){
			rows = rowsTBP;
		}
		else{
			rows = this.row;
		}
		
		for(ArrayList<ArrayList<Item>> irow: rows){
			for(ArrayList<Item> grp: irow){
				cnt.put(grp.get(0).product, cnt.get(grp.get(0).product) +1);
			}
		}
		
		//[ prod for prod,c in cnt.items() if c > count]
		
		ArrayList<String> InvalidGrps = new ArrayList<String>();
		Iterator<String> keys = (Iterator<String>)cnt.keys();
		while(keys.hasNext()){
			String nextKey = keys.next();
			if(cnt.get(nextKey) > count){
				InvalidGrps.add(nextKey);
			}
		}
		
		if(verbose && InvalidGrps.size() > 0){
			String debug = "Get products split into more than " + count + " groups: ";
			for(String i : InvalidGrps){
				debug +=  i + "\t";
			}
			
			System.out.println(debug);
		}
		
		return InvalidGrps;
	}

	
	/**
	 * @return List of missing items (or duplicate) with count
	 */
	
	class ItemCount{
		Item itm;
		int cnt;
		
		public ItemCount(Item itm, int cnt){
			this.itm = itm;
			this.cnt = cnt;
		}
	}
	
	public ArrayList<ItemCount> missingItems(boolean verbose){
		ArrayList<ItemCount> tuples = new ArrayList<ItemCount>();
		
		Hashtable<Item, Integer> cnt = new Hashtable<Item, Integer>();
		for(Item i : this.slap.Itm){
			cnt.put(i, 0);
		}
		
		//Check the appearance of all items in this.row
		for(ArrayList<ArrayList<Item>> irow: this.row){
			for(ArrayList<Item> grp: irow){
				for(Item i: grp){
					cnt.put(i, cnt.get(i)+1);
				}
			}
		}
		
		//Check all the items to find those don't appear for the correct times
		for(Item i: this.slap.Itm){
			if(cnt.get(i) != 1){
				ItemCount t = new ItemCount(i, cnt.get(i));
				tuples.add(t);
			}
		}
		if(verbose && tuples.size() > 0){
			String debug = "Has " +  this.slap.Itm.size() + " items in total \n";
			 debug += "Get missing items : \n";
			for(ItemCount i : tuples){
				debug +=  i.itm.toString() + "\t " + i.cnt + "\n";
			}
			
			System.out.println(debug);
		}
		
	    return tuples;
	}
	

	public boolean isFeasible(boolean verbose){
		ArrayList<Integer> infr = this.infoRows(verbose);
		ArrayList<String> inG = this.infGroups(2, null, verbose);
		ArrayList<Layout.ItemCount> itms = this.missingItems(verbose);
		
		if (infr.size() == 0 && inG.size()==0 && itms.size()==0){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * 
	 * @param row
	 * @param loc
	 * @return Return triple group number,item index given row and location index
	 */
	public SLAP.tuple getGroupItemIndex(int row, int loc){
		int grp = 0;
        while (loc >= this.row.get(row).get(grp).size()){
        	loc -= this.row.get(row).get(grp).size(); 
            grp +=1 ;
        }
        
        SLAP.tuple t = new SLAP.tuple(grp, loc);
        return  t;
	}
	
	public Item _getitem_(SLAP.tuple row_loc){
		SLAP.tuple grp_iloc = this.getGroupItemIndex(row_loc.row, row_loc.loc);
		return this.row.get(row_loc.row).get(grp_iloc.row).get(grp_iloc.loc);
	}
	
	public void _setitem_(SLAP.tuple row_loc, Item val){
		SLAP.tuple grp_iloc = this.getGroupItemIndex(row_loc.row, row_loc.loc);
		this.row.get(row_loc.row).get(grp_iloc.row).set(grp_iloc.loc, val);
	}

	/**
	 * print layout by row
	 */
	public void printLayout(){
		int rcnt=0;
		for(ArrayList<ArrayList<Item>> row: this.row){
			String rowS =rcnt+": \t";
			for(ArrayList<Item> grp : row){
				for(Item i: grp){
					rowS += i.toString() + "\t";
				}
				//rowS += "\n";
			}
			System.out.println(rowS);
			rcnt++;
		}
	}
	
	/**
	 * Sort items for each product across both groups of the product
	 */
	public void itemSort(){
		Hashtable<String, Integer> idx = (Hashtable<String, Integer>)this.dic(this.slap.product, 0);
		ArrayList<SLAP.tuple> tuples = this.slap.allRowLoc();
		
		for(SLAP.tuple t: tuples){
			String prod = this._getitem_(t).product;
			this._setitem_(t, this.slap.product.get(prod).get(idx.get(prod)));
			idx.put(prod, idx.get(prod)+1);
		}
	}
	
	
	/**
	 * Sort groups in each row by average frequency
	 * @param row
	 */
	public void rowSort(ArrayList<ArrayList<Item>> row){
		ArrayList<ArrayList<ArrayList<Item>>> rows;
		
		if(row == null){
			rows = this.row;
		}
		else{
			rows = new ArrayList<ArrayList<ArrayList<Item>>>();
			rows.add(row);
		}
		
		for(ArrayList<ArrayList<Item>> irow: rows){
			//sort
			Collections.sort(irow, new Comparator<ArrayList<Item>>() {
															@Override
															public int compare(ArrayList<Item> o1, ArrayList<Item> o2) {
																return Double.compare(-avgFreq(o1), -avgFreq(o2));
															}
														});
		}

	}
	
	public double avgFreq(ArrayList<Item> x){
		double sum = 0;
		for(Item i : x){
			sum += i.freq;
		}
		return sum/x.size();
	}

	/**
	 * sort all row on freq
	 * @return
	 */
	public void allRowSort(){

		Collections.sort(this.row, new Comparator<ArrayList<ArrayList<Item>>>(){
										@Override
										public int compare(ArrayList<ArrayList<Item>> o1, ArrayList<ArrayList<Item>> o2) {
											return Double.compare(-sum(o1), -sum(o2));
										}
									});
		

	}
	
	public double sum(List<ArrayList<Item>> row){
		double sum = 0;
		for(ArrayList<Item> grp: row){
			for(Item itm: grp){
				sum += itm.freq;
			}
		}
		return sum;
	}

	/**
	 * Sort all groups of given size (or all groups without argument)
	 * @param grpSize
	 */
	public void groupSort(int grpSize){
		
		if(grpSize <= 0){
			for(int i = this.slap.nL -1; i >0; i--){
				this.groupSort(i);
			}
			return;
		}
		
		ArrayList<List<ArrayList<Item>>> sort = new ArrayList<List<ArrayList<Item>>>(); //list (-sum freq, list of groups length grpSize)
		ArrayList<pos> position = new ArrayList<pos>(); //list (distance to start, row, rangeStart,rangeEnd)
		ArrayList<List<ArrayList<Item>>> tail = new ArrayList<List<ArrayList<Item>>>();
		
		for(int h = 0; h < this.row.size(); h++){
			ArrayList<ArrayList<Item>> irow = this.row.get(h);
			
			int last = 0;
			int i = 0;
			int length = 0;
			while(i < irow.size()){
				int j = 0;
				for(j = i+1; j < irow.size()+1; j++){
					
					length = this.countARow(irow.subList(i, j));
					if(length >= grpSize) break;
				}
				
				if(length == grpSize){
					ArrayList<SLAP.tuple> possible = new ArrayList<SLAP.tuple>();
					possible.add(new SLAP.tuple(i, j));
					
					for(int ki = i+1; ki < j; ki++){
						for(int kj = ki+1; kj < irow.size()+1; kj++){
							if(this.countARow(irow.subList(ki, kj)) == grpSize){
								possible.add(new SLAP.tuple(ki, kj));
							}
						}
					}
					
					SLAP.tuple next = possible.get(this.random.nextInt(possible.size()));
					int ki = next.row;
					int kj = next.loc;
					
					sort.add(irow.subList(ki, kj)); //to be sorted based on the -sum freq, sort.append( (-sum(itm.freq for g in row[ki:kj] for itm in g), row[ki:kj]))
					pos p = new pos( h + this.countARow(irow.subList(0, ki)), h, irow.subList(last, ki)); //position.append( (h + sum(len(g) for g in row[:ki]),h,row[last:ki]) )
					position.add(p);
					last = kj;
					i = kj-1;
				}
				i++;
			}
			tail.add(irow.subList(last, irow.size()));
		}
		
		Layout oldSoln = new Layout(this.slap, "Copy", this.random);
		oldSoln.row =this.deepcopy3( this.row);
		int oldObj = this.objective();
		
		this.row = new ArrayList<ArrayList<ArrayList<Item>>>(); //for each shelf have list of lists of items
		for(int i: slap.S){
			this.row.add(new ArrayList<ArrayList<Item>>()); //add rows
		}
		//sort sort and position
		//System.out.println(sort);
		
		Collections.sort(sort, new Comparator<List<ArrayList<Item>>>() {
									@Override
									public int compare(List<ArrayList<Item>> o1,List<ArrayList<Item>> o2) {
										return Double.compare(-sum(o1), -sum(o2));
									}
								});
		Collections.sort(position, new Comparator<pos>() {
									@Override
									public int compare(pos o1,pos o2) {
										return Integer.compare(o1.dist, o2.dist);
									}
								});
		for(int i = 0; i < sort.size(); i++){
			List<ArrayList<Item>> grps = sort.get(i);
			pos x = position.get(i);
			
			for(ArrayList<Item> grp: x.skipped){
				this.row.get(x.r).add(grp);
			}
			for(ArrayList<Item> grp: grps){
				this.row.get(x.r).add(grp);
			}
		}
		
		int h= 0;
		for(List<ArrayList<Item>> grps: tail){
			for(ArrayList<Item> grp: grps){
				this.row.get(h).add(grp);
			}
			
			if(this.countARow(this.row.get(h)) != this.slap.nL){
				System.out.println("Infeasible solution!");
				System.exit(-1);
			}
			h++;
		}
		
		if(this.objective() > oldObj){
			System.out.println("Error: new obj " + this.objective());
			this.printLayout();
			System.out.println("Error: old obj " + oldSoln);
			oldSoln.printLayout();
			System.out.println(" Did not improve!");
			System.exit(-1);
		}		
	}
	
	 static class pos{
		int dist;
		int r;
		List<ArrayList<Item>> skipped;
		
		public pos(int dist, int r, List<ArrayList<Item>> skipped){
			this.dist = dist;
			this.r = r;
			this.skipped = skipped;
		}
	}

    public static int countARow(List<ArrayList<Item>> row){
		ArrayList<ArrayList<Item>> x = new ArrayList<ArrayList<Item>>();
		for(ArrayList<Item> grp: row){
			x.add(grp);
		}
		return countARow(x);
	}
    
	public static int countARow(ArrayList<ArrayList<Item>> row){
		int count = 0;
		
		for(ArrayList<Item> i: row){
			count+= i.size();
		}
		
		return count;
	}
	
	class GROUP{
		String product;
		int size;
		
		public GROUP(String product, int size){
			this.product = product;
			this.size = size;
		}
	}
	
	class neigh{
		/**
		 * Object to encapsulate list of rows (a layout solution)
		 */
		ArrayList<ArrayList<GROUP>> rows;
		ArrayList<hash> hashList;
		
		public neigh(ArrayList<ArrayList<GROUP>> rows, hash hashCode){
			this.rows = rows;
			this.hashList = new ArrayList<hash>();
			this.hashList.add(hashCode);
		}
		
		public neigh(ArrayList<ArrayList<GROUP>> rows, ArrayList<hash> hashList){
			this.rows = rows;
			this.hashList = hashList;
		}

	}
	
	private ArrayList<ArrayList<ArrayList<Item>>> deepcopy3(ArrayList<ArrayList<ArrayList<Item>>> rows){
		ArrayList<ArrayList<ArrayList<Item>>> newRow = new ArrayList<ArrayList<ArrayList<Item>>>();
		
		for(ArrayList<ArrayList<Item>> row : rows){
			newRow.add(this.deepcopy2(row));
		}
		
		return newRow;
	}
	
	private ArrayList<ArrayList<Item>> deepcopy2(ArrayList<ArrayList<Item>> row){
		ArrayList<ArrayList<Item>> copyRow = new ArrayList<ArrayList<Item>>();
		
		for(ArrayList<Item> grp: row){
			copyRow.add((ArrayList<Item>)grp.clone());
		}
		return copyRow;
	}
	
 	private boolean hasProd(String Name, ArrayList<ArrayList<Item>> row){
		boolean hasP = false;
		
		for(ArrayList<Item> i: row){
			if(i.get(0).product.equals(Name)){
				hasP = true;
			}
		}
		return hasP;
	}
 	
	////////////////////////////////////////////
	/**
	 * When we have two subgroups on different rows, there are multiple ways of resizing these two groups.
	 * the resizing is not done by changing the size by 1 or by the minimum groups that can be found on the other row,
	 * it tries to find all the possible ways of resizing (in size, which means all one group x are considered the same)
	 * @param row1
	 * @param row2
	 * @return
	 */
	public ArrayList<ArrayList<ArrayList<Item>>> resizeAllPossibles(ArrayList<ArrayList<Item>> row1, ArrayList<ArrayList<Item>> row2, ArrayList<ArrayList<hash>> hashList){
		if(row1 == null || row2 == null){
			System.out.println("You need to specify the rows to be resized");
		}
		ArrayList<ArrayList<ArrayList<Item>>> rowsTBP = new ArrayList<ArrayList<ArrayList<Item>>>();
		rowsTBP.add(row1);
		rowsTBP.add(row2);
		ArrayList<String> pNames = this.infGroups(1, rowsTBP, false);
		
		ArrayList<String> tmpNames = new ArrayList<String>();
		
		for(String pName: pNames){
			if(hasProd(pName, row1) && hasProd(pName, row2)){
				tmpNames.add(pName);
			}
		}
		pNames = tmpNames;

		ArrayList<ArrayList<ArrayList<Item>>> resizedRows = new ArrayList<ArrayList<ArrayList<Item>>>();
		
		if(pNames.size() == 0){
			return resizedRows;
		}
		
		if(pNames.size() == 1){
			//only on product fit the condition
			ArrayList<ArrayList<Item>> part1 = this.lookup(row1, pNames.get(0)); //should only have one group comes in form a row
			ArrayList<ArrayList<Item>> part2 = this.lookup(row2, pNames.get(0)); //part1 is in row1, part2 in row2
			
			if((part1.get(0).size() == countARow(row1)) && (part2.get(0).size() == countARow(row2))) return resizedRows;
			
			ArrayList<ArrayList<Item>> inhRow1 = this.exLookup(row1, pNames);
			ArrayList<ArrayList<Item>> inhRow2 = this.exLookup(row2, pNames);
			
			//Move item from part1 to part2, as a return, move a product on row2 to row1
			//instead of looking for 1 item product on row2, try to find the one with the smallest size
			
			ArrayList<ArrayList<Item>> alts = this.findAllSizeSmallerThan(inhRow2, part1.get(0).size());
			
			for(ArrayList<Item> min: alts){
				ArrayList<ArrayList<Item>> newInhRow2 = this.exLookupItem(inhRow2, min);
				this.Move((ArrayList<Item>)part1.get(0).clone(), inhRow1, (ArrayList<Item>)part2.get(0).clone(), newInhRow2, min, resizedRows, hashList);
			}
			
			alts = this.findAllSizeSmallerThan(inhRow1, part2.get(0).size());
			
			for(ArrayList<Item> min: alts){
				ArrayList<ArrayList<Item>> newInhRow1 = this.exLookupItem(inhRow1, min);
				this.Move((ArrayList<Item>)part2.get(0).clone(), inhRow2, (ArrayList<Item>)part1.get(0).clone(), newInhRow1, min, resizedRows, hashList);
			}
		}
		else{
			//more than one product fit the condition, have to do it for each pair of product
			for(int i = 0; i < pNames.size()-1; i++){
				String name1 = pNames.get(i);
				String name2 = pNames.get(i+1);
				
				ArrayList<Item> grp1 = this.lookup(row1, name1).get(0);
				ArrayList<Item> grp2 = this.lookup(row2, name1).get(0);

				ArrayList<Item> grp3 = this.lookup(row1, name2).get(0);
				ArrayList<Item> grp4 = this.lookup(row2, name2).get(0);
				
				ArrayList<String> keys = new ArrayList<String>();
				keys.add(name1);
				keys.add(name2);
				ArrayList<ArrayList<Item>> inhRow1 = this.exLookup(row1, keys);
				ArrayList<ArrayList<Item>> inhRow2 = this.exLookup(row2, keys);
				
                //Move one item from grp1 to grp2, one item from grp4 to grp3
                this.MoveGs(grp1, grp2, grp4, grp3, inhRow1, inhRow2, resizedRows, row1, row2, hashList);
                //Move one item from grp2 to grp1, one item from grp3 to grp4
                this.MoveGs(grp2, grp1, grp3, grp4, inhRow2, inhRow1, resizedRows, row1, row2, hashList);
			}
		}

		if(resizedRows.size() % 2 != 0){
			System.out.println("Should have even number of resized rows");
		}
		
        if(resizedRows == null){
        	System.out.println("Pause here");
        }
		
		return resizedRows;
	}
	
	
	////////////////////////////////////////////
	
	/**
	 * @param fromG
	 * @param inhRowFrom
	 * @param toG
	 * @param inhRowTo
	 * @param sigItm
	 * @param resizedRows
	 * 
	 * We have two subgroups on different row and we move one item in 
	 * fromG to toG, and move sigItm on row2 to row1 to fill the gap
	 * This would guarantee the feasibility
	 */
	public void Move(ArrayList<Item> fromG, ArrayList<ArrayList<Item>> inhRowFrom, 
						ArrayList<Item> toG, ArrayList<ArrayList<Item>> inhRowTo,
							ArrayList<Item> sigProduct, ArrayList<ArrayList<ArrayList<Item>>> resizedRows,
							ArrayList<ArrayList<hash>> hashList){
		ArrayList<Item> newfromG = Search.convert(fromG.subList(0, fromG.size()-sigProduct.size()));
		ArrayList<Item> newToG = (ArrayList<Item>)toG.clone();
		newToG.addAll(Search.convert(fromG.subList(fromG.size()-sigProduct.size(), fromG.size())));
		
		ArrayList<ArrayList<Item>> newRow1 = this.deepcopy2(inhRowFrom);
		newRow1.add(sigProduct);
		newRow1.add(newfromG);
		
		ArrayList<ArrayList<Item>> newRow2 = this.deepcopy2(inhRowTo);
		newRow2.add(newToG);
		
		ArrayList<hash> hashTobeAdded = new ArrayList<hash>();
	//	hashTobeAdded.add(new hash(newfromG.get(0).product, newfromG.size(), newToG.size()));
		hashTobeAdded.add(new hash(newfromG.get(0).product));
		hashList.add(hashTobeAdded);
		
        this.rowSort(newRow1);
        this.rowSort(newRow2);
        
        resizedRows.add(newRow1);
        resizedRows.add(newRow2);
        
//        if(countARow(newRow1) != 5 || countARow(newRow2) != 5){
//        	System.err.println("Incorrect");
//        }
	}
		
	/**
	 * 
	 * @param fromG1:Move the last item in fromG1 to toG1
	 * @param toG1
	 * @param fromG2: Move the last item in fromG2 to toG2
	 * @param toG2
	 * @param inhRow1
	 * @param inhRow2
	 * @param resizedRows
	 */
	public void MoveGs(ArrayList<Item> fromG1, ArrayList<Item> toG1,
						ArrayList<Item> fromG2, ArrayList<Item> toG2,
							ArrayList<ArrayList<Item>> inhRow1, ArrayList<ArrayList<Item>> inhRow2,
								ArrayList<ArrayList<ArrayList<Item>>> resizedRows,
								ArrayList<ArrayList<Item>> row1, ArrayList<ArrayList<Item>> row2,
								ArrayList<ArrayList<hash>> hashList){
		
		//initialize
		ArrayList<Item> newFromG1 = null;
		ArrayList<Item> newFromG2  = null;
		
		ArrayList<Item> newToG1 = (ArrayList<Item>)toG1.clone();
		if(fromG1.size() > 1){
			newFromG1 = Search.convert(((ArrayList<Item>)fromG1.clone()).subList(0, fromG1.size()-1));
			newToG1.add(0, fromG1.get(fromG1.size()-1));
		}
		
		ArrayList<Item> newToG2 = (ArrayList<Item>)toG2.clone();
		if(fromG2.size() > 1){
			newFromG2 = Search.convert(((ArrayList<Item>)fromG2.clone()).subList(0, fromG2.size()-1));
			newToG2.add(0, fromG2.get(fromG2.size()-1));
		}

		ArrayList<hash> hashTobeAdded = new ArrayList<hash>();
		
		//Where to add these new groups
		ArrayList<ArrayList<Item>> newRow1 = this.deepcopy2(inhRow1);
		newRow1.add(newToG2);
		ArrayList<ArrayList<Item>> newRow2 = this.deepcopy2(inhRow2);
		newRow2.add(newToG1);
		
		if(newFromG1 != null){ 
			newRow1.add(newFromG1);
			//hashTobeAdded.add(new hash(newFromG1.get(0).product, newFromG1.size(), newToG1.size()));
			hashTobeAdded.add(new hash(newFromG1.get(0).product));
		}
		else{
			newRow2.add(fromG1);
			//hashTobeAdded.add(new hash(fromG1.get(0).product, fromG1.size(), newToG1.size()));
			hashTobeAdded.add(new hash(fromG1.get(0).product));
		}
        if (newFromG2 != null){
        	newRow2.add(newFromG2);
        	//hashTobeAdded.add(new hash(newFromG2.get(0).product, newFromG2.size(), newToG2.size()));
        	hashTobeAdded.add(new hash(newFromG2.get(0).product));
        }
        else{
        	newRow1.add(fromG2);
        	//hashTobeAdded.add(new hash(fromG2.get(0).product, fromG2.size(), newToG2.size()));
        	hashTobeAdded.add(new hash(fromG2.get(0).product));
        }
        
        hashList.add(hashTobeAdded);
        
        this.rowSort(newRow1);
        this.rowSort(newRow2);
        
        resizedRows.add(newRow1);
        resizedRows.add(newRow2);		
        
        if(this.countARow(newRow1) != this.countARow(row1) || this.countARow(newRow2) != this.countARow(row2)){
        	System.err.println("Incorrect row no. in MoveGs");
        	
        }
	}

	private boolean lookup(ArrayList<ArrayList<Item>> row, Item itm){
		boolean res= false;
		for(ArrayList<Item> grp: row){
			for(Item i: grp){
				if(i.equals(itm)){
					res = true;
					break;
				}
			}
		}
		return res;
	}
	
	/**
	 * @param rows
	 * @return
	 * 
	 *  "This function only change the subgroups on different row by 1 
	 *  and return all the possible new solns"
	 *  
	 *    "possible choice may include the following:"
	 *       "1. There are two pairs of groups on two rows, the former one 
	 *       	always give up the last element or get the first element 
	 *       	from another group:"
	 *       "2. The same row has single item that can be moved to another 
	 *       	row, randomly pick is fine as it will do group sort later"
	 *       
	 */
	public ArrayList<neigh> resizeDifRow(ArrayList<ArrayList<ArrayList<Item>>> mul_rows){
		ArrayList<ArrayList<ArrayList<Item>>> rows;
	    if(mul_rows == null){
	    	rows = this.row;
	    }
	    else{
	    	rows = mul_rows;
	    }
	       
	    ArrayList<neigh> neighbours = new ArrayList<neigh>();
	    int cnt1 = 0;
	    ArrayList<ArrayList<ArrayList<Item>>> rowCopy = deepcopy3(this.row);
	    
	    for(int i = 0; i < rows.size() - 1; i++){
	    	ArrayList<ArrayList<Item>> row1 = rows.get(i);
	    	
	    	int cnt2 = cnt1+1;
	    	for(int j = cnt1+1; j < rows.size(); j++){
	    		ArrayList<ArrayList<Item>> row2 = rows.get(j);
	    		
	    		ArrayList<ArrayList<hash>> hashLists = new ArrayList<ArrayList<hash>>();
	    		ArrayList<ArrayList<ArrayList<Item>>> resizedRows = this.resizeAllPossibles(row1, row2, hashLists);
	    		
	    	//	System.out.println(hashLists.size() + "\t" + resizedRows.size());
	    		
	    		for(int k = 0; k < resizedRows.size()-1; k+=2){
	    			ArrayList<ArrayList<Item>> newRow1 = resizedRows.get(k);
	    			ArrayList<ArrayList<Item>> newRow2 = resizedRows.get(k+1);
	    			
	    			this.row.set(cnt1, newRow1);
	    			this.row.set(cnt2, newRow2);	    
	    			
	    			neighbours.add(new neigh(encode(this.row), hashLists.remove(0)));
	    			this.row.set(cnt1, this.deepcopy2(rowCopy.get(cnt1)));
	    			this.row.set(cnt2, this.deepcopy2(rowCopy.get(cnt2)));
	    		}
	    		cnt2++;
	    	}
	    	cnt1++;	
	    }
	    return neighbours;
	}
	
	/**
	 * @param row
	 * @return This function transfer a row by count the number of items in each group
	 */
	private ArrayList<GROUP> translate(ArrayList<ArrayList<Item>> row){
		ArrayList<GROUP> TransRow = new ArrayList<GROUP>();
		for(ArrayList<Item> grp: row){
			GROUP g = new GROUP(grp.get(0).product, grp.size());
			TransRow.add(g);
		}
		return TransRow;
	}
	
	
	public ArrayList<ArrayList<GROUP>> encode(ArrayList<ArrayList<ArrayList<Item>>> rows){
		ArrayList<ArrayList<GROUP>> TransRows = new ArrayList<ArrayList<GROUP>>();
		for(ArrayList<ArrayList<Item>> row: rows){
			TransRows.add(translate(row));
		}
		return TransRows;
	}
	
	
	public ArrayList<ArrayList<ArrayList<Item>>> decode(ArrayList<ArrayList<GROUP>> tRows){
		ArrayList<ArrayList<ArrayList<Item>>> soln = new ArrayList<ArrayList<ArrayList<Item>>>();
		
		Hashtable<String, Integer> idx = (Hashtable<String, Integer>)this.dic(this.slap.product, 0);

		for(ArrayList<GROUP> tRow: tRows){
			//do it row by row
			
			ArrayList<ArrayList<Item>> rowTobeAdded = new ArrayList<ArrayList<Item>>();
			for(GROUP g: tRow){
				ArrayList<Item> fullProduct = (ArrayList<Item>) this.slap.product.get(g.product).clone();
				int startIdx = idx.get(g.product);
				int sizeNd = g.size;
				
				List<Item> subList= fullProduct.subList(startIdx, startIdx+sizeNd);
				rowTobeAdded.add(Search.convert(subList));
				
				//do not forget to update idx
				idx.put(g.product, idx.get(g.product) + sizeNd);
			}
			
			soln.add(rowTobeAdded);
		}
		return soln;
	}
	/**
	 * @param row to be resized and it's idx, if row is null, then resize all rows
	 * @return
	 *  "This function only change the subgroups on same row 
	 *  	by 1 and return all the possible new solns stored in neigh"
     *  "Each possible choice lead to a new solution"
     *  For each group we only need to keep the product name 
     *  	and the number of items and this will be translated into real solution using similar method in itemSort
	 */
	public ArrayList<neigh> resizeSameRow(ArrayList<ArrayList<Item>> single_row, int idx){
		ArrayList<ArrayList<ArrayList<Item>>> rows;
		boolean isSglRow = false;
		if(single_row != null){
			rows = new ArrayList<ArrayList<ArrayList<Item>>>();
			rows.add(single_row);
			isSglRow = true;
		}
		else{
			rows = this.row;
		}
		
		ArrayList<neigh> neighbours = new ArrayList<neigh>();
		
		for(int i = 0; i < rows.size(); i++){
			ArrayList<ArrayList<Item>> irow = rows.get(i);
			
			ArrayList<ArrayList<ArrayList<Item>>> rowToCheck = new ArrayList<ArrayList<ArrayList<Item>>>();
			rowToCheck.add(irow);
			ArrayList<String> ps = this.infGroups(1,rowToCheck , false);
			
			//Has groups to be rearranged
			if(ps.size() >= 1){
				ArrayList<ArrayList<Item>> inhRow = new ArrayList<ArrayList<Item>>(); // groups that don't need to be changes
				for(ArrayList<Item> grp: irow){
					if(ps.indexOf(grp.get(0).product) < 0){
						inhRow.add(grp);
					}
				}
				
				if(inhRow.size() == 0 && ps.size() == 0) continue;  //Only one product on the shelf, no need to do any improvement
				
				ArrayList<ArrayList<Item>> grpsAdd = new ArrayList<ArrayList<Item>>(); //a list of products with name in ps
				
				Iterator<String> proNames = (Iterator<String>)this.slap.product.keys();
				while(proNames.hasNext()){
					String proName = proNames.next();
					if(ps.indexOf(proName) >= 0){
						grpsAdd.add(this.slap.product.get(proName));
					}
				}
				
				ArrayList<ArrayList<Item>> tmp = new ArrayList<ArrayList<Item>>();
				for(ArrayList<Item> g : grpsAdd){
					//product with size 2 do not need to be resplit
					if(g.size() == 2){
						inhRow.addAll(lookup(irow, g.get(0).product));
						ps.remove(g.get(0).product);
					}
					else{
						tmp.add(g);
					}
				}
				
				if(tmp.size() == 0) continue; //no grps are chosen to generate new neighbours
				
				grpsAdd = tmp;
				ArrayList<ArrayList<Item>> allSplit = new ArrayList<ArrayList<Item>>();
				for(String key: ps){
					allSplit.addAll(lookup(irow, key));
				}
				
				int idxTobeAdded;
				if(isSglRow){
					//the newRow should be added to idx, not i
					idxTobeAdded = idx;
				}else{
					idxTobeAdded = i;
				}
				
				for(ArrayList<Item> grpAdd: grpsAdd){
					//for each product to be resplit, we have two options
					ArrayList<ArrayList<Item>> splits = lookup(irow, grpAdd.get(0).product);
					ArrayList<ArrayList<Item>> splitNotCount = new ArrayList<ArrayList<Item>>();
					
					for(ArrayList<Item> g: allSplit){
						if(splits.indexOf(g) < 0){
							splitNotCount.add(g);
						}
					}
					
					if(splits.size() != 2){
						System.err.println("Should have 2 splits here");
					}
					
					ArrayList<Item> head = splits.get(0);
					ArrayList<Item> tail = splits.get(1);
					
					if(head.size() > 1){
						//move one item in split[0] to split[1], as there are in the same row, always move the last one in split[0] and insert the the head of split[1]
						ArrayList<Item> newHead = Search.convert(head.subList(0, head.size()-1));
						ArrayList<Item> newTail = (ArrayList<Item>)tail.clone();
						newTail.add(0,head.get(head.size()-1));
						ArrayList<ArrayList<Item>> newRow = new ArrayList<ArrayList<Item>>();
						newRow.addAll(inhRow);
						newRow.add(newHead);
						newRow.add(newTail);
						newRow.addAll(splitNotCount);
						this.rowSort(newRow);
						ArrayList<ArrayList<ArrayList<Item>>> newRows = new ArrayList<ArrayList<ArrayList<Item>>>();
						int cnt = 0;
						for(ArrayList<ArrayList<Item>> r : this.row){
							
							if(cnt == idxTobeAdded){
								newRows.add(newRow);
							}else{
								newRows.add(r);
							}
							cnt++;
						}
					//	neighbours.add(new neigh(encode(newRows), new hash(newHead.get(0).product, newHead.size(), newTail.size())));
						neighbours.add(new neigh(encode(newRows), new hash(newHead.get(0).product)));

					}
					if(tail.size() > 1){
						// move one item in split[1] to split[0]
						
						ArrayList<Item> newTail = Search.convert(tail.subList(1, tail.size()));
						ArrayList<Item> newHead = (ArrayList<Item>)head.clone();
						newHead.add(tail.get(0));
						ArrayList<ArrayList<Item>> newRow = new ArrayList<ArrayList<Item>>();
						newRow.addAll(inhRow);
						newRow.add(newHead);
						newRow.add(newTail);
						newRow.addAll(splitNotCount);
						this.rowSort(newRow);
						ArrayList<ArrayList<ArrayList<Item>>> newRows = new ArrayList<ArrayList<ArrayList<Item>>>();
						int cnt = 0;
						for(ArrayList<ArrayList<Item>> r : this.row){
							
							if(cnt == idxTobeAdded){
								newRows.add(newRow);
							}else{
								newRows.add(r);
							}
							cnt++;
						}
						//neighbours.add(new neigh(encode(newRows), new hash(newHead.get(0).product, newHead.size(), newTail.size())));
						neighbours.add(new neigh(encode(newRows), new hash(newHead.get(0).product)));

					}
				}
			}
		}
		
		return neighbours;
	}
	
	
	private ArrayList<Item> findMinSize(ArrayList<ArrayList<Item>> from){
		ArrayList<Item> min = null;
		
		for(ArrayList<Item> cur: from){
			if(min == null || min.size() >= cur.size()){
				min = cur;
			}
		}
		
		return min;
	}
	
	private ArrayList<ArrayList<Item>> findAllSizeSmallerThan( ArrayList<ArrayList<Item>> from, int size){
		//sort based on size of groups
		 ArrayList<ArrayList<Item>> fromCopy = ( ArrayList<ArrayList<Item>>)from.clone();
		Collections.sort(fromCopy, new Comparator<ArrayList<Item>>() {
			@Override
			public int compare(ArrayList<Item> o1, ArrayList<Item> o2) {
				return Integer.compare(o1.size(), o2.size());
			}
		});
		
		ArrayList<ArrayList<Item>> list = new ArrayList<ArrayList<Item>>();
		for(ArrayList<Item> grp:fromCopy ){
			if(grp.size() >= size) break;
			
			if(list.size() == 0 || grp.size() > list.get(list.size() - 1).size()){
				list.add(grp);
			}
		}
		
		return list;
	}
	
	/**
	 * 
	 * @param from
	 * @param size
	 * @return a list of groups from a row with size less than (size)
	 */
	private ArrayList<ArrayList<Item>> filterSize(ArrayList<ArrayList<Item>> from, int size){
		ArrayList<ArrayList<Item>> fled = new ArrayList<ArrayList<Item>>();
		
		for(ArrayList<Item> grp: from){
			if(grp.size() == size){
				fled.add(grp);
			}
		}
		return fled;
	}
	
	private ArrayList<ArrayList<Item>> exLookup(ArrayList<ArrayList<Item>> from, ArrayList<String> keys){
		ArrayList<ArrayList<Item>> grps = new ArrayList<ArrayList<Item>>();
		
		for(ArrayList<Item> grp: from){
			if(keys.indexOf(grp.get(0).product) < 0){
				grps.add(grp);
			}
		}
		return grps;
	}
	
	private ArrayList<ArrayList<Item>> exLookupItem(ArrayList<ArrayList<Item>> from, ArrayList<Item> item){
		ArrayList<ArrayList<Item>> grps = new ArrayList<ArrayList<Item>>();
		
		for(ArrayList<Item> grp: from){
			//compare Item in grp
			if(grp.size() != item.size()){
				grps.add(grp);
				continue;
			}
			else{
				boolean isEqual = true;
				for(int i = 0; i < grp.size(); i++){
					if(!grp.get(i).equals(item.get(i))){
						isEqual = false;
						break;
					}
				}
				
				if(!isEqual){
					grps.add(grp);
				}
			}
		}
		return grps;
	}
	
	private ArrayList<ArrayList<Item>> exLookupItems(ArrayList<ArrayList<Item>> from, ArrayList<ArrayList<Item>> items){
		
		for(ArrayList<Item> itm: items){
			from = this.exLookupItem(from, itm);
		}
		return from;
	}

	private ArrayList<ArrayList<Item>> lookup(ArrayList<ArrayList<Item>> from, ArrayList<String> keys){
		ArrayList<ArrayList<Item>> grps = new ArrayList<ArrayList<Item>>();
		
		for(String key: keys){
			grps.addAll(this.lookup(from, key));
		}
		return grps;
	}
	
	private ArrayList<ArrayList<Item>> lookup(ArrayList<ArrayList<Item>> from, String key){
		ArrayList<ArrayList<Item>> res= new ArrayList<ArrayList<Item>>();
		for(ArrayList<Item> i: from){
			if(i.get(0).product.equals(key)){
				res.add(i);
			}
		}
		return res;
	}

	
}
