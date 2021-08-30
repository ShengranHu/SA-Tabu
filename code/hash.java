/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */
package TCYB;

public class hash {
	public String SKU;
	public int size_g1;
	public int size_g2;
	
	public hash(String SKU){
		this.SKU = SKU;
	}
	
	public boolean equals(Object o){
		hash obj = (hash)o;
		
		if(obj.SKU.equals(this.SKU))
			return true;
		else return false;
	}
	
	public String toString(){
		return this.SKU ;
	}
}
