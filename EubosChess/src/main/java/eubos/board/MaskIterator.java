package eubos.board;

import java.util.PrimitiveIterator;
import java.util.function.LongConsumer;

public class MaskIterator implements PrimitiveIterator.OfLong {

	private long [] setBitsIndexes = null;
	private int count = 0;
	private int next = 0;

	public MaskIterator(long bitBoard) {
		setBitsIndexes = new long[64];
		long scratchBitBoard = bitBoard;
		while ( scratchBitBoard != 0x0L ) {
			setBitsIndexes[count++] = Long.lowestOneBit(scratchBitBoard);
			// clear the lssb
			scratchBitBoard &= scratchBitBoard-1;
		}
	}

	public boolean hasNext() {
		return next < setBitsIndexes.length && next < count;
	}

	public Long next() {
		assert false; // use nextLong()
		return setBitsIndexes[next++];
	}

	@Override
	public void remove() {
	}

	@Override
	public void forEachRemaining(LongConsumer action) {
	}

	@Override
	public long nextLong() {
		return setBitsIndexes[next++];
	}
}
