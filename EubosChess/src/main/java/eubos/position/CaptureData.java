package eubos.position;

import eubos.board.Piece.PieceType;

public class CaptureData {
	PieceType target;
	int square;
	
	public CaptureData() {
		this(PieceType.NONE, Position.NOPOSITION);
	}
	
	public CaptureData(PieceType type, int atPos) {
		this.target = type;
		this.square = atPos;
	}
	
	public int getSquare() { return square; }
};
