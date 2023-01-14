package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	
	public static final int PIECE_PHASE = 192;
	public static final int ROOK_PHASE = 320;
	public static final int QUEEN_PHASE = 640;
	public static final int TOTAL_PHASE = PIECE_PHASE * 8 + ROOK_PHASE * 4 + QUEEN_PHASE * 2;
	
	public short mg_material = 0;
	public short eg_material = 0;
	public short dynamicPosition = 0;
	public int combinedPosition = 0;
	
	public int phase = 0;
	public int [] numberOfPieces;
	
	public PiecewiseEvaluation() {
		numberOfPieces = new int [16];
	}
	
	public boolean isEndgame() {
		return phase > 3000;
	}
	
	public short getMiddleGameDelta() { return mg_material; }
	
	public short getEndGameDelta() { return eg_material; }
	
	public short getPosition() { return (short)((short)(combinedPosition & 0xFFFF) + dynamicPosition); }
	
	public short getEndgamePosition() { return (short)((short)(combinedPosition >> 16) + dynamicPosition); }

	public int getPhase() {
		return phase;
	}
	
	public void setPhase() {
		// Phase calculation; pawns are excluded, king is excluded
		phase = TOTAL_PHASE;
		phase -= numberOfPieces[Piece.WHITE_KNIGHT] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.WHITE_BISHOP] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.WHITE_ROOK] * ROOK_PHASE;
		phase -= numberOfPieces[Piece.WHITE_QUEEN] * QUEEN_PHASE;
		phase -= numberOfPieces[Piece.BLACK_KNIGHT] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.BLACK_BISHOP] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.BLACK_ROOK] * ROOK_PHASE;
		phase -= numberOfPieces[Piece.BLACK_QUEEN] * QUEEN_PHASE;
		// Phase is now a 10 bit fixed point fraction of the total phase
	}
}
