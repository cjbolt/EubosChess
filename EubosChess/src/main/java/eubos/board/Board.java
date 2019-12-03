package eubos.board;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Piece.PieceType;

import com.fluxchess.jcpi.models.GenericChessman;
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
		BitBoard bitBoardToIterate = (side == Colour.white) ? whitePieces : blackPieces;
		ArrayList<GenericMove> movesList = new ArrayList<GenericMove>();
		for (int bit_index: bitBoardToIterate) {
			GenericPosition atSquare = BitBoard.bitToPosition_Lut[bit_index];
			BitBoard pieceToPickUp = new BitBoard(1L<<bit_index);
			if (blackPieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					movesList.addAll(king_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					movesList.addAll(queen_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					movesList.addAll(rook_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					movesList.addAll(bishop_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					movesList.addAll(knight_generateMoves(this, atSquare, Colour.black));
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					movesList.addAll(pawn_generateMoves(this, atSquare, Colour.black));
				}
			} else if (whitePieces.and(pieceToPickUp).isNonZero()) {
				if (pieces[INDEX_KING].isSet(bit_index)) {
					movesList.addAll(king_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_QUEEN].isSet(bit_index)) {
					movesList.addAll(queen_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_ROOK].isSet(bit_index)) {
					movesList.addAll(rook_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_BISHOP].isSet(bit_index)) {
					movesList.addAll(bishop_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_KNIGHT].isSet(bit_index)) {
					movesList.addAll(knight_generateMoves(this, atSquare, Colour.white));
				} else if (pieces[INDEX_PAWN].isSet(bit_index)) {
					movesList.addAll(pawn_generateMoves(this, atSquare, Colour.white));
				}
			} else {
				assert false;
			}
		}
		return movesList;
	}
	
	private boolean isOppositeColour(Piece.Colour ownColour, PieceType toCheck) {
		boolean isOpposite = false;
		assert toCheck != PieceType.NONE;
		if (ownColour.equals(Colour.white)) {
			isOpposite = toCheck.ordinal() >= PieceType.BlackKing.ordinal();
		} else {
			isOpposite = toCheck.ordinal() < PieceType.BlackKing.ordinal();
		}
		return isOpposite;
	}
	
	private List<GenericMove> king_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.up, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.upRight, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.right, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.downRight, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.down, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.downLeft, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.left, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.upLeft, atSquare));
		return moveList;
	}

	private void king_checkAddMove(Piece.Colour ownSide, GenericPosition atSquare, List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if ( theBoard.squareIsEmpty(targetSquare) || 
					(targetPiece != PieceType.NONE && isOppositeColour(ownSide, targetPiece))) {
				moveList.add( new GenericMove( atSquare, targetSquare ) );
			}
		}
	}	
	
	private GenericPosition king_getOneSq( Direction dir, GenericPosition atSquare ) {
		return Direction.getDirectMoveSq(dir, atSquare);
	}
	
	private void knight_checkAddMove(Piece.Colour ownSide, GenericPosition atSquare, List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( new GenericMove( atSquare, targetSquare ));
			}
			else if (targetPiece != PieceType.NONE && isOppositeColour(ownSide, targetPiece)) {
				// Indicates a capture
				moveList.add( new GenericMove( atSquare, targetSquare ));
			}
			// Indicates blocked by own piece.
		}
	}
	
	public List<GenericMove> knight_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.upRight, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.upLeft, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.rightUp, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.rightDown, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.downRight, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.downLeft, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.leftUp, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.leftDown, atSquare));
		return moveList;		
	}
	
	
	public List<GenericMove> rook_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.down, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.up, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.left, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.right, theBoard));
		return moveList;	
	}
	
	public List<GenericMove> queen_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.down, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.up, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.left, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.right, theBoard));
		return moveList;	
	}
	
	public List<GenericMove> bishop_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upRight, theBoard));
		return moveList;	
	}
	
	
	private List<GenericPosition> multidirect_getAllSqs(GenericPosition atSquare, Direction dir, Board theBoard) {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = atSquare;
		while ((currTargetSq = Direction.getDirectMoveSq(dir, currTargetSq)) != null) {
			targetSquares.add(currTargetSq);
			if (multidirect_sqConstrainsAttack(theBoard, currTargetSq)) break;
		}
		return targetSquares;
	}
	
	private boolean multidirect_checkAddMove(Piece.Colour ownSide, GenericPosition atSquare, List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		boolean continueAddingMoves = false;
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( new GenericMove( atSquare, targetSquare ));
				continueAddingMoves = true;
			}
			else if (targetPiece != PieceType.NONE && isOppositeColour(ownSide, targetPiece)) {
				// Indicates a capture
				moveList.add( new GenericMove( atSquare, targetSquare ));
			}
			// Indicates blocked by own piece.
		}
		return continueAddingMoves;
	}
		
	private boolean multidirect_sqConstrainsAttack(Board theBoard, GenericPosition targetSquare) {
		boolean constrains = false;
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (targetPiece != PieceType.NONE) {
				constrains = true;
			}
		}
		return constrains;
	}	

	private void multidirect_addMoves(GenericPosition atSquare, Piece.Colour ownSide, List<GenericMove> moveList, Board theBoard, List<GenericPosition> targetSqs) {
		boolean continueAddingMoves = true;
		Iterator<GenericPosition> it = targetSqs.iterator();
		while ( it.hasNext() && continueAddingMoves ) {
			continueAddingMoves = multidirect_checkAddMove(ownSide, atSquare, moveList, theBoard, it.next());
		}
	}
	
	
	public boolean pawn_isAtInitialPosition(GenericPosition atSquare, Piece.Colour ownSide) {
		if ( ownSide.equals(Colour.black)) {
			return (atSquare.rank.equals( GenericRank.R7 ));
		} else {
			return (atSquare.rank.equals( GenericRank.R2 ));
		}
	}

	private GenericPosition pawn_genOneSqTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		if ( ownSide.equals(Colour.black) ) {
			return king_getOneSq(Direction.down, atSquare);
		} else {
			return king_getOneSq(Direction.up, atSquare);
		}
	}	
	
	private GenericPosition pawn_genTwoSqTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		GenericPosition moveTo = null;
		if ( pawn_isAtInitialPosition(atSquare, ownSide) ) {
			if ( ownSide.equals(Colour.black)) {
				moveTo = Direction.getDirectMoveSq(Direction.down, Direction.getDirectMoveSq(Direction.down, atSquare));
			} else {
				moveTo = Direction.getDirectMoveSq(Direction.up, Direction.getDirectMoveSq(Direction.up, atSquare));
			}
		}
		return moveTo;
	}
	
	private GenericPosition pawn_genLeftCaptureTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		if (ownSide.equals(Colour.black)) {
			return king_getOneSq(Direction.downRight, atSquare);
		} else {
			return king_getOneSq(Direction.upLeft, atSquare);
		}
	}
	
	private GenericPosition pawn_genRightCaptureTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		if (ownSide.equals(Colour.black)) {
			return king_getOneSq(Direction.downLeft, atSquare);
		} else {
			return king_getOneSq(Direction.upRight, atSquare);
		}		
	}
	
	private boolean pawn_isCapturable(Piece.Colour ownSide, Board theBoard, GenericPosition captureAt ) {
		boolean isCapturable = false;
		PieceType queryPiece = theBoard.getPieceAtSquare( captureAt );
		if ( queryPiece != PieceType.NONE ) {
			isCapturable = isOppositeColour( ownSide, queryPiece );
		} else if (captureAt == theBoard.getEnPassantTargetSq()) {
			isCapturable = true;
		}
		return isCapturable;
	}
	
	private boolean pawn_checkPromotionPossible(Piece.Colour ownSide, GenericPosition targetSquare ) {
		return (( ownSide.equals(Colour.black) && targetSquare.rank == GenericRank.R1 ) || 
				( ownSide.equals(Colour.white) && targetSquare.rank == GenericRank.R8 ));
	}
	
	private void pawn_checkPromotionAddMove(GenericPosition atSquare, Piece.Colour ownSide, List<GenericMove> moveList,
			GenericPosition targetSquare) {
		if ( pawn_checkPromotionPossible( ownSide, targetSquare )) {
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.KNIGHT ));
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.BISHOP ));
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.ROOK ));
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.QUEEN ));
		} else {
			moveList.add( new GenericMove( atSquare, targetSquare ) );
		}
	}	
	
	public List<GenericMove> pawn_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		// Check for standard one and two square moves
		GenericPosition moveTo = pawn_genOneSqTarget(atSquare, ownSide);
		if ( moveTo != null && theBoard.squareIsEmpty( moveTo )) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, moveTo);
			moveTo = pawn_genTwoSqTarget(atSquare, ownSide);
			if ( moveTo != null && theBoard.squareIsEmpty( moveTo )) {
				moveList.add( new GenericMove( atSquare, moveTo ) );
			}	
		}
		// Check for capture moves, includes en passant
		GenericPosition captureAt = pawn_genLeftCaptureTarget(atSquare, ownSide);
		if ( captureAt != null && pawn_isCapturable(ownSide, theBoard,captureAt)) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, captureAt);
		}
		captureAt = pawn_genRightCaptureTarget(atSquare, ownSide);
		if ( captureAt != null && pawn_isCapturable(ownSide, theBoard,captureAt)) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, captureAt);
		}
		return moveList;
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
		BitBoard getFromBoard = side.equals(Colour.white) ? whitePieces : blackPieces;
		BitBoard temp = getFromBoard.and(pieces[INDEX_KING]);
		for (int bit_index: temp) {
			GenericPosition atSquare = BitBoard.bitToPosition_Lut[bit_index];
			inCheck = squareIsAttacked(atSquare, side);
		}
		return inCheck;
	}

	public PieceType pickUpPieceAtSquare( GenericPosition atPos ) {
		// Calculate bit index
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
	
	private static final Map<GenericFile, BitBoard> DoubledPawn_Lut = new EnumMap<GenericFile, BitBoard>(GenericFile.class);
	static {
		for (GenericFile file : GenericFile.values()) {
			long mask = 0;
			int f=IntFile.valueOf(file);
			for (int r = 0; r<8; r++) {
				mask  |= 1L << r*8+f;
			}
			DoubledPawn_Lut.put(file, new BitBoard(mask));
		}
	}
	
	public int countDoubledPawnsForSide(Colour side) {
		int doubledCount = 0;
		BitBoard pawns = (side==Colour.white) ? getWhitePawns() : getBlackPawns();
		for (GenericFile file : GenericFile.values()) {
			BitBoard mask = DoubledPawn_Lut.get(file);
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
		BitBoard otherSidePawns = (side==Colour.white) ? getBlackPawns() : getWhitePawns();
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
	static BitBoard buildPassedPawnFileMask(GenericFile file, GenericRank rank, boolean isWhite) {
		long mask = 0;
		int r = IntRank.valueOf(rank);
		int f = IntFile.valueOf(file);
		boolean hasPrevFile = file.hasPrev();
		boolean hasNextFile = file.hasNext();
		if (isWhite) {
			for (r=r+1; r < 7; r++) {
				if (hasPrevFile) {
					mask |= 1L << r*8+(f-1);
				}
				mask |= 1L << r*8+f;
				if (hasNextFile) {
					mask |= 1L << r*8+(f+1);
				}
			}
		} else {
			for (r=r-1; r > 0; r--) {
				if (hasPrevFile) {
					mask |= 1L << r*8+(f-1);
				}
				mask |= 1L << r*8+f;
				if (hasNextFile) {
					mask |= 1L << r*8+(f+1);
				}	
			}
		}
		return new BitBoard(mask);
	}
	
	
	
	public class RankAndFile {
		public int rank = IntRank.NORANK;
		public int file = IntFile.NOFILE;
		
		public RankAndFile(GenericPosition atPos) {
			rank = IntRank.valueOf(atPos.rank);
			file = IntFile.valueOf(atPos.file);	
		}
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
			BitBoard bitBoardToIterate = allPieces;
			buildIterList(bitBoardToIterate);
		}

		allPiecesOnBoardIterator( Piece.Colour colourToIterate ) throws InvalidPieceException {
			iterList = new LinkedList<GenericPosition>();
			BitBoard bitBoardToIterate;
			if (colourToIterate == Colour.white) {
				bitBoardToIterate = whitePieces;
			} else {
				bitBoardToIterate = blackPieces;
			}
			buildIterList(bitBoardToIterate);
		}
		
		allPiecesOnBoardIterator( PieceType colourToIterate ) throws InvalidPieceException {
			iterList = new LinkedList<GenericPosition>();
			BitBoard bitBoardToIterate;
			if (colourToIterate == PieceType.WhitePawn) {
				bitBoardToIterate = getWhitePawns();
			} else if (colourToIterate == PieceType.BlackPawn) {
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
			break;
		case WhiteRook:
			break;
		case WhiteBishop:
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
			break;
		case BlackRook:
			break;
		case BlackBishop:
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
	
	public BitBoard getBlackKing() {
		return blackPieces.and(pieces[INDEX_KING]);
	}
	
	public BitBoard getWhitePawns() {
		return whitePieces.and(pieces[INDEX_PAWN]);
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
}
