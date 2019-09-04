 package eubos.board;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;

public class SquareAttackEvaluator {
	
	private Piece.Colour attackingColour;
	private GenericPosition attackedSq;
	private Board theBoard;
	
	static private final GenericPosition [][][] directPieceMove_Lut = new GenericPosition [64][][];
	static {
		for (GenericPosition square : GenericPosition.values()) {
			int f = IntFile.valueOf(square.file);
			int r = IntRank.valueOf(square.rank);
			directPieceMove_Lut[f+(r*8)] = createDiagonalForSq(square);
		}
	};
	
	static private GenericPosition [][] createDiagonalForSq(GenericPosition square) {
		ArrayList<GenericPosition> squaresInDirection = new ArrayList<GenericPosition>();
		GenericPosition [][] ret = new GenericPosition [Direction.values().length][];
		int index = 0;
		for (Direction dir: Direction.values()) {
			squaresInDirection.addAll(getSqsInDirection(dir, square));
			ret[index] = squaresInDirection.toArray(new GenericPosition [0]);
			squaresInDirection.clear();
			index++;
		}
		return ret;
	}
	
	static private List<GenericPosition> getSqsInDirection(Direction dir, GenericPosition fromSq) {
		GenericPosition newSquare = fromSq;
		ArrayList<GenericPosition> sqsInDirection = new ArrayList<GenericPosition>();
		while ((newSquare = Direction.getDirectMoveSq(dir, newSquare)) != null) {
			sqsInDirection.add(newSquare);
		}
		return sqsInDirection;
	}
	
	public SquareAttackEvaluator( Board bd, GenericPosition atPos, Piece.Colour ownColour ) {
		attackingColour = Piece.Colour.getOpposite(ownColour);
		attackedSq = atPos;
		theBoard = bd;
	}
	
	public boolean isAttacked() {
		boolean attacked = false;
		boolean doKnightCheck = false;
		boolean doDiagonalCheck = false;
		boolean doRankFileCheck = false;
		Iterator<Piece> iter = theBoard.iterateColour(attackingColour);
		while (iter.hasNext()) {
			Piece curr = iter.next();
			if (curr instanceof Knight) {
				doKnightCheck = true;
			} else {
				if (curr instanceof Queen || curr instanceof Bishop) {
					doDiagonalCheck = true;
				}
				if (curr instanceof Queen || curr instanceof Rook) {
					doRankFileCheck = true;
				}
			}
		}
		// do/while loop is to allow the function to return attacked=true at earliest possibility
		do {
			if (attackingColour == Colour.black) {
				attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.upRight,attackedSq));
				if (attacked) break;
				attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.upLeft,attackedSq));
				if (attacked) break;
			} else {
				attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.downRight,attackedSq));
				if (attacked) break;
				attacked = attackedByPawn(Direction.getDirectMoveSq(Direction.downLeft,attackedSq));
				if (attacked) break;
			}
			attacked = checkForKingAttacks();
			if (attacked) break;
			if (doKnightCheck) {
				attacked = checkForKnightAttacks();
				if (attacked) break;
			}
			if (doDiagonalCheck || doRankFileCheck) {
				attacked = checkForDirectPieceAttacker(attackedSq);
				if (attacked) break;
			}
		} while (false);
		return attacked;	
	}

	private boolean checkForKnightAttacks() {
		boolean attacked = false;
		GenericPosition atPos;
		Piece currPiece;
		for (Direction dir: Direction.values()) {
			atPos = Direction.getIndirectMoveSq(dir, attackedSq);
			if (atPos != null) {
				currPiece = theBoard.getPieceAtSquare(atPos);
				if ( currPiece != null && currPiece instanceof Knight && currPiece.getColour()==attackingColour) {
					attacked = true;
					break;
				}
			}
		}
		return attacked;
	}

	private boolean checkForKingAttacks() {
		boolean attacked = false;
		GenericPosition atPos;
		Piece currPiece;
		for (Direction dir: Direction.values()) {
			atPos = Direction.getDirectMoveSq(dir, attackedSq);
			if (atPos != null) {
				currPiece = theBoard.getPieceAtSquare(atPos);
				if ( currPiece != null && currPiece instanceof King && currPiece.getColour()==attackingColour) {
					attacked = true;
					break;
				}
			}
		}
		return attacked;
	}	

	private boolean checkForDirectPieceAttacker(GenericPosition targetSq) {
		boolean attacked = false;
		int f = IntFile.valueOf(targetSq.file);
		int r = IntRank.valueOf(targetSq.rank);
		GenericPosition [][] array = SquareAttackEvaluator.directPieceMove_Lut[f+(r*8)];
		int index = 0;
		for (Direction dir: Direction.values()) { 
			for (GenericPosition attackerSq: array[index]) {
				Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
				if (currPiece != null ) {
					if (dir == Direction.downLeft || dir == Direction.upLeft || dir == Direction.upRight || dir == Direction.downRight) {
						if (((currPiece instanceof Bishop) || (currPiece instanceof Queen)) && currPiece.getColour()==attackingColour) {
							// Indicates attacked
							attacked = true;
						} // else blocked by own piece or non-attacking enemy
						break;
					} else if (dir == Direction.left || dir == Direction.up || dir == Direction.right || dir == Direction.down) {
						if (((currPiece instanceof Rook) || (currPiece instanceof Queen)) && currPiece.getColour()==attackingColour) {
							// Indicates attacked
							attacked = true;
						} // else blocked by own piece or non-attacking enemy
						break;
					}
				}
			}
			if (attacked) break;
			index++;
		}
		return attacked;
	}

	private boolean attackedByPawn(GenericPosition attackerSq) {
		Piece currPiece;
		boolean attacked = false;
		if (attackerSq != null) {
			currPiece = theBoard.getPieceAtSquare(attackerSq);
			if ( currPiece != null && currPiece instanceof Pawn && currPiece.getColour()==attackingColour) {
				attacked = true;
			}
		}
		return attacked;
	}
}
