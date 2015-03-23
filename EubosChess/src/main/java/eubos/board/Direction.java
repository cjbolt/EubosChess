package eubos.board;

import com.fluxchess.jcpi.models.GenericPosition;

public enum Direction {
	up, upRight, right, downRight, down, downLeft, left, upLeft; 
	public GenericPosition getSqInDirection( GenericPosition startSq, Direction dir ) {
		GenericPosition retVal = null;
		switch( dir ) {
		case downLeft:
			retVal = GenericPosition.valueOf( startSq.file.prev(), startSq.rank.prev());
		case down:
			break;
		case downRight:
			break;
		case left:
			break;
		case right:
			break;
		case up:
			break;
		case upLeft:
			break;
		case upRight:
			break;
		default:
			break;
		}
		return retVal;
	}
}
