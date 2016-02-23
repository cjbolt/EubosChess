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
	
	private GenericPosition enPassantTargetSq = null;
	public GenericPosition getEnPassantTargetSq() {
		return enPassantTargetSq;
	}
	void setEnPassantTargetSq(GenericPosition enPassantTargetSq) {
		this.enPassantTargetSq = enPassantTargetSq;
	}
	
	private BitBoard allPieces = new BitBoard();
	private BitBoard whitePieces = new BitBoard();
	private BitBoard blackPieces = new BitBoard(); 
	
	void setPieceAtSquare( Piece pieceToPlace ) {
		GenericPosition atPos = pieceToPlace.getSquare();
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		theBoard[file][rank] = pieceToPlace;
		// Update bit boards
		allPieces.set(rank, file);
		getBitBoardForColour(pieceToPlace).set(rank, file);
	}

	Piece pickUpPieceAtSquare( GenericPosition atPos ) throws InvalidPieceException {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		Piece pieceToPickUp = theBoard[file][rank];
		if (pieceToPickUp == null ) throw new InvalidPieceException(atPos);
		theBoard[file][rank] = null;
		// Update bit boards
		allPieces.clear(rank, file);
		getBitBoardForColour(pieceToPickUp).clear(rank, file);
		return ( pieceToPickUp );
	}

	Piece captureAtSquare( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		Piece capturedPiece = theBoard[file][rank];
		// Update bit boards
		allPieces.clear(rank, file);
		if (capturedPiece != null) {
			getBitBoardForColour(capturedPiece).clear(rank, file);
		}
		return ( capturedPiece );		
	}

	public boolean squareIsEmpty( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		return !allPieces.isSet(rank, file);		
	}
	
	public Piece getPieceAtSquare( GenericPosition atPos ) {
		int file, rank;
		file = IntFile.valueOf(atPos.file);
		rank = IntRank.valueOf(atPos.rank);
		return ( theBoard[file][rank] );
	}

	private BitBoard getBitBoardForColour(Piece pieceToPickUp) {
		BitBoard bitBoardForColour;
		if (pieceToPickUp.isWhite()) {
			bitBoardForColour = whitePieces;
		} else {
			bitBoardForColour = blackPieces;
		}
		return bitBoardForColour;
	}

	class allPiecesOnBoardIterator implements Iterator<Piece> {

		private LinkedList<Piece> iterList = null;

		allPiecesOnBoardIterator() throws InvalidPieceException {
			iterList = new LinkedList<Piece>();
			BitBoard bitBoardToIterate = allPieces;
			buildIterList(bitBoardToIterate);
		}

		allPiecesOnBoardIterator( Piece.Colour colourToIterate ) throws InvalidPieceException {
			iterList = new LinkedList<Piece>();
			BitBoard bitBoardToIterate;
			if (colourToIterate == Colour.white) {
				bitBoardToIterate = whitePieces;
			} else {
				bitBoardToIterate = blackPieces;
			}
			buildIterList(bitBoardToIterate);
		}

		private void buildIterList(BitBoard bitBoardToIterate) {
			for (Integer bit_index: bitBoardToIterate) {
				int file = bit_index%8;
				int rank = bit_index/8;
				iterList.add(theBoard[file][rank]);
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

	Iterator<Piece> iterateColour( Piece.Colour colourToIterate ) {
		try {
			return new allPiecesOnBoardIterator( colourToIterate );
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
