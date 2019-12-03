package eubos.board.pieces;

public abstract class Piece {
	public enum Colour { 
		white, black;
		
		public static Colour getOpposite( Colour arg ) { return (arg == white) ? black : white; }
		public static boolean isWhite( Colour arg ) { return arg == white; }
		public static boolean isBlack( Colour arg ) { return arg == black; }
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
		NONE;
		
		public static boolean isOppositeColour(Colour ownColour, PieceType toCheck) {
			boolean isOpposite = false;
			assert toCheck != PieceType.NONE;
			if (Colour.isWhite(ownColour)) {
				isOpposite = toCheck.ordinal() >= PieceType.BlackKing.ordinal();
			} else {
				isOpposite = toCheck.ordinal() < PieceType.BlackKing.ordinal();
			}
			return isOpposite;
		}
		public static boolean isWhite(PieceType arg) {
			return arg.ordinal() < PieceType.BlackKing.ordinal();
		}
		public static boolean isBlack(PieceType arg) {
			return arg.ordinal() >= PieceType.BlackKing.ordinal();
		} 
	};
}
