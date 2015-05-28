package eubos.board;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.King;
import eubos.pieces.Piece.Colour;

public interface IBoardManager {
	// Primary API
	public void performMove( GenericMove move ) throws InvalidPieceException;
	public void unperformMove() throws InvalidPieceException;
	// Secondary API
	public void addCastlingMoves(List<GenericMove> ml);
	public GenericPosition getEnPassantTargetSq();
	public boolean lastMoveWasCapture();
	// Accessor methods
	public Board getTheBoard();
	public Colour getOnMove();
	public King getKing( Colour colour );
}
