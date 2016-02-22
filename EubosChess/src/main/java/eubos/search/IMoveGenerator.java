package eubos.search;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

interface IMoveGenerator {
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException;
}
