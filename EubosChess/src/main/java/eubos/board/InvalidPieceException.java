package eubos.board;

import com.fluxchess.jcpi.models.GenericPosition;

public class InvalidPieceException extends Exception {
	private static final long serialVersionUID = 1L;
	private GenericPosition atPosition;
	
	public InvalidPieceException( GenericPosition pos ) {
		super();
		atPosition = pos;
	}
	
	public GenericPosition getAtPosition() { return atPosition; }
}
