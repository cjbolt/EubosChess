package eubos.position;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.function.IntConsumer;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntChessman;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.board.Piece.Colour;

public class MoveList implements Iterable<Integer> {
	
	private int[] normal_search_moves;
	private int[] extended_search_moves;
	private byte normalSearchBestMovePreviousIndex = -1;
	private byte extendedSearchListBestMovePreviousIndex = -1;
	
    class MoveTypeComparator implements Comparator<Integer> {
        @Override public int compare(Integer move1, Integer move2) {
            boolean gt = Move.getType(move1) > Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm) {
		this(pm, null);
	}
	
	public MoveList(PositionManager pm, GenericMove bestMove) {
		// N.b. Need to use a linked hash map to ensure that the search order is deterministic.
		List<Integer> moveList = new LinkedList<Integer>();
		Colour onMove = pm.getOnMove();
		boolean needToEscapeMate = false;
		if (pm.isKingInCheck(onMove)) {
			needToEscapeMate = true;
		}
		for (Integer currMove : pm.generateMoves()) {
			try {
				boolean possibleDiscoveredOrMoveIntoCheck = false;
				int piece = pm.getTheBoard().getPieceAtSquare(Move.getOriginPosition(currMove));
				if (pm.getTheBoard().moveCouldLeadToDiscoveredCheck(currMove) || Piece.isKing(piece)) {
					possibleDiscoveredOrMoveIntoCheck = true;
				}
				pm.performMove(currMove);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, includes no escape moves!
				} else {
					int moveType;
					boolean isQueenPromotion = (Move.getPromotion(currMove) == IntChessman.QUEEN);
					boolean isPromotion = (Move.getPromotion(currMove) == IntChessman.BISHOP
							|| Move.getPromotion(currMove) == IntChessman.ROOK
							|| Move.getPromotion(currMove) == IntChessman.KNIGHT);
					CaptureData cap = pm.getCapturedPiece();
					boolean isCapture = (cap != null && cap.target != Piece.NONE);
					boolean isCheck = pm.isKingInCheck(Colour.getOpposite(onMove));
					boolean isCastle = (Piece.isKing(piece)) ? pm.lastMoveWasCastle() : false;
					
					if (isQueenPromotion && isCapture && isCheck) {
						moveType = Move.TYPE_PROMOTION_AND_CAPTURE_WITH_CHECK;
					} else if (isQueenPromotion && isCapture) {
						moveType = Move.TYPE_PROMOTION_AND_CAPTURE;
					} else if (isQueenPromotion) {
						moveType = Move.TYPE_PROMOTION;
					} else if (isPromotion) {
						moveType = Move.TYPE_KBR_PROMOTION;
					} else if (isCapture && isCheck) {
						moveType = Move.TYPE_CAPTURE_WITH_CHECK;
					} else if (isCapture && Piece.isQueen(cap.target)) {
						moveType = Move.TYPE_CAPTURE_QUEEN;
					} else if (isCapture && Piece.isRook(cap.target)) {
						moveType = Move.TYPE_CAPTURE_ROOK;
					} else if (isCapture && (Piece.isKnight(cap.target) || Piece.isBishop(cap.target))) {
						moveType = Move.TYPE_CAPTURE_PIECE;
					} else if (isCapture && Piece.isPawn(cap.target)) {
						moveType = Move.TYPE_CAPTURE_PAWN;
					} else if (isCastle) {
						moveType = Move.TYPE_CASTLE;
					} else if (isCheck) {
						moveType = Move.TYPE_CHECK;
					}  else {
						moveType = Move.TYPE_REGULAR;
					}
					moveList.add(Move.setType(currMove, moveType));
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
				assert false;
			}
		}
		
		// Sort the list
		Collections.sort(moveList, new MoveTypeComparator());
		normal_search_moves = create_normal_list(moveList);
		extended_search_moves = create_extended_list(moveList);
		
		int intBestMove = 0;
		if (bestMove != null) {
			for (Integer move : moveList ) {
				if (Move.toGenericMove(move).equals(bestMove)) {
					// the moves are the same, so set the type of the best move from the existing move
					intBestMove = Move.toMove(bestMove, pm.getTheBoard(), Move.getType(move));
					break;
				}
			}
			seedListWithBestMove(normal_search_moves, intBestMove);
			seedListWithBestMove(extended_search_moves, intBestMove);
		}
	}
	
	private byte getIndex(int[] moveArray, int move) {
		byte index = 0;
		boolean found = false;
		for (int current : moveArray) {
			if (Move.areEqual(move,current)) {
				found = true;
				break;	
			}
			index++;
		}
		if (!found) {
			index = -1;
		}
		return index;
	}
	
	private void swapWithFirst(int[] moveArray, int index) {
		if (isMovePresent(index) && index < moveArray.length) {
			int temp = moveArray[0];
			moveArray[0] = moveArray[index];
			moveArray[index] = temp;
		}
	}
	
	private void seedListWithBestMove(int[] moveArray, int newBestMove) {
		int index = getIndex(moveArray, newBestMove);
	    swapWithFirst(moveArray, index);
	}
	
	private int [] create_normal_list(List<Integer> moves) {
		int [] moveArray = new int[moves.size()];
		int index = 0;
		for (Integer move : moves ) {
			moveArray[index++] = move;
		}
		return moveArray;
	}
	
