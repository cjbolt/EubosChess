package eubos.position;

public interface IPawnHash {
	public void removePawn(boolean isWhite, int at);
	public void movePawn(boolean isWhite, int from, int to);
}
