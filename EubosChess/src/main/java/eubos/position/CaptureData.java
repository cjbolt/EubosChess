package eubos.position;

import eubos.board.Piece;

public class CaptureData {
	
	public static final int NULL_CAPTURE = 0x0;
	
	public static int getSquare(int captureData) { return ((captureData >> 8) & Position.MASK); }
	
	public static int getPiece(int captureData) { return (captureData & Piece.PIECE_WHOLE_MASK); }
	
	public static int setSquare(int captureData, int position) {
		captureData &= ~(Position.MASK << 8);
		captureData |= (position << 8);
		return captureData;
	}
	
	public static int setPiece(int captureData, int piece) {
		captureData &= ~Piece.PIECE_WHOLE_MASK;
		captureData |= (Piece.PIECE_WHOLE_MASK & piece);
		return captureData;
	}
	
	public static int valueOf(int piece, int square) {
		int captureData = NULL_CAPTURE;
		captureData = (piece & Piece.PIECE_WHOLE_MASK) | ((square & Position.MASK) << 8);
		return captureData;
	}
};
