package eubos.board.pieces;

public abstract class Piece {
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
}
