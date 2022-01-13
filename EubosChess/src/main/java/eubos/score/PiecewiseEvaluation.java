package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	short black = 0;
	short white = 0;
	short position = 0;
	short dynamicPosition = 0;
	
	public PiecewiseEvaluation() {
	}
	
	public PiecewiseEvaluation(short white, short black, short position) {
		this.black = black;
		this.white = white;
		this.position = position;
	}
	
	public short getBlack() {return black;}
	public short getWhite() {return white;}
	
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
	
	private void subBlack(short toAdd) { black -= toAdd; }
	private void subWhite(short toAdd) { white -= toAdd; }
	
	public void subPiece(boolean isWhite, int piece_no_colour) {
		short value = Piece.PIECE_TO_MATERIAL_LUT[piece_no_colour];
		if (isWhite) {
			subWhite(value);
		} else {
			subBlack(value);
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
	
	private void addDynamicPositionWhite(short pstBoost) { dynamicPosition += pstBoost; }
	private void addDynamicPositionBlack(short pstBoost) { dynamicPosition -= pstBoost; }
	public void addDynamicPosition(boolean isWhite, short pstBoost) {
		if (isWhite) {
			addDynamicPositionWhite(pstBoost);
		} else {
			addDynamicPositionBlack(pstBoost);
		}
	}
	
	private void subPositionWhite(short pstBoost) { position -= pstBoost; }
	private void subPositionBlack(short pstBoost) { position += pstBoost; }
	public void subPosition(boolean isWhite, short pstBoost) {
		if (isWhite) {
			subPositionWhite(pstBoost);
		} else {
			subPositionBlack(pstBoost);
		}
	}
	
	public short getPosition() { return (short)(position + dynamicPosition); }
	public void clearDynamicPosition() { dynamicPosition = 0; }
	
}
