package eubos.board;

import eubos.position.Position;

public enum Direction {
	up, upRight, right, downRight, down, downLeft, left, upLeft, rightUp, rightDown, leftDown, leftUp;
	
	public static int getDirectMoveSq( Direction dir, int startSq ) {
		int retVal = Position.NOPOSITION;
		switch( dir ) {
		case downLeft:
		case leftDown:
			retVal = startSq-17;
			break;
		case down:
			retVal = startSq-16;
			break;
		case downRight:
		case rightDown:
			retVal = startSq-15;
			break;
		case left:
			retVal = startSq-1;
			break;
		case right:
			retVal = startSq+1;
			break;
		case up:
			retVal = startSq+16;
			break;
		case upLeft:
		case leftUp:
			retVal = startSq+15;
			break;
		case upRight:
		case rightUp:
			retVal = startSq+17;
			break;
		default:
			break;
		}
		if ((retVal & 0x88) != 0)
			retVal = Position.NOPOSITION;
		return retVal;
	}
	
	public static int getIndirectMoveSq( Direction dir, int onSquare ) {
		int retVal = Position.NOPOSITION;
		switch( dir ) {
		case downLeft:
			retVal = onSquare-33;
			break;
		case downRight:
			retVal = onSquare-31;
			break;
		case leftUp:
			retVal = onSquare+14;
			break;
		case leftDown:
			retVal = onSquare-18;
			break;
		case rightUp:
			retVal = onSquare+18;
			break;
		case rightDown:
			retVal = onSquare-14;
			break;
		case upLeft:
			retVal = onSquare+31;
			break;
		case upRight:
			retVal = onSquare+33;	
			break;
		default:
			break;
		}
		if ((retVal & 0x88) != 0)
			retVal = Position.NOPOSITION;
		return retVal;
	}	
}
