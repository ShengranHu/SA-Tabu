package TCYB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.coinor.opents.*;

public class GCMoveManager implements MoveManager {

	public Search search;
	
	public double threshold;

	public double evaluatePredictionThreshold;

	public String mode;

	public GCMoveManager(Search search, double threshold, double evaluatePredictionThreshold) {
		this.search = search;
		
		this.threshold = threshold; //define proportion of the neighbours to be explored

		this.evaluatePredictionThreshold = evaluatePredictionThreshold;
	}

	public GCMoveManager(Search search, double threshold, double evaluatePredictionThreshold, String mode) {
		this.search = search;

		this.threshold = threshold; //define proportion of the neighbours to be explored

		this.evaluatePredictionThreshold = evaluatePredictionThreshold;

		this.mode = mode;
	}

	public GCMove[] getAllMoves(Solution sol) {
		return getAllMoves(sol, threshold);
	}

	public GCMove[] getAllMoves(Solution sol, double thre) {
		Layout layout = ((GCSolution) sol).layout;
		
		ArrayList<Layout.neigh> neighs = layout.resizeDifRow(null);
		//System.out.println(neighs.size());
		ArrayList<Layout.neigh> neighs2 = layout.resizeSameRow(null, -1);
		neighs.addAll(neighs2);

		int nextBufferPos = 0;
		int N = layout.slap.n * layout.slap.n;
		GCMove[] buffer = new GCMove[N];
		

		for(Layout.neigh neigh: neighs){
			buffer[nextBufferPos] = new GCMove(search, neigh.rows, neigh.hashList);
			nextBufferPos++;
		}
		//randomly go
		if(nextBufferPos == 0){
			int num = 10;

			for(int i = 0; i < num; i++){
				Layout neigh =  search.init(layout.slap);
				if(neigh != null){
					buffer[nextBufferPos] = new GCMove(search, neigh.encode(neigh.row), null);
					nextBufferPos++;
				}
			}
		}
        // Trim buffer
        
		return getSubArray(buffer, nextBufferPos, thre);
	} // end getAllMoves
	
	
	public GCMove[] getSubArray(GCMove[] input, int orgSize, double thre){
		
		if(thre == 1){
			GCMove[] moves = new GCMove[orgSize];
			
			System.arraycopy(input, 0, moves, 0, orgSize);
			
			return moves;
		}
		
		List<Integer> idx = new ArrayList<Integer>();
		
		for(int i = 0; i < orgSize; i++){
			idx.add(i);
		}
		Collections.shuffle(idx);
		
		Move[] adjust = new Move[orgSize];
		
		for(int i = 0; i < orgSize; i++){
			adjust[i] = input[idx.get(i)];
		}
		
		int subSize = (int) (orgSize * thre);
		GCMove[] moves = new GCMove[subSize];
		
		System.arraycopy(adjust, 0, moves, 0, subSize);
		
		return moves;
		
	}
}
