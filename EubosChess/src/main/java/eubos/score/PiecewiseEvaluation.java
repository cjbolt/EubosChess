package eubos.score;

import eubos.board.Piece;

public class PiecewiseEvaluation {
	
	public int [] numberOfPieces;
	
	public PiecewiseEvaluation() {
		numberOfPieces = new int [16];
	}

	public int getNumPieces() {
		int num = 2; // kings
		num += numberOfPieces[Piece.WHITE_KNIGHT];
		num += numberOfPieces[Piece.WHITE_BISHOP];
		num += numberOfPieces[Piece.WHITE_ROOK];
		num += numberOfPieces[Piece.WHITE_QUEEN];
		num += numberOfPieces[Piece.WHITE_PAWN];
		num += numberOfPieces[Piece.BLACK_KNIGHT];
		num += numberOfPieces[Piece.BLACK_BISHOP];
		num += numberOfPieces[Piece.BLACK_ROOK];
		num += numberOfPieces[Piece.BLACK_QUEEN];
		num += numberOfPieces[Piece.BLACK_PAWN];
		return num;
	}
	
	public void updateWhenUndoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		int pawnToReplace = (promoPiece & Piece.BLACK)+Piece.PAWN;
		numberOfPieces[pawnToReplace]++;
		numberOfPieces[promoPiece]--;
	}
	
	public void updateWhenDoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		int pawnToRemove = (promoPiece & Piece.BLACK)+Piece.PAWN;
		numberOfPieces[pawnToRemove]--;
		numberOfPieces[promoPiece]++;
	}
	
	public void updateForCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]--;
	}
	
	public void updateForReplacedCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]++;
	}
}
