package eubos.board;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

public class SetBitsIterator implements PrimitiveIterator.OfInt {

	private int [] setBitsIndexes = null;
	private int count = 0;
	private int next = 0;

	public SetBitsIterator(long bitBoard) {
		setBitsIndexes = new int[64];
		long scratchBitBoard = bitBoard;
		while ( scratchBitBoard != 0x0L ) {
			setBitsIndexes[count++] = Long.numberOfTrailingZeros(scratchBitBoard);
			// clear the lssb
			scratchBitBoard &= scratchBitBoard-1;
		}
	}

	public boolean hasNext() {
		return next < setBitsIndexes.length && next < count;
	}

	public Integer next() {
		assert false; // use nextInt()
		return setBitsIndexes[next++];
	}

	@Override
	public void remove() {
	}

	@Override
	public void forEachRemaining(IntConsumer action) {
	}

	@Override
	public int nextInt() {
		return setBitsIndexes[next++];
	}
}
