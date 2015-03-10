package eubos.pieces;

import eubos.board.BoardManager;

import com.fluxchess.jcpi.models.*;
import java.util.*;

public abstract class Piece implements IPiece {
	public enum Colour { white, black };
	protected Colour colour = Colour.black;
	protected boolean everMoved = false;
	public abstract LinkedList<GenericMove> generateMoves( BoardManager bm ); 
	protected GenericPosition onSquare;
	
	public Colour getColour() { return colour; }
	public boolean isWhite() { return ( colour == Colour.white ); }
	public boolean isBlack() { return ( colour == Colour.black ); }
	public boolean isOppositeColour(Piece toCheck) { return ( colour != toCheck.getColour()); }
	
	public void setSquare( GenericPosition pos) { onSquare = pos; everMoved = true; }
	public GenericPosition getSquare() { return(onSquare); }
}
