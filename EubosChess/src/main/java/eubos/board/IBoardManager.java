package eubos.board;

import com.fluxchess.jcpi.models.GenericMove;

public interface IBoardManager {
	public void performMove( GenericMove move ) throws InvalidPieceException;
	public void undoPreviousMove() throws InvalidPieceException;
	public GenericMove getPreviousMove();
}
