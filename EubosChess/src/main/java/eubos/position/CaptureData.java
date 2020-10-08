package eubos.position;

import eubos.board.Piece;

public class CaptureData {
	int target;
	int square;
	
	public CaptureData() {
		this(Piece.NONE, Position.NOPOSITION);
	}
	
	public CaptureData(int type, int atPos) {
		this.target = type;
		this.square = atPos;
	}
	
	public int getSquare() { return square; }
	
	public int getPiece() { return target; }
};
