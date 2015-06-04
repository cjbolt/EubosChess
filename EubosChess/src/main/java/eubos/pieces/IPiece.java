package eubos.pieces;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.BoardManager;

public interface IPiece {
	List<GenericMove> generateMoves( BoardManager bm );
}
