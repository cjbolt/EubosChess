package eubos.position;

public interface IChangePosition {
	public void performMove( int move );
	public void performMove( int move, boolean computeHash );
	public void unperformMove();
	public void unperformMove( boolean computeHash );
	
	public void performNullMove();
	public void unperformNullMove();
}
