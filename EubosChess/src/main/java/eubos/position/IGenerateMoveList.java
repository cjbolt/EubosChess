package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;



public interface IGenerateMoveList {
	public MoveList getMoveList() ;
	public MoveList getMoveList(GenericMove prevBest) ;
}
