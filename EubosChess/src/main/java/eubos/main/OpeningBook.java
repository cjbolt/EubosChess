package eubos.main;

//import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class OpeningBook {
	private boolean inBook = true;
	private GenericMove[] KGA = null;
	
	public OpeningBook() {
		inBook = true;
		try {
			KGA = new GenericMove[] { new GenericMove("e2e4"), new GenericMove("e7e5"), new GenericMove("f2f4"), new GenericMove("exf"), new GenericMove("exf") };;
		} catch (IllegalNotationException e) {
			KGA = new GenericMove[]{new GenericMove(GenericPosition.e2, GenericPosition.e4), new GenericMove(GenericPosition.e7, GenericPosition.e5)};
		}
	}
	
	public GenericMove getMove( List<GenericMove> prevMoves ) {
		int movNum = 0;
		GenericMove bookMove = null;
		if (inBook) {
			for ( GenericMove mov: prevMoves) {
				bookMove = KGA[movNum];
				if (!mov.equals(bookMove)) {
					inBook = false;
					break;
				}
				movNum++;
			}
		}
		if (inBook) {
			return KGA[movNum];
		} else {
			return null;
		}
	}
}
