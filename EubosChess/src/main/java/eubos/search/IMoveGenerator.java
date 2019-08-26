package eubos.search;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

interface IMoveGenerator {
	public SearchResult findMove() throws NoLegalMoveException, InvalidPieceException;
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException, InvalidPieceException;
	public SearchResult findMove(byte searchDepth, List<GenericMove> lastPc) throws NoLegalMoveException, InvalidPieceException;
}
