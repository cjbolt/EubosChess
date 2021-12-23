package eubos.position;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

public class MoveListIterator implements PrimitiveIterator.OfInt {

	private int [] moves = null;
	private int next = 0;

	public MoveListIterator(int[] normal_search_moves) {
		moves = normal_search_moves;
	}

	public boolean hasNext() {
		return next < moves.length;
	}

	public Integer next() {
		assert false; // use nextInt()
		return moves[next++];
	}

	@Override
	public void remove() {
	}

	@Override
	public void forEachRemaining(IntConsumer action) {
	}

	@Override
	public int nextInt() {
		return moves[next++];
	}
}
