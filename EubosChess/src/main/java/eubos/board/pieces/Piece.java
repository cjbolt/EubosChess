package eubos.board.pieces;

import com.fluxchess.jcpi.models.GenericPosition;

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
	
	protected GenericPosition onSquare;
	public void setSquare( GenericPosition pos) { onSquare = pos; }
	public GenericPosition getSquare() { return(onSquare); }
}
