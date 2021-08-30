/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */
package TCYB;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

public class SLAP {
	
	public Hashtable<String, ArrayList<Item>> product; // Each product has a list of items
	public Hashtable<String, ArrayList<Integer>> freq;
	public int nS;
	public int nL;
	public ArrayList<Integer> S; //number of shelves
	public ArrayList<Integer> L;
	public int n; //number of items
	public ArrayList<Item> Itm;
	
	public int numProducts(){
		return this.product.values().size();
	}
	
	public int maxPsize(){
		Iterator<ArrayList<Item>> itms = (this.product.values()).iterator();
		int maxSize = 0;
		
		while(itms.hasNext()){
			ArrayList<Item> tmp = itms.next();
			
			if(tmp.size() > maxSize){
				maxSize = tmp.size();
			}
		}
		
		return maxSize;
	}

	public double avgP(){
		Iterator<ArrayList<Item>> itms = (this.product.values()).iterator();
		int maxSize = 0;
		
		while(itms.hasNext()){
			ArrayList<Item> tmp = itms.next();
			maxSize += tmp.size();
		}
		
		return maxSize/numProducts();
	}
	
	public SLAP(String filename, int m) {
		if (filename != null) {
			this.product = new Hashtable<String, ArrayList<Item>>();
			this.freq = new Hashtable<String, ArrayList<Integer>>();
			this.Itm = new ArrayList<Item>();
			this.nS = m;
			this.load(filename);
		}
	}
	

	public  ArrayList<String> Split(String line) {
		ArrayList<String> SplittedString = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(line);

		while (st.hasMoreElements()) {
			String cur = st.nextElement().toString();
			SplittedString.add(cur);
		}
		return SplittedString;
	}

	/**
	 * read file
	 *
	 * @param filename
	 */
	public boolean load(String filename) {
		String line;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			int c = 0;
			
			while ((line = br.readLine()) != null) {
				if (line.startsWith("Product:")) {
					String p = Split(line).get(1);
					this.product.put(p, new ArrayList<Item>());
				} else if (line.startsWith("Item")) {
					c++;
					ArrayList<String> ItemInfo = Split(line);
					Item i = new Item(ItemInfo.get(1),
							Integer.parseInt(ItemInfo.get(2)), ItemInfo.get(3),
							ItemInfo.get(4), 0);
					i.no = c;
					this.product.get(i.product).add(i);
				}
			}
			Iterator<String> keys = (Iterator<String>) this.product.keys();
			while (keys.hasNext()) {
				String p = keys.next();
				Collections.sort(this.product.get(p), new Comparator<Item>() {
															@Override
															public int compare(Item o1, Item o2) {
																return Integer.compare(-o1.freq, -o2.freq);
															}
														});
				// this.product[p].sort(key=lambda x: -x.freq)
				// this.freq[p] = [ x.freq for x in this.product[p]]
				
				ArrayList<Integer> pops = new ArrayList<Integer>();
				ArrayList<Item> tmp = this.product.get(p);
				
				for(Item x: tmp){
					pops.add(x.freq);
				}
				this.freq.put(p, pops);
			}
			
			this.n=0;
			for(ArrayList<Item> it: this.product.values()){
				this.n += it.size();
				this.Itm.addAll(it);
			}
			
			
	            this.nL = (int)(this.n/this.nS);
	          //  this.nL = (int)(this.n / this.nS);
	            if ( this.nS * this.nL != this.n){
	                 System.out.println("WARNING: can't work out shelf length & n. shelves");
	            }
			

	        this.S = range(this.nS);
	        this.L = range(this.nL);
	        
	       // System.out.println("location: " + this.nL + "\t shelf:" + this.nS);

			return true;
		} catch (IOException e) {
			System.err.println("Error: " + e);
			return false;
		}
	}

	public ArrayList<Integer> range(int stop){
		ArrayList<Integer> res = new ArrayList<Integer>();
		for(int i = 0; i < stop;i++){
			res.add(i);
		}
		return res;
	}
	
	static class tuple{
		int row;
		int loc;
		
		public tuple(int row, int loc){
			this.row = row;
			this.loc = loc;
		}
	}
	
	/**
	 * @return iterate over all (row,location) pairs from closest to furthest
	 */
	public ArrayList<tuple> allRowLoc() {
		int row = 0;
		int loc = 0;
		int dist = 0;

		ArrayList<tuple> res = new ArrayList<tuple>();

		while (row < this.nS && loc < this.nL) {
			res.add(new tuple(row, loc));

			if (row > 0 && (loc + 1) < this.nL) {
				row--;
				loc++;
			} else {
				dist++;
				row = Math.min(this.nS - 1, dist);
				loc = dist - row;
			}
		}

		return res;
	}

}
