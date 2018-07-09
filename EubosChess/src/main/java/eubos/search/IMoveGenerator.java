package eubos.search;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.InvalidPieceException;

interface IMoveGenerator {
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException;
}
