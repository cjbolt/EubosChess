 package eubos.board;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;

public class SquareAttackEvaluator {
	
	static private final TreeMap<GenericPosition, GenericPosition[][]> directPieceMove_Lut = new TreeMap<GenericPosition, GenericPosition[][]>();
	static {
		for (GenericPosition square : GenericPosition.values()) {
			directPieceMove_Lut.put(square, createDiagonalForSq(square));
		}
	}
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
	
	static private final TreeMap<GenericPosition, GenericPosition[]> KnightMove_Lut = new TreeMap<GenericPosition, GenericPosition[]>();
	static {
		for (GenericPosition square : GenericPosition.values()) {
			KnightMove_Lut.put(square, createKnightMovesAtSq(square));
		}
	}
	static GenericPosition [] createKnightMovesAtSq(GenericPosition atPos) {
		ArrayList<GenericPosition> list = new ArrayList<GenericPosition>();
		for (Direction dir: Direction.values()) {
			GenericPosition sq = Direction.getIndirectMoveSq(dir, atPos);
			if (sq != null) {
				list.add(sq);
			}
		}
		GenericPosition[] array = new GenericPosition[list.size()];
		return list.toArray(array);
	}
	
	static private final TreeMap<GenericPosition, GenericPosition[]> KingMove_Lut = new TreeMap<GenericPosition, GenericPosition[]>();
	static {
		for (GenericPosition square : GenericPosition.values()) {
			KingMove_Lut.put(square, createKingMovesAtSq(square));
		}
	}
	static GenericPosition [] createKingMovesAtSq(GenericPosition atPos) {
		ArrayList<GenericPosition> list = new ArrayList<GenericPosition>();
		for (Direction dir: Direction.values()) {
			GenericPosition sq = Direction.getDirectMoveSq(dir, atPos);
			if (sq != null) {
				list.add(sq);
			}
		}
		GenericPosition[] array = new GenericPosition[list.size()];
		return list.toArray(array);
	}
	
	public static boolean isAttacked( Board bd, GenericPosition attackedSq, Piece.Colour ownColour ) {
		Colour attackingColour = Piece.Colour.getOpposite(ownColour);
		boolean attacked = false;
		boolean doKnightCheck = false;
		boolean doDiagonalCheck = false;
		boolean doRankFileCheck = false;
		Iterator<Piece> iter = bd.iterateColour(attackingColour);
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
				attacked = attackedByPawn(bd, attackingColour, Direction.getDirectMoveSq(Direction.upRight,attackedSq));
				if (attacked) break;
				attacked = attackedByPawn(bd, attackingColour, Direction.getDirectMoveSq(Direction.upLeft,attackedSq));
				if (attacked) break;
			} else {
				attacked = attackedByPawn(bd, attackingColour, Direction.getDirectMoveSq(Direction.downRight,attackedSq));
				if (attacked) break;
				attacked = attackedByPawn(bd, attackingColour, Direction.getDirectMoveSq(Direction.downLeft,attackedSq));
				if (attacked) break;
			}
			attacked = checkForKingAttacks(bd, attackingColour, attackedSq);
			if (attacked) break;
			if (doKnightCheck) {
				attacked = checkForKnightAttacks(bd, attackingColour, attackedSq);
				if (attacked) break;
			}
			if (doDiagonalCheck || doRankFileCheck) {
				attacked = checkForDirectPieceAttacker(bd, attackingColour, attackedSq);
				if (attacked) break;
			}
		} while (false);
		return attacked;	
	}

	private static boolean checkForKnightAttacks(Board theBoard, Colour attackingColour, GenericPosition attackedSq) {
		boolean attacked = false;
		GenericPosition [] array = KnightMove_Lut.get(attackedSq);
		for (GenericPosition attackerSq: array) {
			Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
			if (currPiece != null && currPiece instanceof Knight && currPiece.getColour()==attackingColour) {
				attacked = true;
				break;
			}
		}
		return attacked;
	}

	private static boolean checkForKingAttacks(Board theBoard, Colour attackingColour, GenericPosition attackedSq) {
		boolean attacked = false;
		GenericPosition [] array = KingMove_Lut.get(attackedSq);
		for (GenericPosition attackerSq: array) {
			Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
			if (currPiece != null && currPiece instanceof King && currPiece.getColour()==attackingColour) {
				attacked = true;
				break;
			}
		}
		return attacked;
	}	

	private static boolean checkForDirectPieceAttacker(Board theBoard, Colour attackingColour, GenericPosition targetSq) {
		boolean attacked = false;
		GenericPosition [][] array = SquareAttackEvaluator.directPieceMove_Lut.get(targetSq);
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

	private static boolean attackedByPawn(Board theBoard, Colour attackingColour, GenericPosition attackerSq) {
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
