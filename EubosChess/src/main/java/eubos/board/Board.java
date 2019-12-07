package eubos.board;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eubos.board.Piece.Colour;
import eubos.board.Piece.PieceType;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntRank;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

public class Board implements Iterable<GenericPosition> {

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
	
	public Board( Map<GenericPosition, PieceType> pieceMap ) {
		allPieces = new BitBoard();
		whitePieces = new BitBoard();
		blackPieces = new BitBoard();
		for (int i=0; i<=INDEX_KING; i++) {
			pieces[i] = new BitBoard();
		}
		for ( Entry<GenericPosition, PieceType> nextPiece : pieceMap.entrySet() ) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue() );
		}
	}
	
	public List<GenericMove> getRegularPieceMoves(Piece.Colour side) {
		BitBoard bitBoardToIterate = Colour.isWhite(side) ? whitePieces : blackPieces;
		ArrayList<GenericMove> movesList = new ArrayList<GenericMove>();
		for (int bit_index: bitBoardToIterate) {
			GenericPosition atSquare = BitBoard.bitToPosition_Lut[bit_index];
			BitBoard pieceToPickUp = new BitBoard(1L<<bit_index);
			if (blackPieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					movesList.addAll(Piece.king_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					movesList.addAll(Piece.queen_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					movesList.addAll(Piece.rook_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					movesList.addAll(Piece.bishop_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					movesList.addAll(Piece.knight_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					movesList.addAll(Piece.pawn_generateMoves(this, atSquare, Colour.black));
				}
			} else if (whitePieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					movesList.addAll(Piece.king_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					movesList.addAll(Piece.queen_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					movesList.addAll(Piece.rook_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					movesList.addAll(Piece.bishop_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					movesList.addAll(Piece.knight_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					movesList.addAll(Piece.pawn_generateMoves(this, atSquare, Colour.white));
				}
			} else {
				assert false;
			}
		}
		return movesList;
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
		return !allPieces.isSet(BitBoard.positionToBit_Lut.get(atPos));		
	}
	
	public boolean squareIsAttacked( GenericPosition atPos, Piece.Colour ownColour ) {
		return SquareAttackEvaluator.isAttacked(this, atPos, ownColour);
	}
	
	public PieceType getPieceAtSquare( GenericPosition atPos ) {
		// Calculate bit index
		PieceType type = PieceType.NONE;
		int bit_index = BitBoard.positionToBit_Lut.get(atPos);
		BitBoard pieceToPickUp = new BitBoard(1L<<bit_index);
		if (allPieces.and(pieceToPickUp).isNonZero()) {	
			if (blackPieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					type = PieceType.BlackKing;
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					type = PieceType.BlackQueen;
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					type = PieceType.BlackRook;
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					type = PieceType.BlackBishop;
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					type = PieceType.BlackKnight;
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					type = PieceType.BlackPawn;
				}
			} else if (whitePieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					type = PieceType.WhiteKing;
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					type = PieceType.WhiteQueen;
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					type = PieceType.WhiteRook;
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					type = PieceType.WhiteBishop;
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					type = PieceType.WhiteKnight;
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					type = PieceType.WhitePawn;
				}
			} else {
				// can't get here
				assert false;
			}
		}
		return type;
	}
	
	public void setPieceAtSquare( GenericPosition atPos, PieceType pieceToPlace ) {
		assert pieceToPlace != PieceType.NONE;
		int bit_index = BitBoard.positionToBit_Lut.get(atPos);
		switch (pieceToPlace) {
		case WhiteKing:
			pieces[INDEX_KING].set(bit_index);
			whitePieces.set(bit_index);
			break;
		case WhiteQueen:
			pieces[INDEX_QUEEN].set(bit_index);
			whitePieces.set(bit_index);
			break;
		case WhiteRook:
			pieces[INDEX_ROOK].set(bit_index);
			whitePieces.set(bit_index);
			break;
		case WhiteBishop:
			pieces[INDEX_BISHOP].set(bit_index);
			whitePieces.set(bit_index);
			break;
		case WhiteKnight:
			pieces[INDEX_KNIGHT].set(bit_index);
			whitePieces.set(bit_index);
			break;
		case WhitePawn:
			pieces[INDEX_PAWN].set(bit_index);
			whitePieces.set(bit_index);
			break;
		case BlackKing:
			pieces[INDEX_KING].set(bit_index);
			blackPieces.set(bit_index);
			break;
		case BlackQueen:
			pieces[INDEX_QUEEN].set(bit_index);
			blackPieces.set(bit_index);
			break;
		case BlackRook:
			pieces[INDEX_ROOK].set(bit_index);
			blackPieces.set(bit_index);
			break;
		case BlackBishop:
			pieces[INDEX_BISHOP].set(bit_index);
			blackPieces.set(bit_index);
			break;
		case BlackKnight:
			pieces[INDEX_KNIGHT].set(bit_index);
			blackPieces.set(bit_index);
			break;
		case BlackPawn:
			pieces[INDEX_PAWN].set(bit_index);
			blackPieces.set(bit_index);
			break;
		case NONE:
		default:
			assert false;
			break;
		}
		allPieces.set(bit_index);
	}
	
	public boolean isKingInCheck(Piece.Colour side) {
		boolean inCheck = false;
		BitBoard getFromBoard = Colour.isWhite(side) ? whitePieces : blackPieces;
		BitBoard kingMask = getFromBoard.and(pieces[INDEX_KING]);
		if (kingMask.isNonZero()) {
			// The conditional is needed because some unit test positions don't have a king...
			GenericPosition kingSquare = BitBoard.maskToPosition_Lut.get(kingMask.getValue());
			inCheck = squareIsAttacked(kingSquare, side);
		}
		return inCheck;
	}

	public PieceType pickUpPieceAtSquare( GenericPosition atPos ) {
		PieceType type = PieceType.NONE;
		int bit_index = BitBoard.positionToBit_Lut.get(atPos);
		BitBoard pieceToPickUp = new BitBoard(1L<<bit_index);
		if (allPieces.and(pieceToPickUp).isNonZero()) {
			if (blackPieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					pieces[INDEX_KING].clear(bit_index);
					type = PieceType.BlackKing;
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					pieces[INDEX_QUEEN].clear(bit_index);
					type = PieceType.BlackQueen;
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					pieces[INDEX_ROOK].clear(bit_index);
					type = PieceType.BlackRook;
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					pieces[INDEX_BISHOP].clear(bit_index);
					type = PieceType.BlackBishop;
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					pieces[INDEX_KNIGHT].clear(bit_index);
					type = PieceType.BlackKnight;
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					pieces[INDEX_PAWN].clear(bit_index);
					type = PieceType.BlackPawn;
				}
				blackPieces.clear(bit_index);
			} else {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					pieces[INDEX_KING].clear(bit_index);
					type = PieceType.WhiteKing;
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					pieces[INDEX_QUEEN].clear(bit_index);
					type = PieceType.WhiteQueen;
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					pieces[INDEX_ROOK].clear(bit_index);
					type = PieceType.WhiteRook;
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					pieces[INDEX_BISHOP].clear(bit_index);
					type = PieceType.WhiteBishop;
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					pieces[INDEX_KNIGHT].clear(bit_index);
					type = PieceType.WhiteKnight;
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					pieces[INDEX_PAWN].clear(bit_index);
					type = PieceType.WhitePawn;
				}
				whitePieces.clear(bit_index);
			}
			allPieces.clear(bit_index);
		}
		return type;
	}
	
	private static final Map<GenericPosition, List<BitBoard>> DiagonalMask_Lut = new EnumMap<GenericPosition, List<BitBoard>>(GenericPosition.class);
	static {
		for (GenericPosition square : GenericPosition.values()) {
			List<BitBoard> array = new ArrayList<BitBoard>();
			for (int index=1; index<8; index++) {
				createDiagonalMask(square, array, index);
			}
			DiagonalMask_Lut.put(square, array);
		}
	}
	static private void createDiagonalMask(GenericPosition square, List<BitBoard> array, int index) {
		Long currMask = 0L;
		Direction [] diagonals = { Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
		for (Direction dir: diagonals) {
			currMask = setAllInDirection(dir, square, currMask, index);
		}
		// Only add the mask if it isn't the same as previous (i.e. no more squares to add)
		if (array.size()-1 >= 0) {
			if (currMask != array.get(array.size()-1).getValue())
				array.add(new BitBoard(currMask));
		} else {
			array.add(new BitBoard(currMask));
		}
	}
	static private Long setAllInDirection(Direction dir, GenericPosition fromSq, Long currMask, int index) {
		GenericPosition newSquare = fromSq;
		for (int i=0; i < index; i++) {
			if (newSquare != null)
				newSquare = Direction.getDirectMoveSq(dir, newSquare);
			if (newSquare != null)
				currMask |= BitBoard.positionToMask_Lut.get(newSquare).getValue();
		}
		return currMask;
	}
	
	
	private static final Map<GenericFile, BitBoard> FileMask_Lut = new EnumMap<GenericFile, BitBoard>(GenericFile.class);
	static {
		for (GenericFile file : GenericFile.values()) {
			long mask = 0;
			int f=IntFile.valueOf(file);
			for (int r = 0; r<8; r++) {
				mask  |= 1L << r*8+f;
			}
			FileMask_Lut.put(file, new BitBoard(mask));
		}
	}
	
	public int countDoubledPawnsForSide(Colour side) {
		int doubledCount = 0;
		BitBoard pawns = Colour.isWhite(side) ? getWhitePawns() : getBlackPawns();
		for (GenericFile file : GenericFile.values()) {
			BitBoard mask = FileMask_Lut.get(file);
			long fileMask = pawns.and(mask).getValue();
			int numPawnsInFile = Long.bitCount(fileMask);
			if (numPawnsInFile > 1) {
				doubledCount += numPawnsInFile-1;
			}
		}
		return doubledCount;
	}
	
	public boolean isPassedPawn(GenericPosition atPos, Colour side) {
		boolean isPassed = true;
		BitBoard mask = PassedPawn_Lut.get(side.ordinal()).get(atPos);
		BitBoard otherSidePawns = Colour.isWhite(side) ? getBlackPawns() : getWhitePawns();
		if (mask.and(otherSidePawns).isNonZero()) {
			isPassed  = false;
		}
		return isPassed;
	}
	
	private static final List<Map<GenericPosition, BitBoard>> PassedPawn_Lut = new ArrayList<Map<GenericPosition, BitBoard>>(2); 
	static {
		Map<GenericPosition, BitBoard> white_map = new EnumMap<GenericPosition, BitBoard>(GenericPosition.class);
		PassedPawn_Lut.add(Colour.white.ordinal(), white_map);
		for (GenericPosition atPos : GenericPosition.values()) {
			white_map.put(atPos, buildPassedPawnFileMask(atPos.file, atPos.rank, true));
		}
		Map<GenericPosition, BitBoard> black_map = new EnumMap<GenericPosition, BitBoard>(GenericPosition.class);
		PassedPawn_Lut.add(Colour.black.ordinal(), black_map);
		for (GenericPosition atPos : GenericPosition.values()) {
			black_map.put(atPos, buildPassedPawnFileMask(atPos.file, atPos.rank, false));
		}
	}
	private static BitBoard buildPassedPawnFileMask(GenericFile file, GenericRank rank, boolean isWhite) {
		long mask = 0;
		int r = IntRank.valueOf(rank);
		int f = IntFile.valueOf(file);
		boolean hasPrevFile = file.hasPrev();
		boolean hasNextFile = file.hasNext();
		if (isWhite) {
			for (r=r+1; r < 7; r++) {
				mask = addRankForPassedPawnMask(mask, r, f, hasPrevFile,
						hasNextFile);
			}
		} else {
			for (r=r-1; r > 0; r--) {
				mask = addRankForPassedPawnMask(mask, r, f, hasPrevFile,
						hasNextFile);	
			}
		}
		return new BitBoard(mask);
	}
	private static long addRankForPassedPawnMask(long mask, int r, int f,
			boolean hasPrevFile, boolean hasNextFile) {
		if (hasPrevFile) {
			mask |= 1L << r*8+(f-1);
		}
		mask |= 1L << r*8+f;
		if (hasNextFile) {
			mask |= 1L << r*8+(f+1);
		}
		return mask;
	}
	
	public String getAsFenString() {
		PieceType currPiece = null;
		int spaceCounter = 0;
		StringBuilder fen = new StringBuilder();
		for (int rank=7; rank>=0; rank--) {
			for (int file=0; file<8; file++) {
				currPiece = this.getPieceAtSquare(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
				if (currPiece != PieceType.NONE) {
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
	
	private char getFenChar(PieceType piece) {
		char chessman = 0;
		if (piece==PieceType.WhitePawn)
			chessman = 'P';
		else if (piece==PieceType.WhiteKnight)
			chessman = 'N';
		else if (piece==PieceType.WhiteBishop)
			chessman = 'B';
		else if (piece==PieceType.WhiteRook)
			chessman = 'R';
		else if (piece==PieceType.WhiteQueen)
			chessman = 'Q';
		else if (piece==PieceType.WhiteKing)
			chessman = 'K';
		else if (piece==PieceType.BlackPawn)
			chessman = 'p';
		else if (piece==PieceType.BlackKnight)
			chessman = 'n';
		else if (piece==PieceType.BlackBishop)
			chessman = 'b';
		else if (piece==PieceType.BlackRook)
			chessman = 'r';
		else if (piece==PieceType.BlackQueen)
			chessman = 'q';
		else if (piece==PieceType.BlackKing)
			chessman = 'k';
		return chessman;
	}
	
	class allPiecesOnBoardIterator implements Iterator<GenericPosition> {

		private LinkedList<GenericPosition> iterList = null;

		allPiecesOnBoardIterator() throws InvalidPieceException {
			iterList = new LinkedList<GenericPosition>();
			buildIterList(allPieces);
		}

		allPiecesOnBoardIterator( Piece.Colour colourToIterate ) throws InvalidPieceException {
			iterList = new LinkedList<GenericPosition>();
			buildIterList(Colour.isWhite(colourToIterate) ? whitePieces : blackPieces);
		}
		
		allPiecesOnBoardIterator( PieceType typeToIterate ) throws InvalidPieceException {
			iterList = new LinkedList<GenericPosition>();
			BitBoard bitBoardToIterate;
			if (typeToIterate == PieceType.WhitePawn) {
				bitBoardToIterate = getWhitePawns();
			} else if (typeToIterate == PieceType.BlackPawn) {
				bitBoardToIterate = getBlackPawns();
			} else {
				bitBoardToIterate = new BitBoard();
			}
			buildIterList(bitBoardToIterate);
		}

		private void buildIterList(BitBoard bitBoardToIterate) {
			for (int bit_index: bitBoardToIterate) {
				iterList.add(BitBoard.bitToPosition_Lut[bit_index]);
			}
		}	

		public boolean hasNext() {
			if (!iterList.isEmpty()) {
				return true;
			} else {
				return false;
			}
		}

		public GenericPosition next() {
			return iterList.remove();
		}

		@Override
		public void remove() {
			iterList.remove();
		}
	}

	public Iterator<GenericPosition> iterator() {
		// default iterator returns all the pieces on the board
		try {
			return new allPiecesOnBoardIterator( );
		} catch (InvalidPieceException e) {
			return null;
		}
	}

	public Iterator<GenericPosition> iterateColour( Piece.Colour colourToIterate ) {
		try {
			return new allPiecesOnBoardIterator( colourToIterate );
		} catch (InvalidPieceException e) {
			return null;
		}
	}
	
	public BitBoard getMaskForType(PieceType type) {
		BitBoard mask = null;
		switch(type) {
		case WhiteKing:
			mask = getWhiteKing();
			break;
		case WhiteQueen:
			mask = getWhiteQueens();
			break;
		case WhiteRook:
			mask = getWhiteRooks();
			break;
		case WhiteBishop:
			mask = getWhiteBishops();
			break;
		case WhiteKnight:
			mask = getWhiteKnights();
			break;
		case WhitePawn:
			mask = getWhitePawns();
			break;
		case BlackKing:
			mask = getBlackKing();
			break;
		case BlackQueen:
			mask = getBlackQueens();
			break;
		case BlackRook:
			mask = getBlackRooks();
			break;
		case BlackBishop:
			mask = getBlackBishops();
			break;
		case BlackKnight:
			mask = getBlackKnights();
			break;
		case BlackPawn:
			mask = getBlackPawns();
			break;
		case NONE:
		default:
			assert false;
			break;
		}
		return mask;
	}
		
	public BitBoard getBlackPawns() {
		return blackPieces.and(pieces[INDEX_PAWN]);
	}
	
	public BitBoard getBlackKnights() {
		return blackPieces.and(pieces[INDEX_KNIGHT]);
	}
	
	public BitBoard getBlackBishops() {
		return blackPieces.and(pieces[INDEX_BISHOP]);
	}
	
	public BitBoard getBlackRooks() {
		return blackPieces.and(pieces[INDEX_ROOK]);
	}
	
	public BitBoard getBlackQueens() {
		return blackPieces.and(pieces[INDEX_QUEEN]);
	}
	
	public BitBoard getBlackKing() {
		return blackPieces.and(pieces[INDEX_KING]);
	}
	
	public BitBoard getWhitePawns() {
		return whitePieces.and(pieces[INDEX_PAWN]);
	}
	
	public BitBoard getWhiteBishops() {
		return whitePieces.and(pieces[INDEX_BISHOP]);
	}
	
	public BitBoard getWhiteRooks() {
		return whitePieces.and(pieces[INDEX_ROOK]);
	}
	
	public BitBoard getWhiteQueens() {
		return whitePieces.and(pieces[INDEX_QUEEN]);
	}
	
	public BitBoard getWhiteKnights() {
		return whitePieces.and(pieces[INDEX_KNIGHT]);
	}
	
	public BitBoard getWhiteKing() {
		return whitePieces.and(pieces[INDEX_KING]);
	}
	
	public Iterator<GenericPosition> iterateType( PieceType typeToIterate ) {
		try {
			return new allPiecesOnBoardIterator( typeToIterate );
		} catch (InvalidPieceException e) {
			return null;
		}
	}

	public boolean isOnOpenFile(GenericPosition atPos) {
		BitBoard fileMask = new BitBoard(FileMask_Lut.get(atPos.file).getValue());
		fileMask.clear(BitBoard.positionToBit_Lut.get(atPos));
		return allPieces.and(fileMask).getValue() == 0;
	}
	
	public int isOnOpenDiagonal(GenericPosition atPos) {
		int levelCount = 0;
		int bit = BitBoard.positionToBit_Lut.get(atPos);
		List<BitBoard> list = DiagonalMask_Lut.get(atPos);
		for (BitBoard levelMask : list) {
			if (checkSingleMask(bit, levelMask))
				levelCount++;
		}
		return levelCount;
	}

	private boolean checkSingleMask(int bit, BitBoard levelMask) {
		levelMask.clear(bit);
		return allPieces.and(levelMask).getValue() == 0;
	}
	
	public boolean isOnHalfOpenFile(GenericPosition atPos, PieceType type) {
		boolean isHalfOpen = false;
		BitBoard fileMask = new BitBoard(FileMask_Lut.get(atPos.file).getValue());
		BitBoard otherSide = PieceType.getOpposite(type) == Colour.white ? whitePieces : blackPieces;
		BitBoard pawnMask = otherSide.and(pieces[INDEX_PAWN]);
		boolean opponentPawnOnFile = pawnMask.and(fileMask).isNonZero();
		if (opponentPawnOnFile) {
			BitBoard ownSide = PieceType.isWhite(type) ? whitePieces : blackPieces;
			pawnMask = ownSide.and(pieces[INDEX_PAWN]);
			// and no pawns of own side
			isHalfOpen = !pawnMask.and(fileMask).isNonZero();
		}
		return isHalfOpen;
	}
}
