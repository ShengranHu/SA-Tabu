/**
 * @author Jing Xie
 * @email jing@xie.us
 * Please only use the code for academic purposes.
 */
package TCYB;

import java.util.ArrayList;

import org.coinor.opents.Move;
import org.coinor.opents.Solution;
import org.coinor.opents.TabuList;


public class zeroTabuList implements TabuList
{
    /**
     * The value 10 will be used as the tenure if
     * the null constructor is used.
     *
     * @since 1.0-exp3
     */
    public final static int DEFAULT_TENURE = 10;
    
    private int   tenure;       // Tabu list tenure
    private hash[] tabuList;     // Data structure used to store list
    private int   currentPos;   // Monotomically increasing counter
    private int   listLength;   // Always equals tabuList.length
    
    
    /**
     * When the int[] used for the tabu list needs to be
     * expanded, it will be set to the requested tenure
     * times this factor.
     *
     * @since 1.0-exp3
     */
    private final static double LIST_GROW_FACTOR = 2.0;
    
    
    /**
     * Constructs a <code>SimpleTabuList</code> with the
     * {@link #DEFAULT_TENURE} value of ten (10).
     *
     * @since 1.0-exp3
     */
    public zeroTabuList()
    {   this( DEFAULT_TENURE );        
    }   // end SimpleTabuList
    
    
    /**
     * Constructs a <code>SimpleTabuList</code> with a given tenure.
     *
     * @param tenure the tabu list's tenure
     * @since 1.0-exp3
     */
    public zeroTabuList( int tenure )
    {
        super();
        
        if(tenure != 0){
            
            this.tenure     = tenure;
            this.listLength = (int) (tenure * LIST_GROW_FACTOR);
            this.tabuList   = new hash[ listLength ];
            this.currentPos = 0;
            for( int i = 0; i < listLength; i++ )
                this.tabuList[i] = new hash("");
            	//this.tabuList[i] = new hash("", Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
    }   // end SimpleTabuList
    
    
    
    /**
     * Determines if the {@link Move} is on the tabu list and ignores the
     * {@link Solution} that is passed to it. The move's identity is determined
     * by its <code>hasCode()</code> method, <strong>so it's imperative
     * that you override the <code>hashCode()</code> method with one 
     * that identifies the move appropriately for your problem</strong>.
     *
     * @param move A move
     * @param solution The solution before the move operation - ignored.
     * @return whether or not the tabu list permits the move.
     * @see Move
     * @see Solution
     * @since 1.0-exp3
     */
    public boolean isTabu(Solution fromSolution, Move move) 
    {   
    	
    	if(listLength != 0){
            ArrayList<hash> attrs = ((GCMove)move).hash();

           // System.out.println(attr);
            
            for( int i = 1; i <= tenure; i++ )
                if ( currentPos - i < 0 )
                    return false;
                else{
                	for(hash attr: attrs){	
                        if ( attr.equals(tabuList[ (currentPos-i) % listLength ])){
//                        	System.out.println("is tabooed!!!!!!!");
//                        	System.out.print(attr);
                            return true;
                        }
                	}
                }
            return false;
    	}
    	else{
    		return false;
    	}

    }   // end isTabu 
    
    /**
     * This method accepts a {@link Move} and {@link Solution} as
     * arguments and updates the tabu list as necessary.
     * <P>
     * Although the tabu list may not use both of the passed
     * arguments, both must be included in the definition.
     *
     * Records a {@link Move} on the tabu list by calling the move's
     * <code>hashCode()</code> method. <strong>It's imperative
     * that you override the <code>hashCode()</code> method with one 
     * that identifies the move appropriately for your problem</strong>.
     *
     * @param move The {@link Move} to register
     * @param solution The {@link Solution} before the move operation - ignored.
     * @see Move
     * @see Solution
     */
    public void setTabu(Solution fromSolution, Move move) 
    {        
    	if(listLength != 0){
    		ArrayList<hash> list = ((GCMove)move).hash();
    		
    		if(list != null){
        		for(hash h: list){	
                    tabuList[ (currentPos++) % listLength ] = h;
        		}
    		}
    	}
    }   // end setTabu
    
    
    /**
     * Returns the tenure being used by this tabu list.
     *
     * @return tabu list's tenure
     * @since 1.0-exp3
     */
    public int getTenure()
    {   return tenure;
    }   // end getTenure
    
    
    /**
     * Sets the tenure used by the tabu list. The data structure
     * being used to record the tabu list grows if the requested
     * tenure is larger than the array being used, but stays the 
     * same size if the tenure is reduced. This is for performance
     * reasons and insures that you can change the size of the
     * tenure often without a performance degredation.
     * A negative value will be ignored.
     *
     * @param tenure the tabu list's new tenure
     */
    public void setTenure( int tenure )
    {
        if( tenure < 0 )
            return;
        
        if(listLength != 0){ 
            if( tenure > this.tenure && tenure > tabuList.length )
            {
                listLength        = (int) (tenure * LIST_GROW_FACTOR);
                hash[] newTabuList = new hash[ listLength ];
                System.arraycopy( tabuList, 0, newTabuList, 0, tabuList.length );
                tabuList   = newTabuList;
            }   // end if: grow tabu list
        }

        this.tenure = tenure;
    }   // end setTenure
    
    
}   // end class SimpleTabuList

