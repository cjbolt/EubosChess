package eubos.pieces;

public abstract class Piece {
	public enum PieceColour { white, black };
	protected PieceColour colour = PieceColour.black;
	protected boolean moved = false;
	public abstract void generateMoveList(); 
}
