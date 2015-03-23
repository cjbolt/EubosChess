package eubos.board;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

public enum Direction {
	up, upRight, right, downRight, down, downLeft, left, upLeft;
	
	public static GenericPosition getSqInDirection( Direction dir, GenericPosition startSq ) {
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
}
