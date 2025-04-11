package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	
	public int [] numberOfPieces;
	
	public PiecewiseEvaluation() {
		numberOfPieces = new int [16];
	}
	
	public static final int PIECE_PHASE = 192;
	public static final int ROOK_PHASE = 320;
	public static final int QUEEN_PHASE = 640;
	public static final int TOTAL_PHASE = PIECE_PHASE * 8 + ROOK_PHASE * 4 + QUEEN_PHASE * 2;
	
	public boolean isEndgame() {
		// Phase calculation; pawns are excluded, king is excluded
		int phase = TOTAL_PHASE;
		phase -= numberOfPieces[Piece.WHITE_KNIGHT] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.WHITE_BISHOP] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.WHITE_ROOK] * ROOK_PHASE;
		phase -= numberOfPieces[Piece.WHITE_QUEEN] * QUEEN_PHASE;
		phase -= numberOfPieces[Piece.BLACK_KNIGHT] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.BLACK_BISHOP] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.BLACK_ROOK] * ROOK_PHASE;
		phase -= numberOfPieces[Piece.BLACK_QUEEN] * QUEEN_PHASE;
		// Phase is now a 10 bit fixed point fraction of the total phase
		return phase > 3000;
	}

	
//	public boolean phaseGreaterThan3000() {
//
//		int num_minor = numberOfPieces[Piece.WHITE_KNIGHT];
//		num_minor += numberOfPieces[Piece.WHITE_BISHOP];
//		num_minor += numberOfPieces[Piece.BLACK_KNIGHT];
//		num_minor += numberOfPieces[Piece.BLACK_BISHOP];
//		if (num_minor > 5)
//			return false;
//		
//		int num_queens = numberOfPieces[Piece.WHITE_QUEEN];
//		num_queens += numberOfPieces[Piece.BLACK_QUEEN];
//		if (num_queens > 1)
//			return false;
//		
//		int num_major = numberOfPieces[Piece.WHITE_ROOK];
//		num_major += numberOfPieces[Piece.BLACK_ROOK];
//		if (num_major > 2)
//			return false;
//		
//		if (num_major >= 1 && num_queens >= 1)
//			return false;
//		
//		if (num_minor >= 2 && num_major >= 2)
//			return false;
//		return true;
//	}

	public boolean phaseLessThan4000() {
		if (numberOfPieces[Piece.WHITE_KNIGHT] > 0)
			return true;
		if (numberOfPieces[Piece.WHITE_ROOK] > 0)
			return true;
		if (numberOfPieces[Piece.WHITE_BISHOP] > 0)
			return true;
		if (numberOfPieces[Piece.WHITE_QUEEN] > 0)
			return true;
		if (numberOfPieces[Piece.BLACK_KNIGHT] > 0)
			return true;
		if (numberOfPieces[Piece.BLACK_ROOK] > 0)
			return true;
		if (numberOfPieces[Piece.BLACK_BISHOP] > 0)
			return true;
		if (numberOfPieces[Piece.BLACK_QUEEN] > 0)
			return true;
		return false;
	}
	
	public int getNumPieces() {
		int num = 2; // kings
		num += numberOfPieces[Piece.WHITE_KNIGHT];
		num += numberOfPieces[Piece.WHITE_BISHOP];
		num += numberOfPieces[Piece.WHITE_ROOK];
		num += numberOfPieces[Piece.WHITE_QUEEN];
		num += numberOfPieces[Piece.WHITE_PAWN];
		num += numberOfPieces[Piece.BLACK_KNIGHT];
		num += numberOfPieces[Piece.BLACK_BISHOP];
		num += numberOfPieces[Piece.BLACK_ROOK];
		num += numberOfPieces[Piece.BLACK_QUEEN];
		num += numberOfPieces[Piece.BLACK_PAWN];
		return num;
	}
	
	public void updateWhenUndoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		int pawnToReplace = (promoPiece & Piece.BLACK)+Piece.PAWN;
		numberOfPieces[pawnToReplace]++;
		numberOfPieces[promoPiece]--;
	}
	
	public void updateWhenDoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		int pawnToRemove = (promoPiece & Piece.BLACK)+Piece.PAWN;
		numberOfPieces[pawnToRemove]--;
		numberOfPieces[promoPiece]++;
	}
	
	public void updateForCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]--;
	}
	
	public void updateForReplacedCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]++;
	}
}
