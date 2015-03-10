package eubos.board;

import com.fluxchess.jcpi.models.GenericMove;

public interface IBoardManager {
	public void performMove( GenericMove move );
	public void undoPreviousMove();
	public GenericMove getPreviousMove();
}
