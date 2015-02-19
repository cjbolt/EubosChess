package eubos.pieces;

import eubos.board.*;
import com.fluxchess.jcpi.models.*;
import java.util.*;

public abstract class Piece {
	public enum PieceColour { white, black };
	protected PieceColour colour = PieceColour.black;
	protected boolean everMoved = false;
	public abstract LinkedList<GenericMove> generateMoveList( Board theBoard ); 
	protected GenericPosition onSquare;
	
	public boolean isWhite() { return ( colour == PieceColour.white ); }
	public boolean isBlack() { return !isWhite(); }	
	
	public void updateSquare( GenericPosition pos) {
		onSquare = pos;
	}
}
