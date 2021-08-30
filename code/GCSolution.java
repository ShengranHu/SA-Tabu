/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */
package TCYB;
import java.util.ArrayList;
import java.util.Random;

import org.coinor.opents.*;

public class GCSolution extends SolutionAdapter {
	public Layout layout;
	
	public GCSolution (){} //Default constructor
	
	public GCSolution (Layout layout){	
		this.layout = layout;
		this.setObjectiveValue(new double[]{this.layout.obj});
	} //End constructor
	
	
	public GCSolution clone(){
		GCSolution copy = (GCSolution)super.clone();
		copy.layout = (Layout)this.layout.clone();
		copy.setObjectiveValue(new double[]{this.layout.obj});
		return copy;
	} //End Clone
	
}
