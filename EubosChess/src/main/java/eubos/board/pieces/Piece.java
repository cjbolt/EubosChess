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
		
		public static boolean isPawn(PieceType arg) { return arg==WhitePawn || arg==BlackPawn; }
		public static boolean isKing(PieceType arg) { return arg==WhiteKing || arg==BlackKing; }
		public static boolean isQueen(PieceType arg) { return arg==WhiteQueen || arg==BlackQueen; }
		public static boolean isRook(PieceType arg) { return arg==WhiteRook || arg==BlackRook; }
		public static boolean isBishop(PieceType arg) { return arg==WhiteBishop || arg==BlackBishop; }
		public static boolean isKnight(PieceType arg) { return arg==WhiteKnight || arg==BlackKnight; }
		
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
