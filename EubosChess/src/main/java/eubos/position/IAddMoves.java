package eubos.position;

public interface IAddMoves {
	public boolean addNormal(int move);
	public boolean addPrio(int move);
	public boolean isLegalMoveFound();
}
