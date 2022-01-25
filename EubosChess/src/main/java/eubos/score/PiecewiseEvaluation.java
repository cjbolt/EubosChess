package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	public short black = 0;
	public short white = 0;
	public short position = 0;
	public short positionEndgame = 0;
	public short dynamicPosition = 0;
	
	public int phase = 0;
	public int [] numberOfPieces;
	
	public static final int PawnPhase = 0;
	public static final int KnightPhase = 1;
	public static final int BishopPhase = 1;
	public static final int RookPhase = 2;
	public static final int QueenPhase = 4;
	public static final int TotalPhase = PawnPhase*16 + KnightPhase*4 + BishopPhase*4 + RookPhase*4 + QueenPhase*2;
	
	public PiecewiseEvaluation() {
		numberOfPieces = new int [16];
	}
	
	public boolean isEndgame() {
		return phase > 170;
	}
	
	private void addBlack(short toAdd) { black += toAdd; }
	private void addWhite(short toAdd) { white += toAdd; }
	
	public void addPiece(boolean isWhite, int piece_no_colour) {
		short value = Piece.PIECE_TO_MATERIAL_LUT[piece_no_colour];
		int side = isWhite ? 0 : Piece.BLACK;
		numberOfPieces[side+piece_no_colour]++;
		if (isWhite) {
			addWhite(value);
		} else {
			addBlack(value);
		}
	}
	
	public short getDelta() { return (short)(white-black); }

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
		phase = TotalPhase;

		phase -= numberOfPieces[Piece.WHITE_PAWN] * PawnPhase;
		phase -= numberOfPieces[Piece.WHITE_KNIGHT] * KnightPhase;
		phase -= numberOfPieces[Piece.WHITE_BISHOP] * BishopPhase;
		phase -= numberOfPieces[Piece.WHITE_ROOK] * RookPhase;
		phase -= numberOfPieces[Piece.WHITE_QUEEN] * QueenPhase;
		phase -= numberOfPieces[Piece.BLACK_PAWN] * PawnPhase;
		phase -= numberOfPieces[Piece.BLACK_KNIGHT] * KnightPhase;
		phase -= numberOfPieces[Piece.BLACK_BISHOP] * BishopPhase;
		phase -= numberOfPieces[Piece.BLACK_ROOK] * RookPhase;
		phase -= numberOfPieces[Piece.BLACK_QUEEN] * QueenPhase;

		phase = (phase * 256 + (TotalPhase / 2)) / TotalPhase;
	}
}
