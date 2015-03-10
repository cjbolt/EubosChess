package eubos.board;

import com.fluxchess.jcpi.models.GenericMove;

public interface IMoveGenerator {
	public GenericMove findBestMove() throws NoLegalMoveException;
}
