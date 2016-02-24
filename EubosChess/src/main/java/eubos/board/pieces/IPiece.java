package eubos.board.pieces;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;

public interface IPiece {
	List<GenericMove> generateMoves( Board theBoard );
}
