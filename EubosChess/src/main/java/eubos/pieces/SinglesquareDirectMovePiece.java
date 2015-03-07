package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.BoardManager;

public abstract class SinglesquareDirectMovePiece extends DirectMovePiece {
	public abstract LinkedList<GenericMove> generateMoveList(BoardManager bm);

	protected GenericPosition downLeft() {
		if ( onSquare.file != GenericFile.Fa && onSquare.rank != GenericRank.R1 ) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev());
		}
		return null;
	}

	protected GenericPosition upRight() {
		if ( onSquare.file != GenericFile.Fh && onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.next());	
		}
		return null;
	}
	
	protected GenericPosition upLeft() {
		if ( onSquare.file != GenericFile.Fa && onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.next());
		}
		return null;
	}
	
	protected GenericPosition downRight() {
		if ( onSquare.file != GenericFile.Fh && onSquare.rank != GenericRank.R1 ) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev());
		}
		return null;
	}

	protected GenericPosition down() {
		if ( onSquare.rank != GenericRank.R1 ) {
			return GenericPosition.valueOf( onSquare.file, onSquare.rank.prev());
		}
		return null;
	}	
	
	protected GenericPosition up() {
		if ( onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file, onSquare.rank.next());
		}
		return null;
	}	

	protected GenericPosition left() {
		if ( onSquare.file != GenericFile.Fa ) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank);
		}
		return null;
	}	
	
	protected GenericPosition right() {
		if ( onSquare.file != GenericFile.Fh ) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank);
		}
		return null;
	}	
	
}
