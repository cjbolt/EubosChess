package eubos.position;

import eubos.board.InvalidPieceException;

public interface IChangePosition {
	public void performMove( int move ) throws InvalidPieceException;
	public void performMove( int move, boolean computeHash ) throws InvalidPieceException;
	public void unperformMove() throws InvalidPieceException;
	public void unperformMove( boolean computeHash ) throws InvalidPieceException;
}
