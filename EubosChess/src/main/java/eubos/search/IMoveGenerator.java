package eubos.search;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;

interface IMoveGenerator {
	public SearchResult findMove() throws NoLegalMoveException, InvalidPieceException;
	public SearchResult findMove(int searchDepth) throws NoLegalMoveException, InvalidPieceException;
	public SearchResult findMove(int searchDepth, LinkedList<GenericMove> lastPc) throws NoLegalMoveException, InvalidPieceException;
}