	private int[] create_extended_list(List<Integer> moves) {
		List<Integer> list = new LinkedList<Integer>();
		for (Integer move : moves ) {
			if ((Move.getType(move) == Move.TYPE_PROMOTION) ||
				(Move.getType(move) == Move.TYPE_PROMOTION_AND_CAPTURE_WITH_CHECK) ||
				(Move.getType(move) == Move.TYPE_PROMOTION_AND_CAPTURE) ||
				(Move.getType(move) == Move.TYPE_KBR_PROMOTION) ||
				(Move.getType(move) == Move.TYPE_CAPTURE_WITH_CHECK) ||
				(Move.getType(move) == Move.TYPE_CAPTURE_QUEEN) ||
				(Move.getType(move) == Move.TYPE_CAPTURE_ROOK) ||
				(Move.getType(move) == Move.TYPE_CAPTURE_PIECE) ||
				(Move.getType(move) == Move.TYPE_CAPTURE_PAWN) ||
				(Move.getType(move) == Move.TYPE_CHECK)) {
				list.add(move);
			}
		}
		int[] array = new int[list.size()];
		for (int i=0; i<array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public class MovesIterator implements PrimitiveIterator.OfInt {

		private int[] moveList = null;
		private int next = 0;
	
		public MovesIterator(int[] array) {
			moveList = array.clone();
			next = 0;
		}

		public boolean hasNext() {
			return next < moveList.length;
		}

		public Integer next() {
			return moveList[next++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
			
		}

		@Override
		public int nextInt() {
			return moveList[next++];
		}
	}
	
	public PrimitiveIterator.OfInt getIterator(boolean extended) {
		return new MovesIterator(extended ? extended_search_moves : normal_search_moves);
	}
	
	@Override
	public PrimitiveIterator.OfInt iterator() {
		return new MovesIterator(normal_search_moves);
	}
		
	public boolean isMateOccurred() {
		return (normal_search_moves.length == 0);
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if (!isMateOccurred()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_search_moves.length);
			bestMove = Move.toGenericMove(normal_search_moves[indexToGet]);		
		}
		return bestMove;
	}
	
	public int getMoveTypeFromNormalList(GenericMove genericMove) {
		int type = Move.TYPE_KBR_PROMOTION;
		boolean found = false;
		for (int move : normal_search_moves) {
			if (Move.getOriginPosition(move) == Position.valueOf(genericMove.from)
				&& Move.getTargetPosition(move) == Position.valueOf(genericMove.to)) {
				if (genericMove.promotion != null && genericMove.promotion.isLegalPromotion()) {
					if (IntChessman.valueOfPromotion(genericMove.promotion) == Move.getPromotion(move)) {
						type = Move.getType(move);
						found = true;
						break;
					}
				} else {
					type = Move.getType(move);
					found = true;
					break;
				}
			}
		}
		assert found : genericMove;
		return type;
	}
	
	public void reorderWithNewBestMove(GenericMove newBestMove) {
		// Only used by tests
		int move = Move.toMove(newBestMove, null, getMoveTypeFromNormalList(newBestMove));
		reorderWithNewBestMove(move);
	}

	public void reorderWithNewBestMove(int newBestMove) {
		normalSearchBestMovePreviousIndex = reorderList(normal_search_moves, newBestMove, normalSearchBestMovePreviousIndex);
		extendedSearchListBestMovePreviousIndex = reorderList(extended_search_moves, newBestMove, extendedSearchListBestMovePreviousIndex);
	}
	
	int[] reorderListShuffle(int[] moveArray, int newBestMove) {
		int index = getIndex(moveArray, newBestMove);
		if (isMovePresent(index) && !isMoveAlreadyBest(index)) {
			int [] newMoveArray = moveArray.clone();
			newMoveArray[0] = moveArray[index];
			for(int i=0; i < (moveArray.length-1); i++) {
				if (i != index) {
					newMoveArray[i+1] = moveArray[i];
				}
			}
			return newMoveArray;
		}
		return moveArray;
	}
	
	private byte reorderList(int[] moveArray, int newBestMove, byte prevBestOriginalIndex) {
		byte index = getIndex(moveArray, newBestMove);
		if (isMovePresent(index) && !isMoveAlreadyBest(index)) {
			
			if (wasPreviouslyModified(prevBestOriginalIndex)) {
				// Swap back the previous best move into its previous position, if this isn't a direct swap
				if (prevBestOriginalIndex == index) {
					// It is a direct swap back, set to -1 as the list is restored to its initial state
					prevBestOriginalIndex = -1;
				} else {
					swapWithFirst(moveArray, prevBestOriginalIndex);
					prevBestOriginalIndex = index;
				}
			} else {
				// Initialise the previous best index the first time the best move is altered
				prevBestOriginalIndex = index;
			}
			
			// Swap in the new best move
			swapWithFirst(moveArray, index);
		}
		return prevBestOriginalIndex;
	}
	
	private boolean isMovePresent(int index) { return (index != -1); }
	
	private boolean isMoveAlreadyBest(int index) { return (index == 0); }
	
	private boolean wasPreviouslyModified(int index) { return index != -1; }
	
	@Override
	public String toString() {
		String retVal = "";
		for (int move : this.normal_search_moves) {
			retVal += Move.toString(move);
			retVal += ", ";
		}
		return retVal;
	}
}
