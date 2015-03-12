package eubos.pieces;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

public abstract class SinglesquareDirectMovePiece extends DirectMovePiece {

	protected GenericPosition getDownLeft() {
		if ( onSquare.file != GenericFile.Fa && onSquare.rank != GenericRank.R1 ) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev());
		}
		return null;
	}

	protected GenericPosition getUpRight() {
		if ( onSquare.file != GenericFile.Fh && onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.next());	
		}
		return null;
	}
	
	protected GenericPosition getUpLeft() {
		if ( onSquare.file != GenericFile.Fa && onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.next());
		}
		return null;
	}
	
	protected GenericPosition getDownRight() {
		if ( onSquare.file != GenericFile.Fh && onSquare.rank != GenericRank.R1 ) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev());
		}
		return null;
	}

	protected GenericPosition getDown() {
		if ( onSquare.rank != GenericRank.R1 ) {
			return GenericPosition.valueOf( onSquare.file, onSquare.rank.prev());
		}
		return null;
	}	
	
	protected GenericPosition getUp() {
		if ( onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file, onSquare.rank.next());
		}
		return null;
	}	

	protected GenericPosition getLeft() {
		if ( onSquare.file != GenericFile.Fa ) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank);
		}
		return null;
	}	
	
	protected GenericPosition getRight() {
		if ( onSquare.file != GenericFile.Fh ) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank);
		}
		return null;
	}	
	
}
