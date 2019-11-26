package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;

import com.fluxchess.jcpi.models.IntRank;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

public class Board implements Iterable<Piece> {

	private static final int INDEX_PAWN = 0;
	private static final int INDEX_KNIGHT = 1;
	private static final int INDEX_BISHOP = 2;
	private static final int INDEX_ROOK = 3;
	private static final int INDEX_QUEEN = 4;
	private static final int INDEX_KING = 5;
	
	private BitBoard allPieces = null;
	private BitBoard whitePieces = null;
	private BitBoard blackPieces = null;
	private BitBoard[] pieces = new BitBoard[6];
	
	public Board( List<Piece> pieceList ) {
		allPieces = new BitBoard();
		whitePieces = new BitBoard();
		blackPieces = new BitBoard();
		for (int i=0; i<=INDEX_KING; i++) {
			pieces[i] = new BitBoard();
		}
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
	
	public boolean squareIsAttacked( GenericPosition atPos, Piece.Colour ownColour ) {
		return SquareAttackEvaluator.isAttacked(this, atPos, ownColour);
	}
	
	public Piece getPieceAtSquare( GenericPosition atPos ) {
		Piece piece = null;
		RankAndFile rnf = new RankAndFile(atPos);
		if (allPieces.isSet(rnf.rank, rnf.file)) {
			piece = createPiece(atPos, false);
		}
		return piece;
	}
	
	private Piece createPiece(GenericPosition atPos, boolean remove) {
		RankAndFile rnf = new RankAndFile(atPos);
		Piece.Colour col = Colour.white;
		if (blackPieces.isSet(rnf.rank, rnf.file)) {
			col = Colour.black;
		}
		Piece piece = null;
		if (pieces[INDEX_KING].isSet(rnf.rank, rnf.file)) {
			if (remove) {
				pieces[INDEX_KING].clear(rnf.rank, rnf.file);
			}
			piece = new King(col, atPos);
		} else if (pieces[INDEX_QUEEN].isSet(rnf.rank, rnf.file)) {
			if (remove) {
				pieces[INDEX_QUEEN].clear(rnf.rank, rnf.file);
			}
			piece = new Queen(col, atPos);
		} else if (pieces[INDEX_ROOK].isSet(rnf.rank, rnf.file)) {
			if (remove) {
				pieces[INDEX_ROOK].clear(rnf.rank, rnf.file);
			}
			piece = new Rook(col, atPos);
		} else if (pieces[INDEX_BISHOP].isSet(rnf.rank, rnf.file)) {
			if (remove) {
				pieces[INDEX_BISHOP].clear(rnf.rank, rnf.file);
			}
			piece = new Bishop(col, atPos);
		} else if (pieces[INDEX_KNIGHT].isSet(rnf.rank, rnf.file)) {
			if (remove) {
				pieces[INDEX_KNIGHT].clear(rnf.rank, rnf.file);
			}
			piece = new Knight(col, atPos);
		} else if (pieces[INDEX_PAWN].isSet(rnf.rank, rnf.file)) {
			if (remove) {
				pieces[INDEX_PAWN].clear(rnf.rank, rnf.file);
			}
			piece = new Pawn(col, atPos);
		}
		if (piece != null && remove) {
			allPieces.clear(rnf.rank, rnf.file);
			getBitBoardForColour(piece).clear(rnf.rank, rnf.file);
		}
		return piece;
	}
	
	public void setPieceAtSquare( Piece pieceToPlace ) {
		GenericPosition atPos = pieceToPlace.getSquare();
		RankAndFile rnf = new RankAndFile(atPos);
		if (pieceToPlace instanceof King) {
			pieces[INDEX_KING].set(rnf.rank, rnf.file);
		} else if (pieceToPlace instanceof Queen) {
			pieces[INDEX_QUEEN].set(rnf.rank, rnf.file);
		} else if (pieceToPlace instanceof Rook) {
			pieces[INDEX_ROOK].set(rnf.rank, rnf.file);
		} else if (pieceToPlace instanceof Bishop) {
			pieces[INDEX_BISHOP].set(rnf.rank, rnf.file);
		} else if (pieceToPlace instanceof Knight) {
			pieces[INDEX_KNIGHT].set(rnf.rank, rnf.file);
		} else if (pieceToPlace instanceof Pawn) {
			pieces[INDEX_PAWN].set(rnf.rank, rnf.file);
		} else {
			assert false;
		}
		allPieces.set(rnf.rank, rnf.file);
		getBitBoardForColour(pieceToPlace).set(rnf.rank, rnf.file);
	}
	
	public King getKing(Piece.Colour kingToGet) {
		King king = null;
		BitBoard getFromBoard = null;
		if (kingToGet.equals(Colour.white)) {
			getFromBoard = whitePieces;
		} else {
			getFromBoard = blackPieces;
		}
		BitBoard temp = getFromBoard.and(pieces[INDEX_KING]);
		for (int bit_index: temp) {
			int file = bit_index%8;
			int rank = bit_index/8;
			king = new King(kingToGet, GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
		}
		return king;
	}

	public Piece pickUpPieceAtSquare( GenericPosition atPos ) throws InvalidPieceException {
		Piece pieceToPickUp = getPieceAndRemoveFromBoard(atPos);
		if (pieceToPickUp == null ) throw new InvalidPieceException(atPos);
		return pieceToPickUp;
	}
	
	public Piece captureAtSquare( GenericPosition atPos ) {
		return getPieceAndRemoveFromBoard(atPos);	
	}	
	
	private Piece getPieceAndRemoveFromBoard( GenericPosition atPos ) {
		return createPiece(atPos, true);	
	}
	
	public boolean checkIfOpposingPawnInFile(GenericFile file, GenericRank rank, Colour side) {
		boolean opposingPawnPresentInFile = false;
		int r = IntRank.valueOf(rank);
		int f = IntFile.valueOf(file);
		if (side == Colour.white) {
			for (r=r+1; r < 7; r++) {
				if (isOpposingPawn(side, r, f))
					opposingPawnPresentInFile = true;
			}
		} else {
			for (r=r-1; r > 0; r--) {
				if (isOpposingPawn(side, r, f))
					opposingPawnPresentInFile = true;	
			}			
		}
		return opposingPawnPresentInFile;
	}
	
	private boolean isOpposingPawn(Colour ownSide, int rank, int file) {
		boolean isPawn = pieces[INDEX_PAWN].isSet(rank, file);
		if (isPawn) {
			boolean enemyPawn = false;
			if (ownSide.equals(Colour.white)) {
				if (blackPieces.isSet(rank, file)) {
					enemyPawn = true;
				}
			} else {
				if (whitePieces.isSet(rank, file)) {
					enemyPawn = true;
				}
			}
			return enemyPawn;
		}
		return false;
	}
	
	private class RankAndFile {
		public int rank = IntRank.NORANK;
		public int file = IntFile.NOFILE;
		
		public RankAndFile(GenericPosition atPos) {
			rank = IntRank.valueOf(atPos.rank);
			file = IntFile.valueOf(atPos.file);	
		}
	}
	
	public String getAsFenString() {
		Piece currPiece = null;
		int spaceCounter = 0;
		StringBuilder fen = new StringBuilder();
		for (int rank=7; rank>=0; rank--) {
			for (int file=0; file<8; file++) {
				currPiece = createPiece(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)), false);
				if (currPiece != null) {
					if (spaceCounter != 0)
						fen.append(spaceCounter);
					fen.append(getFenChar(currPiece));
					spaceCounter=0;					
				} else {
					spaceCounter++;
				}
			}
			if (spaceCounter != 0)
				fen.append(spaceCounter);
			if (rank != 0)
				fen.append('/');
			spaceCounter=0;
		}
		return fen.toString();
	}
	
	private char getFenChar(Piece piece) {
		char chessman = 0;
		if (piece instanceof Pawn)
			chessman = 'P';
		else if (piece instanceof Knight)
			chessman = 'N';
		else if (piece instanceof Bishop)
			chessman = 'B';
		else if (piece instanceof Rook)
			chessman = 'R';
		else if (piece instanceof Queen)
			chessman = 'Q';
		else if (piece instanceof King)
			chessman = 'K';
		if (piece.isBlack())
			chessman += 32;
		return chessman;
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
			for (int bit_index: bitBoardToIterate) {
				int file = bit_index%8;
				int rank = bit_index/8;
				iterList.add(createPiece(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)), false));
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
