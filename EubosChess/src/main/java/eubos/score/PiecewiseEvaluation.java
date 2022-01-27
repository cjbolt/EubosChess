package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	
	public static final int PIECE_PHASE = 192;
	public static final int ROOK_PHASE = 320;
	public static final int QUEEN_PHASE = 640;
	public static final int TOTAL_PHASE = PIECE_PHASE * 8 + ROOK_PHASE * 4 + QUEEN_PHASE * 2;
	
	public short material = 0;
	public short position = 0;
	public short positionEndgame = 0;
	public short dynamicPosition = 0;
	
	public int phase = 0;
	public int [] numberOfPieces;
	
	public PiecewiseEvaluation() {
		numberOfPieces = new int [16];
	}
	
	public boolean isEndgame() {
		return phase > 2720;
	}
	
	public void addPiece(boolean isWhite, int piece_no_colour) {
		short value = Piece.PIECE_TO_MATERIAL_LUT[piece_no_colour];
		int side = isWhite ? 0 : Piece.BLACK;
		numberOfPieces[side+piece_no_colour]++;
		if (isWhite) {
			material += value;
		} else {
			material -= value;
		}
	}
	
	public short getDelta() { return material; }

	private void addPositionWhite(short pstBoost) { position += pstBoost; }
	private void addPositionBlack(short pstBoost) { position -= pstBoost; }
	public void addPosition(boolean isWhite, short pstBoost) {
		if (isWhite) {
			addPositionWhite(pstBoost);
		} else {
			addPositionBlack(pstBoost);
		}
	}
	
	public short getPosition() { return (short)(position + dynamicPosition); }
	
	public short getEndgamePosition() { return (short)(positionEndgame + dynamicPosition); }

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
