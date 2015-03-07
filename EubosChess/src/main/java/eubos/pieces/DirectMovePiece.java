package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.BoardManager;

public abstract class DirectMovePiece extends Piece {
	public abstract LinkedList<GenericMove> generateMoveList(BoardManager bm);
}
