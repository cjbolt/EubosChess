package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;

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
	
	private BitBoard allPieces = new BitBoard();
	private BitBoard whitePieces = new BitBoard();
	private BitBoard blackPieces = new BitBoard(); 

	public void setPieceAtSquare( Piece pieceToPlace ) {
		GenericPosition atPos = pieceToPlace.getSquare();
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		theBoard[file][rank] = pieceToPlace;
		// Update bit boards
		allPieces.set(rank, file);
		BitBoard bitBoardToIterate;
		if (pieceToPlace.isWhite()) {
			bitBoardToIterate = whitePieces;
		} else {
			bitBoardToIterate = blackPieces;
		}
		bitBoardToIterate.set(rank, file);
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
		// Update bit boards
		allPieces.clear(rank, file);
		BitBoard bitBoardToIterate;
		if (pieceToPickUp.isWhite()) {
			bitBoardToIterate = whitePieces;
		} else {
			bitBoardToIterate = blackPieces;
		}
		bitBoardToIterate.clear(rank, file);
		return ( pieceToPickUp );
	}

	public boolean squareIsEmpty( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		return !allPieces.isSet(rank, file);		
	}
	
	public Piece captureAtSquare( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		Piece capturedPiece = theBoard[file][rank];
		// Update bit boards
		allPieces.clear(rank, file);
		if (capturedPiece != null) {
			BitBoard bitBoardToIterate;
			if (capturedPiece.isWhite()) {
				bitBoardToIterate = whitePieces;
			} else {
				bitBoardToIterate = blackPieces;
			}
			bitBoardToIterate.clear(rank, file);
		}
		return ( capturedPiece );		
	}

	public class allPiecesOnBoardIterator implements Iterator<Piece> {

		private LinkedList<Piece> iterList = null;

		public allPiecesOnBoardIterator() throws InvalidPieceException {
			iterList = new LinkedList<Piece>();
			for (Integer bit_index: allPieces) {
				int file = bit_index%8;
				int rank = bit_index/8;
				Piece nextPiece = theBoard[file][rank];
				if (nextPiece != null ) {
					iterList.add(nextPiece);
				} else {
					throw new InvalidPieceException(GenericPosition.a1);
				}
			}
		}

		public allPiecesOnBoardIterator( Piece.Colour colourToIterate ) throws InvalidPieceException {
			iterList = new LinkedList<Piece>();
			BitBoard bitBoardToIterate;
			if (colourToIterate == Colour.white) {
				bitBoardToIterate = whitePieces;
			} else {
				bitBoardToIterate = blackPieces;
			}
			for (Integer bit_index: bitBoardToIterate) {
				int file = bit_index%8;
				int rank = bit_index/8;
				Piece nextPiece = theBoard[file][rank];
				if (nextPiece != null) {
					iterList.add(nextPiece);
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
		try {
			return new allPiecesOnBoardIterator( );
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public Iterator<Piece> iterateColour( Piece.Colour colourToIterate ) {
		try {
			return new allPiecesOnBoardIterator( colourToIterate );
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
