package eubos.board;

public class MaterialPhase {
	
	public int [] numberOfPieces;
	public int phase;
	
	public MaterialPhase() {
		numberOfPieces = new int [16];
	}
	
	public static final int PIECE_PHASE = 192;
	public static final int ROOK_PHASE = 320;
	public static final int QUEEN_PHASE = 640;
	public static final int TOTAL_PHASE = PIECE_PHASE * 8 + ROOK_PHASE * 4 + QUEEN_PHASE * 2;
	
	static private final int PHASE_LUT[];
	static {
		PHASE_LUT = new int[16];
		PHASE_LUT[Piece.BLACK_BISHOP] = PIECE_PHASE;
		PHASE_LUT[Piece.WHITE_BISHOP] = PIECE_PHASE;
		PHASE_LUT[Piece.BLACK_KNIGHT] = PIECE_PHASE;
		PHASE_LUT[Piece.WHITE_KNIGHT] = PIECE_PHASE;
		PHASE_LUT[Piece.BLACK_QUEEN] = PIECE_PHASE;
		PHASE_LUT[Piece.WHITE_QUEEN] = PIECE_PHASE;
	}
	
	public boolean isEndgame() {
		return phase > 3000;
	}
	
	public void updateWhenUndoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		numberOfPieces[promoPiece]--;
		phase -= PHASE_LUT[promoPiece];
	}
	
	public void updateWhenDoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		numberOfPieces[promoPiece]++;
		phase += PHASE_LUT[promoPiece];
	}
	
	public void updateForCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]--;
		phase += PHASE_LUT[currPiece];
	}
	
	public void updateForReplacedCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]++;
		phase -= PHASE_LUT[currPiece];
	}
}
