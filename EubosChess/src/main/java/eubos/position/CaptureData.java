package eubos.position;

import eubos.board.Piece;

public class CaptureData {
	int target;
	int square;
	boolean enPassant;
	
	public CaptureData() {
		this(Piece.NONE, Position.NOPOSITION, false);
	}
	
	public CaptureData(int type, int atPos, boolean enPassant) {
		this.target = type;
		this.square = atPos;
		this.enPassant = enPassant;
	}
	
	public int getSquare() { return square; }
	
	public int getPiece() { return target; }
	
	public boolean getEnPassant() {return enPassant; }
};
