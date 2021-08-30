/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */
package TCYB;

public class HashSKUSize {
	public String SKU;
	public int size_g1;
	public int size_g2;
	
	public HashSKUSize(String SKU, int i, int j){
		this.SKU = SKU;
		this.size_g1 = i;
		this.size_g2 = j;
	}
	
	public boolean equals(Object o){
		HashSKUSize obj = (HashSKUSize)o;
		
		if(obj.SKU.equals(this.SKU)
				&& obj.size_g1 == this.size_g1
				&& obj.size_g2 == this.size_g2)
			return true;
		else if(obj.SKU.equals(this.SKU)
				&& obj.size_g2 == this.size_g1
				&& obj.size_g1 == this.size_g2)
			return true;
		else return false;
	}
	
	public String toString(){
		return this.SKU + " " + this.size_g1 + " " + this.size_g2;
	}
}
