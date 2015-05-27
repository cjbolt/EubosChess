package eubos.board;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.Piece;

public class TrackedMove {
	private GenericMove move = null;
	private Piece capturedPiece = null;
	private GenericPosition enPassantTarget = null; 

	public TrackedMove( GenericMove inMove ) { move = inMove; }
	public TrackedMove( GenericMove inMove, Piece capture, GenericPosition enP ) {
		move = inMove; 
		capturedPiece = capture;
		enPassantTarget = enP;
	}
	public boolean isCapture() { return ((capturedPiece != null) ? true : false); }

	public GenericMove getMove() {
		return move;
	}
	public void setMove(GenericMove move) {
		this.move = move;
	}
	public Piece getCapturedPiece() {
		return capturedPiece;
	}
	public void setCapturedPiece(Piece capturedPiece) {
		this.capturedPiece = capturedPiece;
	}
	public GenericPosition getEnPassantTarget() {
		return enPassantTarget;
	}
	public void setEnPassantTarget(GenericPosition enPassantTarget) {
		this.enPassantTarget = enPassantTarget;
	}
}
