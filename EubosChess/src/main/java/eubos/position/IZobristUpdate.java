package eubos.position;

public interface IZobristUpdate {
	public void doBasicMove(int targetSquare, int originSquare, int piece);
	public void doPromotionMove(int targetSquare, int originSquare, int piece, int promotedPiece);
	public void doCapturedPiece(int capturedPieceSquare, int targetPiece);
}
