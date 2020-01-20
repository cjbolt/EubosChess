package eubos.board;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.position.Position;

public enum Direction {
	up, upRight, rightUp, right, rightDown, downRight, down, downLeft, leftUp, left, leftDown, upLeft;
	
	public static int getDirectMoveSq( Direction dir, int startSq ) {
		int retVal = 0xFF;
		switch( dir ) {
		case downLeft:
		case leftDown:
			if ( Position.getFile(startSq) != IntFile.Fa && Position.getRank(startSq) != IntRank.R1 )
				retVal = startSq-17;
			break;
		case down:
			if ( Position.getRank(startSq) != IntRank.R1 )
				retVal = startSq-16;
			break;
		case downRight:
		case rightDown:
			if ( Position.getFile(startSq) != IntFile.Fh && Position.getRank(startSq) != IntRank.R1 )
				retVal = startSq-15;
			break;
		case left:
			if ( Position.getFile(startSq) != IntFile.Fa )
				retVal = startSq-1;
			break;
		case right:
			if ( Position.getFile(startSq) != IntFile.Fh )
				retVal = startSq+1;
			break;
		case up:
			if ( Position.getRank(startSq) != IntRank.R8 )
				retVal = startSq+16;
			break;
		case upLeft:
		case leftUp:
			if ( Position.getFile(startSq) != IntFile.Fa && Position.getRank(startSq) != IntRank.R8 )
				retVal = startSq+15;
			break;
		case upRight:
		case rightUp:
			if ( Position.getFile(startSq) != IntFile.Fh && Position.getRank(startSq) != IntRank.R8 )
				retVal = startSq+17;
			break;
		default:
			break;
		}
		return retVal;
	}
	
	public static GenericPosition getIndirectMoveSq( Direction dir, GenericPosition onSquare ) {
		GenericPosition retVal = null;
		switch( dir ) {
		case downLeft:
			if ( onSquare.file != GenericFile.Fa && ((onSquare.rank != GenericRank.R1) && (onSquare.rank != GenericRank.R2))) {
				retVal = GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev().prev());
			}
			break;
		case downRight:
			if ( onSquare.file != GenericFile.Fh && ((onSquare.rank != GenericRank.R1) && (onSquare.rank != GenericRank.R2))) {
				retVal = GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev().prev());
			}
			break;
		case leftUp:
			if (((onSquare.file != GenericFile.Fa) && (onSquare.file != GenericFile.Fb)) && onSquare.rank != GenericRank.R8 )  {
				retVal = GenericPosition.valueOf( onSquare.file.prev().prev(), onSquare.rank.next());
			}
			break;
		case leftDown:
			if (((onSquare.file != GenericFile.Fa) && (onSquare.file != GenericFile.Fb)) && onSquare.rank != GenericRank.R1 )  {
				retVal = GenericPosition.valueOf( onSquare.file.prev().prev(), onSquare.rank.prev());
			}
			break;
		case rightUp:
			if (((onSquare.file != GenericFile.Fg) && (onSquare.file != GenericFile.Fh)) && onSquare.rank != GenericRank.R8 ) {
				retVal = GenericPosition.valueOf( onSquare.file.next().next(), onSquare.rank.next());
			}
			break;
		case rightDown:
			if (((onSquare.file != GenericFile.Fg) && (onSquare.file != GenericFile.Fh)) && onSquare.rank != GenericRank.R1  ) {
				retVal = GenericPosition.valueOf( onSquare.file.next().next(), onSquare.rank.prev());
			}
			break;
		case upLeft:
			if ( onSquare.file != GenericFile.Fa && ((onSquare.rank != GenericRank.R8) && (onSquare.rank != GenericRank.R7))) {
				retVal = GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.next().next());
			}
			break;
		case upRight:
			if ( onSquare.file != GenericFile.Fh && ((onSquare.rank != GenericRank.R8) && (onSquare.rank != GenericRank.R7))) {
				retVal = GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.next().next());	
			}
			break;
		default:
			break;
		}
		return retVal;
	}	
}
