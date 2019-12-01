package eubos.board;

import java.util.ArrayList;
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
			if (blackPieces.and(pieceToPickUp).getSquareOccupied() != 0) {
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
			} else if (whitePieces.and(pieceToPickUp).getSquareOccupied() != 0) {
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
		return ( ownColour.equals(Colour.white) ?
				(toCheck==PieceType.BlackKing || 
				toCheck==PieceType.BlackQueen ||
				toCheck==PieceType.BlackRook ||
				toCheck==PieceType.BlackBishop ||
				toCheck==PieceType.BlackKnight ||
				toCheck==PieceType.BlackPawn) :
				(toCheck==PieceType.WhiteKing || 
				toCheck==PieceType.WhiteQueen ||
				toCheck==PieceType.WhiteRook ||
				toCheck==PieceType.WhiteBishop ||
				toCheck==PieceType.WhiteKnight ||
				toCheck==PieceType.WhitePawn)); 
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
		if (blackPieces.and(pieceToPickUp).getSquareOccupied() != 0) {
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
		} else if (whitePieces.and(pieceToPickUp).getSquareOccupied() != 0) {
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
			assert false;
		}
		return type;
	}
	
	public void setPieceAtSquare( GenericPosition atPos, PieceType pieceToPlace ) {
		int bit_index = BitBoard.positionToBit_Lut.get(atPos);
		// assert nothing there already
		if (pieceToPlace.equals(PieceType.WhiteKing)) {
			pieces[INDEX_KING].set(bit_index);
			whitePieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.WhiteQueen)) {
			pieces[INDEX_QUEEN].set(bit_index);
			whitePieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.WhiteRook)) {
			pieces[INDEX_ROOK].set(bit_index);
			whitePieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.WhiteBishop)) {
			pieces[INDEX_BISHOP].set(bit_index);
			whitePieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.WhiteKnight)) {
			pieces[INDEX_KNIGHT].set(bit_index);
			whitePieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.WhitePawn)) {
			pieces[INDEX_PAWN].set(bit_index);
			whitePieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.BlackKing)) {
			pieces[INDEX_KING].set(bit_index);
			blackPieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.BlackQueen)) {
			pieces[INDEX_QUEEN].set(bit_index);
			blackPieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.BlackRook)) {
			pieces[INDEX_ROOK].set(bit_index);
			blackPieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.BlackBishop)) {
			pieces[INDEX_BISHOP].set(bit_index);
			blackPieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.BlackKnight)) {
			pieces[INDEX_KNIGHT].set(bit_index);
			blackPieces.set(bit_index);
		} else if (pieceToPlace.equals(PieceType.BlackPawn)) {
			pieces[INDEX_PAWN].set(bit_index);
			blackPieces.set(bit_index);
		} else {
			assert false;
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
		if (blackPieces.and(pieceToPickUp).getSquareOccupied() != 0) {
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
		return type;
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

		private void buildIterList(BitBoard bitBoardToIterate) {
			for (int bit_index: bitBoardToIterate) {
				int file = bit_index%8;
				int rank = bit_index/8;
				iterList.add(GenericPosition.valueOf(IntFile.toGenericFile(file),IntRank.toGenericRank(rank)));
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public Iterator<GenericPosition> iterateColour( Piece.Colour colourToIterate ) {
		try {
			return new allPiecesOnBoardIterator( colourToIterate );
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
