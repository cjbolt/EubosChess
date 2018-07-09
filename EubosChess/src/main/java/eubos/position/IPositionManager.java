package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
//import com.fluxchess.jcpi.models.GenericPosition;




import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;

public interface IPositionManager {
	// Primary API
	public void performMove( GenericMove move ) throws InvalidPieceException;
	public void unperformMove() throws InvalidPieceException;
	// Secondary API
	public List<GenericMove> getMoveList() throws InvalidPieceException;
	public int getCastlingAvaillability();
	public boolean isKingInCheck();
	// Accessor methods
	public Board getTheBoard();
	public Colour getOnMove();
}
