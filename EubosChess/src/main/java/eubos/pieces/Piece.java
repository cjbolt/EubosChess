package eubos.pieces;

import com.fluxchess.jcpi.models.GenericPosition;

import java.util.List;

public abstract class Piece implements IPiece {
	public enum Colour { 
		white, black; 
		public static Colour getOpposite( Colour arg ) { return ((arg == white) ? black : white);}
	};
	protected Colour colour = Colour.black;
	public Colour getColour() { return colour; }
	public boolean isWhite() { return ( colour == Colour.white ); }
	public boolean isBlack() { return ( colour == Colour.black ); }
	public boolean isOppositeColour(Piece toCheck) { return ( colour != toCheck.getColour()); }
	public boolean isOppositeColour(Colour colourToCheck) { return ( colour != colourToCheck); }
	
	protected boolean everMoved = false;
	public boolean hasEverMoved() {	return everMoved; }
	
	protected GenericPosition onSquare;
	
	public void setSquare( GenericPosition pos) { onSquare = pos; everMoved = true; }
	public GenericPosition getSquare() { return(onSquare); }
	
	protected boolean evaluateIfAttacks( GenericPosition [] sqsToCheck, List<GenericPosition> targettedSqs ) {
		boolean sqAttacked = false;
		for ( GenericPosition currSq : sqsToCheck) {
			if (targettedSqs.contains(currSq)) {
				sqAttacked = true;
				break;
			}
		}
		return sqAttacked;
	}
}
