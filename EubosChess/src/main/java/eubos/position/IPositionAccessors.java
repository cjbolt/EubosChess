package eubos.position;

import eubos.board.Board;
import eubos.board.Piece.Colour;
import eubos.score.IEvaluate;

public interface IPositionAccessors {
	public Board getTheBoard();
	public Colour getOnMove();
	public boolean onMoveIsWhite();
	public boolean isKingInCheck();
	public int getMoveNumber();
	public boolean lastMoveWasCapture();
	public boolean lastMoveWasCheck();
	public boolean lastMoveWasPromotion();
	public boolean isPromotionPossible();
	public CaptureData getCapturedPiece();
	public long getHash();
	public String getFen();
	public boolean isThreefoldRepetitionPossible();
	public void registerPositionEvaluator(IEvaluate pe);
}
