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
	
	private boolean isBlackPawnNeverMoved() {
		return (!everMoved && onSquare.rank.equals( GenericRank.R7 ));
	}

	private GenericPosition genOneSqTarget() {
		GenericPosition toPos = null;
		if ( onSquare.rank != GenericRank.R1 ) {
			toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev());
		}
		return toPos;
	}	
	
	private GenericPosition genTwoSqTarget() {
		GenericPosition toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev().prev());
		return toPos;
	}
	
	private GenericPosition genLeftCaptureTarget() {
		GenericPosition toPos = null;
		if ( onSquare.file != GenericFile.Fh ) {
			toPos = GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev());
		}
		return toPos;		
	}
	
	private GenericPosition genRightCaptureTarget() {
		GenericPosition toPos = null;
		if ( onSquare.file != GenericFile.Fa ) {
			toPos = GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev());
		}
		return toPos;		
	}
	
	private boolean checkRightEnPassantCapture( Board theBoard, GenericMove lastMove ) {
		if ( onSquare.file != GenericFile.Fh ) {
			if (( lastMove.to.file == onSquare.file.next() )) {
				Piece enPassantPiece = theBoard.getPieceAtSquare( lastMove.to );
				if ( enPassantPiece instanceof Pawn ) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean checkLeftEnPassantCapture( Board theBoard, GenericMove lastMove ) {
		if ( onSquare.file != GenericFile.Fa ) {
			if (( lastMove.to.file == onSquare.file.prev() )) {
				Piece enPassantPiece = theBoard.getPieceAtSquare( lastMove.to );
				if ( enPassantPiece instanceof Pawn ) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		if ( isBlack() ) {
			// Check for standard one and two square moves
			GenericPosition moveTo = genOneSqTarget();
			if ( moveTo != null && theBoard.isSquareEmpty( moveTo )) {
				if ( moveTo.rank == GenericRank.R1 ) {
					moveList.add( new GenericMove( onSquare, moveTo, GenericChessman.QUEEN ));
					moveList.add( new GenericMove( onSquare, moveTo, GenericChessman.KNIGHT ));
					moveList.add( new GenericMove( onSquare, moveTo, GenericChessman.BISHOP ));
					moveList.add( new GenericMove( onSquare, moveTo, GenericChessman.ROOK ));
				} else {
					moveList.add( new GenericMove( onSquare, moveTo ) );
				}
				if ( isBlackPawnNeverMoved() ) {
					moveTo = genTwoSqTarget();
					if ( theBoard.isSquareEmpty( moveTo )) {
						moveList.add( new GenericMove( onSquare, moveTo ) );
					}
				}	
			}
			// Check for capture moves
			GenericPosition captureAt = genLeftCaptureTarget();
			if ( captureAt != null && theBoard.isSquareWhitePiece( captureAt )) {
				if ( captureAt.rank == GenericRank.R1 ) {
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.QUEEN ));
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.KNIGHT ));
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.BISHOP ));
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.ROOK ));
				} else {
					moveList.add( new GenericMove( onSquare, captureAt ) );
				}
			}
			captureAt = genRightCaptureTarget();
			if ( captureAt != null && theBoard.isSquareWhitePiece( captureAt )) {
				if ( captureAt.rank == GenericRank.R1 ) {
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.QUEEN ));
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.KNIGHT ));
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.BISHOP ));
					moveList.add( new GenericMove( onSquare, captureAt, GenericChessman.ROOK ));
				} else {
					moveList.add( new GenericMove( onSquare, captureAt ) );
				}
			}
			// Check for en passant capture moves
			if ( onSquare.rank == GenericRank.R4 ) {
				GenericMove lastMove = theBoard.getPreviousMove();
				if ( lastMove != null ) {
					if ( checkRightEnPassantCapture( theBoard, lastMove )) {
						captureAt = genLeftCaptureTarget();
						if ( captureAt != null ) {
							moveList.add( new GenericMove( onSquare, captureAt ) );
						}
					}
					if ( checkLeftEnPassantCapture( theBoard, lastMove )) {
						captureAt = genRightCaptureTarget();
						if ( captureAt != null ) {
							moveList.add( new GenericMove( onSquare, captureAt ) );
						}
					}
				}
			}
		}
		return moveList;
	}

}
