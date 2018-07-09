package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

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
	public void setEnPassantTargetSq(GenericPosition enPassantTargetSq) {
		// TODO: add bounds checking - only certain en passant squares can be legal.
		this.enPassantTargetSq = enPassantTargetSq;
	}
	
	public boolean squareIsEmpty( GenericPosition atPos ) {
		RankAndFile rnf = new RankAndFile(atPos);
		return !allPieces.isSet(rnf.rank, rnf.file);		
	}
	
	public Piece getPieceAtSquare( GenericPosition atPos ) {
		RankAndFile rnf = new RankAndFile(atPos);
		return ( theBoard[rnf.file][rnf.rank] );
	}
	
	public void setPieceAtSquare( Piece pieceToPlace ) {
		GenericPosition atPos = pieceToPlace.getSquare();
		RankAndFile rnf = new RankAndFile(atPos);
		theBoard[rnf.file][rnf.rank] = pieceToPlace;
		allPieces.set(rnf.rank, rnf.file);
		getBitBoardForColour(pieceToPlace).set(rnf.rank, rnf.file);
	}

	public Piece pickUpPieceAtSquare( GenericPosition atPos ) throws InvalidPieceException {
		Piece pieceToPickUp = getPieceAndRemoveFromBoard(atPos);
		if (pieceToPickUp == null ) throw new InvalidPieceException(atPos);
		return ( pieceToPickUp );
	}
	
	public Piece captureAtSquare( GenericPosition atPos ) {
		return getPieceAndRemoveFromBoard(atPos);	
	}	
	
	private Piece getPieceAndRemoveFromBoard( GenericPosition atPos ) {
		RankAndFile rnf = new RankAndFile(atPos);
		Piece pieceToGet = theBoard[rnf.file][rnf.rank];
		if (pieceToGet != null) {
			theBoard[rnf.file][rnf.rank] = null;
			allPieces.clear(rnf.rank, rnf.file);
			getBitBoardForColour(pieceToGet).clear(rnf.rank, rnf.file);
		}
		return ( pieceToGet );	
	}
	
	private class RankAndFile {
		public int rank = IntRank.NORANK;
		public int file = IntFile.NOFILE;
		
		public RankAndFile(GenericPosition atPos) {
			rank = IntRank.valueOf(atPos.rank);
			file = IntFile.valueOf(atPos.file);	
		}
	}
	
	private BitBoard allPieces = new BitBoard();
	private BitBoard whitePieces = new BitBoard();
	private BitBoard blackPieces = new BitBoard();
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
