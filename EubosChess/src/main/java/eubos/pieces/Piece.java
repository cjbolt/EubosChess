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
	
	public PieceColour getColour() { return colour; }
	public boolean isWhite() { return ( colour == PieceColour.white ); }
	public boolean isBlack() { return ( colour == PieceColour.black ); }
	public boolean isOppositeColour(Piece toCheck) { return ( colour != toCheck.getColour()); }
	
	public void setSquare( GenericPosition pos) { onSquare = pos; everMoved = true; }
	public GenericPosition getSquare() { return(onSquare); }
}
