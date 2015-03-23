package eubos.board;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

public enum Direction {
	up, upRight, rightUp, right, rightDown, downRight, down, downLeft, leftUp, left, leftDown, upLeft;
	
	public static GenericPosition getDirectMoveSq( Direction dir, GenericPosition startSq ) {
		GenericPosition retVal = null;
		switch( dir ) {
		case downLeft:
			if ( startSq.file != GenericFile.Fa && startSq.rank != GenericRank.R1 )
				retVal = GenericPosition.valueOf( startSq.file.prev(), startSq.rank.prev());
			break;
		case down:
			if ( startSq.rank != GenericRank.R1 )
				retVal = GenericPosition.valueOf( startSq.file, startSq.rank.prev());
			break;
		case downRight:
			if ( startSq.file != GenericFile.Fh && startSq.rank != GenericRank.R1 )
				retVal = GenericPosition.valueOf( startSq.file.next(), startSq.rank.prev());
			break;
		case left:
			if ( startSq.file != GenericFile.Fa )
				retVal = GenericPosition.valueOf( startSq.file.prev(), startSq.rank);
			break;
		case right:
			if ( startSq.file != GenericFile.Fh )
				retVal = GenericPosition.valueOf( startSq.file.next(), startSq.rank);
			break;
		case up:
			if ( startSq.rank != GenericRank.R8 )
				retVal = GenericPosition.valueOf( startSq.file, startSq.rank.next());
			break;
		case upLeft:
			if ( startSq.file != GenericFile.Fa && startSq.rank != GenericRank.R8 )
				retVal = GenericPosition.valueOf( startSq.file.prev(), startSq.rank.next());
			break;
		case upRight:
			if ( startSq.file != GenericFile.Fh && startSq.rank != GenericRank.R8 )
				retVal = GenericPosition.valueOf( startSq.file.next(), startSq.rank.next());
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
