package eubos.position;

import java.util.Arrays;
import java.util.Comparator;
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
	
	/* Not currently used because we sort by ascending numerical order using Arrays.sort() */
    class MoveTypeComparator implements Comparator<Integer> {
        @Override public int compare(Integer move1, Integer move2) {
            boolean gt = Move.getType(move1) < Move.getType(move2);
            boolean eq = Move.getType(move1) == Move.getType(move2);
            return gt ? 1 : (eq ? 0 : -1);
        }
    }
	
	public MoveList(PositionManager pm) {
		this(pm, Move.NULL_MOVE);
	}
	
	int computeMoveType(PositionManager pm, int currMove, int piece) {
		int moveType = Move.TYPE_NONE;
		CaptureData cap = pm.getCapturedPiece();
		boolean isCapture = (cap != null && cap.target != Piece.NONE);
		boolean isCheck = pm.isKingInCheck(pm.getOnMove());
		boolean isCastle = (Piece.isKing(piece)) ? pm.lastMoveWasCastle() : false;
		
		// Promotions
		int promotion = Move.getPromotion(currMove);
		if (promotion == IntChessman.QUEEN)
			moveType |= Move.TYPE_PROMOTION_QUEEN_MASK;
		if (promotion == IntChessman.ROOK)
			moveType |= Move.TYPE_PROMOTION_ROOK_MASK;
		if (promotion == IntChessman.BISHOP || promotion == IntChessman.KNIGHT)
			moveType |= Move.TYPE_PROMOTION_PIECE_MASK;

		// Captures
		if (isCapture) {
			if (Piece.isQueen(cap.target)) {
				moveType |= Move.TYPE_CAPTURE_QUEEN_MASK;
			} else if (Piece.isRook(cap.target)) {
				moveType |= Move.TYPE_CAPTURE_ROOK_MASK;
			} else if (Piece.isKnight(cap.target) || Piece.isBishop(cap.target)) {
				moveType |= Move.TYPE_CAPTURE_PIECE_MASK;
			} else if (Piece.isPawn(cap.target)) {
				moveType |= Move.TYPE_CAPTURE_PAWN_MASK;
			}
		}
		
		// Check
		if (isCheck)
			moveType |= Move.TYPE_CHECK_MASK;
		
		// Castle
		if (isCastle)
			moveType |= Move.TYPE_CASTLE_MASK;
		
		// Regular
		if (moveType == Move.TYPE_NONE)
			moveType |= Move.TYPE_REGULAR_MASK;
		
		return moveType;		
	}
	
	public MoveList(PositionManager pm, int bestMove) {
		Colour onMove = pm.getOnMove();
		boolean needToEscapeMate = false;
		if (pm.lastMoveWasCheck() || (pm.noLastMove() && pm.isKingInCheck(onMove))) {
			needToEscapeMate = true;
		}
		int [] allMoves = pm.generateMoves();
		int [] outputMove = new int [allMoves.length];
		int count = 0;
		for (int currMove : allMoves) {
			try {
				boolean possibleDiscoveredOrMoveIntoCheck = false;
				int piece = pm.getTheBoard().getPieceAtSquare(Move.getOriginPosition(currMove));
				if (pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove) || Piece.isKing(piece)) {
					possibleDiscoveredOrMoveIntoCheck = true;
				}
				pm.performMove(currMove);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, includes no escape moves!
				} else {
					int moveType = computeMoveType(pm, currMove, piece);
					outputMove[count++] = Move.setType(currMove, moveType);
				}
				pm.unperformMove();
			} catch(InvalidPieceException e) {
				assert false;
			}
		}
		// Sort the list
		Arrays.sort(outputMove);
		
		// copy and flip order
		int [] moves = new int[count];
		for (int i=0; i < count; i++) {
			moves[i] = outputMove[outputMove.length-1-i];
		}
		
		normal_search_moves = moves;
		extended_search_moves = create_extended_list(moves);
		
		if (bestMove != Move.NULL_MOVE) {
			seedListWithBestMove(normal_search_moves, bestMove);
			seedListWithBestMove(extended_search_moves, bestMove);
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
		
	private int[] create_extended_list(int[] moves) {
		int[] countList = new int[moves.length];
		int count = 0;
		for (int move : moves ) {
			if (Move.isQueenPromotion(move) || Move.isCapture(move) || Move.isCheck(move)) {
				countList[count++] = move;
			}
		}
		// copy
		int[] array = new int[count];
		for (int i=0; i<count; i++) {
			array[i] = countList[i];
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
			assert false; // should always use nextInt()
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

	public void reorderWithNewBestMove(int newBestMove) {
		normalSearchBestMovePreviousIndex = reorderList(normal_search_moves, newBestMove, normalSearchBestMovePreviousIndex);
		extendedSearchListBestMovePreviousIndex = reorderList(extended_search_moves, newBestMove, extendedSearchListBestMovePreviousIndex);
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

	public boolean hasMultipleRegularMoves() {
		int count = 0;
		for (int move : normal_search_moves) {
			if (Move.isRegular(move)) {
				count++;
				if (count == 2 )
					return true;
			}
		}
		return false;
	}
	
	public boolean contains(int move) {
		for (int reg_move : normal_search_moves) {
			if (move == reg_move)
				return true;
		}
		return false;
	}

	public int getBestMove() {
		if (normal_search_moves.length != 0) {
			return normal_search_moves[0];
		} else {
			return Move.NULL_MOVE;
		}
	}
}
