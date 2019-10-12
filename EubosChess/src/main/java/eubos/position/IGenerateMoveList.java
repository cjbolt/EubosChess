package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

public interface IGenerateMoveList {
	public MoveList getMoveList() throws InvalidPieceException;
	public MoveList getMoveList(GenericMove prevBest) throws InvalidPieceException;
}
