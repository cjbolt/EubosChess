package eubos.position;

public interface IChangePosition {
	public void performMove( int move );
	public void unperformMove();
	
	public void performNullMove();
	public void unperformNullMove();
}
