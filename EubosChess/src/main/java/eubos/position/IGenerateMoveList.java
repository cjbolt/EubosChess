package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

public interface IGenerateMoveList {
	public List<GenericMove> getMoveList() throws InvalidPieceException;
	public List<GenericMove> getMoveList(GenericMove prevBest) throws InvalidPieceException;
}
