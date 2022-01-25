package eubos.position;

public final class TrackedMove {
	public static final long NULL_TRACKED_MOVE = Move.NULL_MOVE;
	
	private static final int MOVE_SHIFT = 0;
	private static final long MOVE_MASK = 0xFFFFFFFFL << MOVE_SHIFT;
	
	private static final int EN_PASSANT_SHIFT = MOVE_SHIFT + 32;
	private static final long EN_PASSANT_MASK = 0xFFL << EN_PASSANT_SHIFT;
	
	private static final int CASTLING_SHIFT = EN_PASSANT_SHIFT + 8;
	private static final long CASTLING_MASK = 0xFL << CASTLING_SHIFT;
		
	private static final long DEFAULT_VALUE = (0x7FL << EN_PASSANT_SHIFT);
	
	public static long valueOf(int move, int enP, int castling) {
		// Default value is the most common value - optimisation
		long trackedMove = DEFAULT_VALUE;
		if (enP != Position.NOPOSITION) {
			long enPassant = enP;
			trackedMove &= ~EN_PASSANT_MASK;
			trackedMove |= enPassant << EN_PASSANT_SHIFT;
		}
		if (castling != 0) {
			long cast = castling;
			trackedMove |= cast << CASTLING_SHIFT;
		}
		// Always add the move
		trackedMove |= move;
		return trackedMove;
	}
	
	public static int getMove(long trackedMove) {
		long move = trackedMove & MOVE_MASK;
		return (int) move;
	}
	
	public static int getEnPassantTarget(long trackedMove) {
		long enP = (trackedMove & EN_PASSANT_MASK) >>> EN_PASSANT_SHIFT;
		return (int) enP;
	}
	
	public static int getCastlingFlags(long trackedMove) {
		long flags = (trackedMove & CASTLING_MASK) >>> CASTLING_SHIFT;
		return (int) flags;
	}
}
