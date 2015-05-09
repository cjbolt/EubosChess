package eubos.pieces;

import eubos.board.BoardManager;

import com.fluxchess.jcpi.models.*;

import java.util.*;

public abstract class Piece implements IPiece {
	public enum Colour { 
		white, black; 
		public static Colour getOpposite( Colour arg ) { return ((arg == white) ? black : white);}
	};
	protected Colour colour = Colour.black;
	protected boolean everMoved = false;
	public boolean hasEverMoved() {	return everMoved; }
	public abstract List<GenericMove> generateMoves( BoardManager bm );
	public abstract boolean attacks( BoardManager bm, GenericPosition [] pos );
	protected GenericPosition onSquare;
	
	public Colour getColour() { return colour; }
	public boolean isWhite() { return ( colour == Colour.white ); }
	public boolean isBlack() { return ( colour == Colour.black ); }
	public boolean isOppositeColour(Piece toCheck) { return ( colour != toCheck.getColour()); }
	
	public void setSquare( GenericPosition pos) { onSquare = pos; everMoved = true; }
	public GenericPosition getSquare() { return(onSquare); }
	
	protected boolean evaluateIfAttacks( GenericPosition [] sqsToCheck, List<GenericPosition> targettedSqs ) {
		boolean sqAttacked = false;
		for ( GenericPosition currSq : sqsToCheck) {
			if (targettedSqs.contains(currSq))
				sqAttacked = true;
		}
		return sqAttacked;
	}
}
