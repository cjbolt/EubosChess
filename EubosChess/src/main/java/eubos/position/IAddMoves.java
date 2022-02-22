package eubos.position;

public interface IAddMoves {
	public void addNormal(int move);
	public void addPrio(int move);
	public boolean isLegalMoveFound();
	public void clearAttackedCache();
}
