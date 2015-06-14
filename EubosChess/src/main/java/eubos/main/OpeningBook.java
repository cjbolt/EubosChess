package eubos.main;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class OpeningBook {
	private boolean inBook = true;
	private GenericMove[] KGA = null;
	
	public OpeningBook() {
		inBook = true;
		try {
			this.KGA = new GenericMove[] { new GenericMove("e2e4"), new GenericMove("e7e5"), new GenericMove("f2f4"), new GenericMove("e5f4"), new GenericMove("g1f3") };;
		} catch (IllegalNotationException e) {}
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
			try {
				return KGA[movNum];
			} catch (IndexOutOfBoundsException e) {
				inBook = false;
				return null;
			}
		} else {
			return null;
		}
	}
}
