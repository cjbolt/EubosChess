package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;

public class Pawn extends SinglesquareDirectMovePiece {

	public Pawn( PieceColour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	private boolean hasNeverMoved() {
		return ( !everMoved && isAtInitialPosition());
	}

	private boolean isAtInitialPosition() {
		if ( isBlack() ) {
			return (onSquare.rank.equals( GenericRank.R7 ));
		} else {
			return (onSquare.rank.equals( GenericRank.R2 ));
		}
	}

	private GenericPosition genOneSqTarget() {
		GenericPosition toPos = null;
		if ( isBlack() ) {
			if ( onSquare.rank != GenericRank.R1 ) {
				toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev());
			}
		} else {
			if ( onSquare.rank != GenericRank.R8 ) {
				toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.next());
			}
		}
		return toPos;
	}	
	
	private GenericPosition genTwoSqTarget() {
		// TODO need to add error protection on this double step move?
		GenericPosition toPos = null;
		if ( isBlack() ) {
			toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev().prev());
		} else {
			toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.next().next());
		}
		return toPos;
	}
	
	private GenericPosition genLeftCaptureTarget() {
		GenericPosition toPos = null;
		if ( isBlack() ) {
			if ( onSquare.file != GenericFile.Fh ) {
				toPos = GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev());
			}
		} else {
			if ( onSquare.file != GenericFile.Fa ) {
				toPos = GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.next());
			}
		}
		return toPos;		
	}
	
	private GenericPosition genRightCaptureTarget() {
		GenericPosition toPos = null;
		if ( isBlack() ) {
			if ( onSquare.file != GenericFile.Fa ) {
				toPos = GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev());
			}
		} else {
			if ( onSquare.file != GenericFile.Fh ) {
				toPos = GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.next());	
			}
		}
		return toPos;		
	}
	
	private boolean checkLeftEnPassantCapture( Board theBoard, GenericMove lastMove ) {
		if ( lastMove.to.rank == onSquare.rank ) {
			if ( isBlack() ) {
				if ( onSquare.file != GenericFile.Fh ) {
					if (( lastMove.to.file == onSquare.file.next() )) {

						Piece enPassantPiece = theBoard.getPieceAtSquare( lastMove.to );
						if ( enPassantPiece instanceof Pawn ) {
							return true;
						}
					}
				}
			} else {
				if ( onSquare.file != GenericFile.Fa ) {
					if (( lastMove.to.file == onSquare.file.prev() )) {
						Piece enPassantPiece = theBoard.getPieceAtSquare( lastMove.to );
						if ( enPassantPiece instanceof Pawn ) {
							return true;
						}
					}
				}	
			}
		}
		return false;
	}
	
	private boolean checkRightEnPassantCapture( Board theBoard, GenericMove lastMove ) {
		if ( lastMove.to.rank == onSquare.rank ) {
			if ( isBlack() ) {
				if ( onSquare.file != GenericFile.Fa ) {
					if (( lastMove.to.file == onSquare.file.prev() )) {
						Piece enPassantPiece = theBoard.getPieceAtSquare( lastMove.to );
						if ( enPassantPiece instanceof Pawn ) {
							return true;
						}
					}
				}		
			} else {
				if ( onSquare.file != GenericFile.Fh ) {
					if (( lastMove.to.file == onSquare.file.next() )) {
						Piece enPassantPiece = theBoard.getPieceAtSquare( lastMove.to );
						if ( enPassantPiece instanceof Pawn ) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean isCapturable(Board theBoard, GenericPosition captureAt ) {
		boolean isCapturable = false;
		Piece queryPiece = theBoard.getPieceAtSquare( captureAt );
		if ( queryPiece != null ) {
			isCapturable = isOppositeColour( queryPiece );
		}
		return isCapturable;
	}
	
	private boolean checkEnPassantPossible() {
		return ((isBlack() && onSquare.rank.equals( GenericRank.R4 )) ||
				(isWhite() && onSquare.rank.equals( GenericRank.R5 )));
	}
	
	private boolean checkPromotionPossible( GenericPosition targetSquare ) {
		return (( isBlack() && targetSquare.rank == GenericRank.R1 ) || 
				( isWhite() && targetSquare.rank == GenericRank.R8 ));
	}
	
	private void checkPromotionAddMove(LinkedList<GenericMove> moveList,
			GenericPosition targetSquare) {
		if ( checkPromotionPossible( targetSquare )) {
			moveList.add( new GenericMove( onSquare, targetSquare, GenericChessman.QUEEN ));
			moveList.add( new GenericMove( onSquare, targetSquare, GenericChessman.KNIGHT ));
			moveList.add( new GenericMove( onSquare, targetSquare, GenericChessman.BISHOP ));
			moveList.add( new GenericMove( onSquare, targetSquare, GenericChessman.ROOK ));
		} else {
			moveList.add( new GenericMove( onSquare, targetSquare ) );
		}
	}	
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		// Check for standard one and two square moves
		GenericPosition moveTo = genOneSqTarget();
		if ( moveTo != null && theBoard.isSquareEmpty( moveTo )) {
			checkPromotionAddMove(moveList, moveTo);
			if ( hasNeverMoved() ) {
				moveTo = genTwoSqTarget();
				if ( theBoard.isSquareEmpty( moveTo )) {
					moveList.add( new GenericMove( onSquare, moveTo ) );
				}
			}	
		}
		// Check for capture moves
		GenericPosition captureAt = genLeftCaptureTarget();
		if ( captureAt != null && isCapturable(theBoard,captureAt)) {
			checkPromotionAddMove(moveList, captureAt);
		}
		captureAt = genRightCaptureTarget();
		if ( captureAt != null && isCapturable(theBoard,captureAt)) {
			checkPromotionAddMove(moveList, captureAt);
		}
		// Check for en passant capture moves
		if ( checkEnPassantPossible() ) {
			GenericMove lastMove = theBoard.getPreviousMove();
			if ( lastMove != null ) {
				if ( checkLeftEnPassantCapture( theBoard, lastMove )) {
					captureLeft(moveList);
				}
				if ( checkRightEnPassantCapture( theBoard, lastMove )) {
					captureRight(moveList);
				}
			}
		}
		return moveList;
	}

	private void captureRight(LinkedList<GenericMove> moveList) {
		GenericPosition captureAt;
		captureAt = genRightCaptureTarget();
		if ( captureAt != null ) {
			moveList.add( new GenericMove( onSquare, captureAt ) );
		}
	}

	private void captureLeft(LinkedList<GenericMove> moveList) {
		GenericPosition captureAt;
		captureAt = genLeftCaptureTarget();
		if ( captureAt != null ) {
			moveList.add( new GenericMove( onSquare, captureAt ) );
		}
	}
}
