package eubos.pieces;

import eubos.board.BoardManager;

import com.fluxchess.jcpi.models.*;
import java.util.*;

public abstract class Piece implements IPiece {
	public enum Colour { 
		white, black; 
		public static Colour getOpposite( Colour arg ) { return ((arg == Colour.white) ? Colour.black : Colour.white);}
	};
	protected Colour colour = Colour.black;
	protected boolean everMoved = false;
	public boolean hasEverMoved() {	return everMoved; }
	public abstract LinkedList<GenericMove> generateMoves( BoardManager bm );
	public abstract boolean attacks( GenericPosition pos );
	protected GenericPosition onSquare;
	
	public Colour getColour() { return colour; }
	public boolean isWhite() { return ( colour == Colour.white ); }
	public boolean isBlack() { return ( colour == Colour.black ); }
	public boolean isOppositeColour(Piece toCheck) { return ( colour != toCheck.getColour()); }
	
	public void setSquare( GenericPosition pos) { onSquare = pos; everMoved = true; }
	public GenericPosition getSquare() { return(onSquare); }
}
