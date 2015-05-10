package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import eubos.pieces.Piece;

import com.fluxchess.jcpi.models.IntRank;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericPosition;

public class Board implements Iterable<Piece> {

	private Piece[][] theBoard = new Piece[8][8];	

	public Board( List<Piece> pieceList ) {
		for ( Piece nextPiece : pieceList ) {
			setPieceAtSquare( nextPiece );
		}
	}

	public void setPieceAtSquare( Piece pieceToPlace ) {
		GenericPosition atPos = pieceToPlace.getSquare();
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		theBoard[file][rank] = pieceToPlace;	
	}

	public Piece getPieceAtSquare( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		return ( theBoard[file][rank] );
	}

	public Piece pickUpPieceAtSquare( GenericPosition atPos ) throws InvalidPieceException {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		Piece pieceToPickUp = theBoard[file][rank];
		if (pieceToPickUp == null ) throw new InvalidPieceException(atPos);
		theBoard[file][rank] = null;
		return ( pieceToPickUp );
	}

	public boolean squareIsEmpty( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		return ( theBoard[file][rank] == null );		
	}

	public class allPiecesOnBoardIterator implements Iterator<Piece> {

		private LinkedList<Piece> iterList = null;

		public allPiecesOnBoardIterator() {
			iterList = new LinkedList<Piece>();
			for (int i: IntFile.values) {
				for (int j: IntRank.values) {
					Piece nextPiece = theBoard[i][j];
					if (nextPiece != null ) {
						iterList.add(nextPiece);
					}
				}
			}
		}

		public allPiecesOnBoardIterator( Piece.Colour colourToIterate ) {
			iterList = new LinkedList<Piece>();
			for (int i: IntFile.values) {
				for (int j: IntRank.values) {
					Piece nextPiece = theBoard[i][j];
					if (nextPiece != null && ( nextPiece.getColour() == colourToIterate )) {
						iterList.add(nextPiece);
					}
				}
			}
		}		

		public boolean hasNext() {
			if (!iterList.isEmpty()) {
				return true;
			} else {
				return false;
			}
		}

		public Piece next() {
			return iterList.remove();
		}

		@Override
		public void remove() {
			iterList.remove();
		}
	}

	public Iterator<Piece> iterator() {
		// default iterator returns all the pieces on the board
		return new allPiecesOnBoardIterator( );
	}

	public Iterator<Piece> iterateColour( Piece.Colour colourToIterate ) {
		return new allPiecesOnBoardIterator( colourToIterate );
	}
}
