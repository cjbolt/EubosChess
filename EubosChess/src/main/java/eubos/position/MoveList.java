package eubos.position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntChessman;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.board.Piece.Colour;

public class MoveList implements Iterable<Integer> {
	
	private List<Integer> normal_search_moves;
	private List<Integer> extended_search_moves;

	private int normalSearchBestMovePreviousIndex = -1;
	
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
		int moveType = Move.TYPE_REGULAR_NONE;
		
		boolean isCastle = (Piece.isKing(piece)) ? pm.lastMoveWasCastle() : false;
		boolean isCheck = pm.isKingInCheck(pm.getOnMove());
		
		// Check
		if (isCheck)
			moveType |= Move.TYPE_CHECK_MASK;
		
		if (isCastle) {
			// Castling (note: therefore excludes possibility of promotion or capture)
			moveType |= Move.TYPE_CASTLE_MASK;
			
		} else {
			int targetPiece = Move.getTargetPiece(currMove);
			
			// Promotions
			int promotion = Move.getPromotion(currMove);
			if (promotion == IntChessman.QUEEN)
				moveType |= Move.TYPE_PROMOTION_QUEEN_MASK;
			if (promotion == IntChessman.BISHOP || promotion == IntChessman.KNIGHT || promotion == IntChessman.ROOK)
				moveType |= Move.TYPE_PROMOTION_PIECE_MASK;
			
			// Captures
			switch (targetPiece & Piece.PIECE_NO_COLOUR_MASK) {
			case Piece.QUEEN:
				moveType |= Move.TYPE_CAPTURE_QUEEN_MASK;
				break;
			case Piece.BISHOP:
			case Piece.KNIGHT:
			case Piece.ROOK:
				moveType |= Move.TYPE_CAPTURE_PIECE_MASK;
				break;
			case Piece.PAWN:
				moveType |= Move.TYPE_CAPTURE_PAWN_MASK;
				break;
			default:
				break;
			}
		}		
		
		return moveType;		
	}
	
	public MoveList(PositionManager pm, int bestMove) {
		Colour onMove = pm.getOnMove();
		boolean needToEscapeMate = false;
		if (pm.lastMoveWasCheck() || (pm.noLastMove() && pm.isKingInCheck(onMove))) {
			needToEscapeMate = true;
		}
		List<Integer> allMoves = pm.generateMoves();
		ListIterator<Integer> it = allMoves.listIterator();
		while (it.hasNext()) {
			int currMove = it.next();
			try {
				boolean possibleDiscoveredOrMoveIntoCheck = false;
				int piece = pm.getTheBoard().getPieceAtSquare(Move.getOriginPosition(currMove));
				if (pm.getTheBoard().moveCouldLeadToOwnKingDiscoveredCheck(currMove) || Piece.isKing(piece)) {
					possibleDiscoveredOrMoveIntoCheck = true;
				}
				pm.performMove(currMove, false);
				if ((possibleDiscoveredOrMoveIntoCheck || needToEscapeMate) && pm.isKingInCheck(onMove)) {
					// Scratch any moves resulting in the king being in check, includes no escape moves!
					it.remove();
				} else {
					int moveType = computeMoveType(pm, currMove, piece);
					it.set(Move.setType(currMove, moveType));
				}
				pm.unperformMove(false);
			} catch(InvalidPieceException e) {
				assert false;
			}
		}
		// Sort the list
		Collections.sort(allMoves, new MoveTypeComparator());
		
		normal_search_moves = allMoves;
		
		if (bestMove != Move.NULL_MOVE) {
			seedListWithBestMove(normal_search_moves, bestMove);
		}
	}
		
	private List<Integer> create_extended_list(List<Integer> moves) {
		List<Integer> extendedMoves = new ArrayList<Integer>(moves.size());
		for (int move : moves ) {
			if (Move.isQueenPromotion(move) || Move.isCapture(move) || Move.isCheck(move)) {
				extendedMoves.add(move);
			}
		}
		return extendedMoves;
	}
	
	@Override
	public Iterator<Integer> iterator() {
		return normal_search_moves.iterator();
	}
	
	public Iterator<Integer> getStandardIterator(boolean extended) {
		Iterator<Integer> it;
		if (extended) {
			extended_search_moves = create_extended_list(normal_search_moves);
			seedListWithBestMove(extended_search_moves, normal_search_moves.get(0));
			it = extended_search_moves.iterator();
		} else {
			it = normal_search_moves.iterator();
		}
		return it; 
	}
		
	public boolean isMateOccurred() {
		return (normal_search_moves.size() == 0);
	}
	
	public GenericMove getRandomMove() {
		GenericMove bestMove = null;
		if (!isMateOccurred()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(normal_search_moves.size());
			bestMove = Move.toGenericMove(normal_search_moves.get(indexToGet));		
		}
		return bestMove;
	}
	
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

	public int getBestMove() {
		if (normal_search_moves.size() != 0) {
			return normal_search_moves.get(0);
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	public int getSafestMove() {
		if (normal_search_moves.size() != 0) {
			/* The logic is to avoid checks, which will be in the highest prio indexes of the ml */
			return normal_search_moves.get(normal_search_moves.size()-1);
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	private int getIndex(List<Integer> moves, int move) {
		int index = -1;
		int count = 0;
		for (int currMove : moves) {
			if (Move.areEqual(currMove, move)) {
				index = count;
				break;
			} else {
				count++;
			}
		}
		return index;
	}
	
	private void seedListWithBestMove(List<Integer> moves, int newBestMove) {
		if (moves.size() != 0) {
			int index = getIndex(moves, newBestMove);
			if (index > 0) {
				int prevBest = moves.get(0);
				moves.set(0, moves.get(index));
				moves.set(index, prevBest);
			}
		}
	}
	
	private void swapWithFirst(List<Integer> moves, int index) {
		if (moves.size() != 0) {
			if (index > 0) {
				int prevBest = moves.get(0);
				moves.set(0, moves.get(index));
				moves.set(index, prevBest);
			}
		}
	}
	
	private int reorderList(List<Integer> moveArray, int newBestMove, int prevBestOriginalIndex) {
		int index = getIndex(moveArray, newBestMove);
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
	
	// Test API
	boolean contains(int move) {
		for (int reg_move : normal_search_moves) {
			if (move == reg_move)
				return true;
		}
		return false;
	}
	
	public void reorderWithNewBestMove(int newBestMove) {
		normalSearchBestMovePreviousIndex = reorderList(normal_search_moves, newBestMove, normalSearchBestMovePreviousIndex);
	}
}
