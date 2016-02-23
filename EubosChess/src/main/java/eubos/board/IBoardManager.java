package eubos.board;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
//import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.Piece.Colour;

public interface IBoardManager {
	// Primary API
	public void performMove( GenericMove move ) throws InvalidPieceException;
	public void unperformMove() throws InvalidPieceException;
	// Secondary API
	public List<GenericMove> getMoveList() throws InvalidPieceException;
	//public GenericPosition getEnPassantTargetSq(); // implemented in Board
	public int getCastlingAvaillability();
	public boolean isKingInCheck();
	// Accessor methods
	public Board getTheBoard();
	public Colour getOnMove();
}
