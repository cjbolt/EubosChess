package eubos.board;

import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.position.Position;

public enum Direction {
	up, upRight, rightUp, right, rightDown, downRight, down, downLeft, leftUp, left, leftDown, upLeft;
	
	public static int getDirectMoveSq( Direction dir, int startSq ) {
		int retVal = Position.NOPOSITION;
		switch( dir ) {
		case downLeft:
		case leftDown:
			if ( Position.getFile(startSq) > IntFile.Fa && 
				 Position.getRank(startSq) > IntRank.R1 )
				retVal = startSq-17;
			break;
		case down:
			if ( Position.getRank(startSq) > IntRank.R1 )
				retVal = startSq-16;
			break;
		case downRight:
		case rightDown:
			if ( Position.getFile(startSq) < IntFile.Fh && 
				 Position.getRank(startSq) > IntRank.R1 )
				retVal = startSq-15;
			break;
		case left:
			if ( Position.getFile(startSq) > IntFile.Fa )
				retVal = startSq-1;
			break;
		case right:
			if ( Position.getFile(startSq) < IntFile.Fh )
				retVal = startSq+1;
			break;
		case up:
			if ( Position.getRank(startSq) < IntRank.R8 )
				retVal = startSq+16;
			break;
		case upLeft:
		case leftUp:
			if ( Position.getFile(startSq) > IntFile.Fa && 
				 Position.getRank(startSq) < IntRank.R8 )
				retVal = startSq+15;
			break;
		case upRight:
		case rightUp:
			if ( Position.getFile(startSq) < IntFile.Fh &&
				 Position.getRank(startSq) < IntRank.R8 )
				retVal = startSq+17;
			break;
		default:
			break;
		}
		return retVal;
	}
	
	public static int getIndirectMoveSq( Direction dir, int onSquare ) {
		int retVal = Position.NOPOSITION;
		switch( dir ) {
		case downLeft:
			if ( Position.getFile(onSquare) > IntFile.Fa && 
			     Position.getRank(onSquare) > IntRank.R2) {
				retVal = onSquare-33;
			}
			break;
		case downRight:
			if ( Position.getFile(onSquare) < IntFile.Fh && 
			     Position.getRank(onSquare) > IntRank.R2) {
				retVal = onSquare-31;
			}
			break;
		case leftUp:
			if (Position.getFile(onSquare) > IntFile.Fb && 
				Position.getRank(onSquare) < IntRank.R8 )  {
				retVal = onSquare+14;
			}
			break;
		case leftDown:
			if (Position.getFile(onSquare) > IntFile.Fb && 
				Position.getRank(onSquare) > IntRank.R1 )  {
				retVal = onSquare-18;
			}
			break;
		case rightUp:
			if (Position.getFile(onSquare) < IntFile.Fg && 
				Position.getRank(onSquare) < IntRank.R8 ) {
				retVal = onSquare+18;
			}
			break;
		case rightDown:
			if (Position.getFile(onSquare) < IntFile.Fg && 
				Position.getRank(onSquare) > IntRank.R1 ) {
				retVal = onSquare-14;
			}
			break;
		case upLeft:
			if (Position.getFile(onSquare) > IntFile.Fa && 
				Position.getRank(onSquare) < IntRank.R7) {
				retVal = onSquare+31;
			}
			break;
		case upRight:
			if (Position.getFile(onSquare) < IntFile.Fh && 
				Position.getRank(onSquare) < IntRank.R7) {
				retVal = onSquare+33;	
			}
			break;
		default:
			break;
		}
		return retVal;
	}	
}
