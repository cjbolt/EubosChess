package eubos.pieces;

import eubos.board.*;
import com.fluxchess.jcpi.models.*;
import java.util.*;

public abstract class Piece {
	public enum PieceColour { white, black };
	protected PieceColour colour = PieceColour.black;
	protected boolean moved = false;
	public abstract LinkedList<GenericMove> generateMoveList( Board theBoard ); 
}
