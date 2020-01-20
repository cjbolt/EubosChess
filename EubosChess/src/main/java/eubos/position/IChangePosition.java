package eubos.position;

import eubos.board.InvalidPieceException;

public interface IChangePosition {
	public void performMove( int move ) throws InvalidPieceException;
	public void unperformMove() throws InvalidPieceException;
}
