/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */

package TCYB;

public class Item {
	public String product;
	public int freq;
	public String colour;
	public String size;
	public int no;
	
	public Item(String product, int freq, String colour, String size, int no){
		this.product = product;
		this.freq = freq;
		this.colour = colour;
		this.size = size;
		this.no = no;
	}
	
	public boolean equals(Object o){
		Item other = (Item)o;
		return this.product.equalsIgnoreCase(other.product) && this.no == other.no;
	}
	
	public String toString(){
		return this.product + "-"+this.no+":" + this.freq ;		
	}
	

}
