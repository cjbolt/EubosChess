package eubos.position;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Piece.PieceType;

public class CaptureData {
	PieceType target;
	GenericPosition square;
	
	public CaptureData() {
		this(PieceType.NONE, null);
	}
	
	public CaptureData(PieceType type, GenericPosition atPos) {
		this.target = type;
		this.square = atPos;
	}
	
	public GenericPosition getSquare() { return square; }
};