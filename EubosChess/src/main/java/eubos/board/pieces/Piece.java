package eubos.board.pieces;

import com.fluxchess.jcpi.models.GenericPosition;

public abstract class Piece implements IPiece {
	public enum Colour { 
		white, black; 
		public static Colour getOpposite( Colour arg ) { return ((arg == white) ? black : white);}
	};
	
	public enum PieceType {
		WhiteKing,
		WhiteQueen,
		WhiteRook,
		WhiteBishop,
		WhiteKnight,
		WhitePawn,
		BlackKing,
		BlackQueen,
		BlackRook,
		BlackBishop,
		BlackKnight,
		BlackPawn,
		NONE
	};
	
	protected Colour colour = Colour.black;
	public Colour getColour() { return colour; }
	public boolean isWhite() { return ( colour == Colour.white ); }
	public boolean isBlack() { return ( colour == Colour.black ); }
	public boolean isOppositeColour(PieceType toCheck) {
		return ( isWhite() ?
				(toCheck==PieceType.BlackKing || 
				toCheck==PieceType.BlackQueen ||
				toCheck==PieceType.BlackRook ||
				toCheck==PieceType.BlackBishop ||
				toCheck==PieceType.BlackKnight ||
				toCheck==PieceType.BlackPawn) :
				(toCheck==PieceType.WhiteKing || 
				toCheck==PieceType.WhiteQueen ||
				toCheck==PieceType.WhiteRook ||
				toCheck==PieceType.WhiteBishop ||
				toCheck==PieceType.WhiteKnight ||
				toCheck==PieceType.WhitePawn)); 
				}
	public boolean isOppositeColour(Colour colourToCheck) { return ( colour != colourToCheck); }
	
	protected GenericPosition onSquare;
	public void setSquare( GenericPosition pos) { onSquare = pos; }
	public GenericPosition getSquare() { return(onSquare); }
}
