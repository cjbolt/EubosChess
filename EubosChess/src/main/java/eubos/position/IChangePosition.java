package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

public interface IChangePosition {
	public void performMove( GenericMove move ) throws InvalidPieceException;
	public void unperformMove() throws InvalidPieceException;
}
