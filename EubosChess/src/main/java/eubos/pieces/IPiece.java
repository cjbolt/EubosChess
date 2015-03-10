package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.BoardManager;

public interface IPiece {
	public LinkedList<GenericMove> generateMoves( BoardManager bm );
}
