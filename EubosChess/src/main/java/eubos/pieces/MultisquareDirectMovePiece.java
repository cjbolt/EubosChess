package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.BoardManager;

public abstract class MultisquareDirectMovePiece extends DirectMovePiece {
	public abstract LinkedList<GenericMove> generateMoveList(BoardManager bm);
}
