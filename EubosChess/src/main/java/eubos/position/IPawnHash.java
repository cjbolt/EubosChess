package eubos.position;

public interface IPawnHash {
	public void removePawn(int pawn, int at);
	public void addPawn(int pawn, int at);
	public void movePawn(int pawn, int from, int to);
}
