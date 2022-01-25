package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	public short black = 0;
	public short white = 0;
	public short position = 0;
	public short positionEndgame = 0;
	public short dynamicPosition = 0;
	
	public int phase = 0;
	public int p;
	public int n;
	public int b;
	public int r;
	public int q;
	
	static final int PawnPhase = 0;
	static final int KnightPhase = 1;
	static final int BishopPhase = 1;
	static final int RookPhase = 2;
	static final int QueenPhase = 4;
	static final int TotalPhase = PawnPhase*16 + KnightPhase*4 + BishopPhase*4 + RookPhase*4 + QueenPhase*2;
	
	public PiecewiseEvaluation() {
	}
	
	public boolean isEndgame() {
		setPhase();
		return phase > 170;
	}
	
	private void addBlack(short toAdd) { black += toAdd; }
	private void addWhite(short toAdd) { white += toAdd; }
	
	public void addPiece(boolean isWhite, int piece_no_colour) {
		short value = Piece.PIECE_TO_MATERIAL_LUT[piece_no_colour];
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
		setPhase();
		return phase;
	}
	
	public void setPhase() {
		phase = TotalPhase;

		phase -= p * PawnPhase;
		phase -= n * KnightPhase;
		phase -= b * BishopPhase;
		phase -= r * RookPhase;
		phase -= q * QueenPhase;

		phase = (phase * 256 + (TotalPhase / 2)) / TotalPhase;
	}
}
