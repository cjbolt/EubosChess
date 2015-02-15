package eubos.pieces;

public abstract class Piece {
	public enum PieceColour { white, black };
	public PieceColour colour = PieceColour.black;
	public boolean moved = false;
	public abstract void generateMoveList(); 
}
