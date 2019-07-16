package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

public interface IChangePosition {
	public void performMove( ZobristHashCode hash, GenericMove move ) throws InvalidPieceException;
	public void unperformMove( ZobristHashCode hash ) throws InvalidPieceException;
}
