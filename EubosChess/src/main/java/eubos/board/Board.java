package eubos.board;

import java.util.*;

import eubos.pieces.*;

import com.fluxchess.jcpi.models.*;

public class Board implements Iterable<Piece> {
	
	private Piece[][] theBoard = new Piece[8][8];	
	
	public Board() { setupNewGame(); }

	// This constructor is primarily used for setting up unit tests...
	public Board( LinkedList<Piece> pieceList ) {
		for ( Piece nextPiece : pieceList ) {
			setPieceAtSquare( nextPiece );
		}
	}
	
	public static boolean moveIsOffBoard(GenericPosition currTargetSq, Direction dir ) {
		if (currTargetSq.file != GenericFile.Fa && currTargetSq.rank != GenericRank.R1)
			return true;
		else
			return false;
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

	private void setupNewGame() {
		setupBackRanks();
		setupPawns();
	}
	
	private void setupPawns() {
		GenericPosition[] allFiles = new GenericPosition[] { 
				GenericPosition.a2,
				GenericPosition.b2,
				GenericPosition.c2,
				GenericPosition.d2,
				GenericPosition.e2,
				GenericPosition.f2,
				GenericPosition.g2,
				GenericPosition.h2 };
		for ( GenericPosition startPos : allFiles ) {
			setPieceAtSquare( new Pawn( Piece.Colour.white, startPos ));
		}
		allFiles = new GenericPosition[] { 
				GenericPosition.a7,
				GenericPosition.b7,
				GenericPosition.c7,
				GenericPosition.d7,
				GenericPosition.e7,
				GenericPosition.f7,
				GenericPosition.g7,
				GenericPosition.h7 };		
		for ( GenericPosition startPos : allFiles ) {
			setPieceAtSquare( new Pawn( Piece.Colour.black, startPos ));
		}
	}

	private void setupBackRanks() {
		// White
		setPieceAtSquare( new Rook  ( Piece.Colour.white, GenericPosition.a1 ));
		setPieceAtSquare( new Knight( Piece.Colour.white, GenericPosition.b1 ));
		setPieceAtSquare( new Bishop( Piece.Colour.white, GenericPosition.c1 ));
		setPieceAtSquare( new Queen ( Piece.Colour.white, GenericPosition.d1 ));
		setPieceAtSquare( new King  ( Piece.Colour.white, GenericPosition.e1 ));
		setPieceAtSquare( new Bishop( Piece.Colour.white, GenericPosition.f1 ));
		setPieceAtSquare( new Knight( Piece.Colour.white, GenericPosition.g1 ));
		setPieceAtSquare( new Rook  ( Piece.Colour.white, GenericPosition.h1 ));
		// Black
		setPieceAtSquare( new Rook  ( Piece.Colour.black, GenericPosition.a8 ));
		setPieceAtSquare( new Knight( Piece.Colour.black, GenericPosition.b8 ));
		setPieceAtSquare( new Bishop( Piece.Colour.black, GenericPosition.c8 ));
		setPieceAtSquare( new Queen ( Piece.Colour.black, GenericPosition.d8 ));
		setPieceAtSquare( new King  ( Piece.Colour.black, GenericPosition.e8 ));
		setPieceAtSquare( new Bishop( Piece.Colour.black, GenericPosition.f8 ));
		setPieceAtSquare( new Knight( Piece.Colour.black, GenericPosition.g8 ));
		setPieceAtSquare( new Rook  ( Piece.Colour.black, GenericPosition.h8 ));
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
	}
	
    public Iterator<Piece> iterator() {
    	// default iterator returns all the pieces on the board
        return new allPiecesOnBoardIterator( );
    }
    
    public Iterator<Piece> iterateColour( Piece.Colour colourToIterate ) {
        return new allPiecesOnBoardIterator( colourToIterate );
    }
    
}
