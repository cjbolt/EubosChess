package eubos.position;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class MoveListIterator implements PrimitiveIterator.OfInt {

	private int [] moves = { Move.NULL_MOVE };
	private int next = 0;
	private int count = 0;
	
	public MoveAdderPromotions ma_promotions;
	public MoveAdderCaptures ma_captures;
	public QuietMovesWithNoKillers ma_quietNoKillers;
	public QuietMovesConsumingKillers ma_quietConsumeKillers;
		
	public class MoveListPly {
		// Internal state
		int[] moves;
		int moves_index;
		int nextCheckPoint;
		int generated_piece;
		boolean isWhite;

		// Cached data provided from PlySearcher
		boolean needToEscapeMate;
		boolean extendedSearch;
		boolean frontierNode;
		int bestMove;
		int[] killers;
		
		public MoveListPly()
		{
			moves = new int[110];
			generated_piece = BitBoard.INVALID;
		}
		
		public void initialise(int best, int[] killer_list, boolean inCheck, boolean extended, boolean frontier) {
			// Initialise working variables for building the MoveList at this ply
			needToEscapeMate = inCheck;
			extendedSearch = extended;
			killers = killer_list;
			bestMove = best;
			frontierNode = frontier;
			
			nextCheckPoint = 0;
			moves_index = 0;
			generated_piece = BitBoard.INVALID;
		}
	}
	
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();

	static class MoveTypeComparator implements IntComparator {
		@Override
		public int compare(int move1, int move2) {
			boolean lt = Move.getType(move1) < Move.getType(move2);
			boolean eq = Move.getType(move1) == Move.getType(move2);
			return lt ? 1 : (eq ? 0 : -1); // want list in descending order, hence opposite return values
		}
	}
	
	private PositionManager pm;
	private int ordering;
	MoveListPly state;
	int ply;
	MoveList ml;
	
	public MoveListIterator(MoveList ml, PositionManager pm, int orderMoveList, int ply) {
		this.pm = pm;
		ordering = orderMoveList;
		this.ply = ply;
		state = new MoveListPly();
		this.ml = ml;
		
		// Create Move Adders
		ma_promotions = new MoveAdderPromotions();
		ma_captures = new MoveAdderCaptures();
		ma_quietNoKillers = new QuietMovesWithNoKillers();
		ma_quietConsumeKillers = new QuietMovesConsumingKillers();
	}
	
	public MoveListIterator initialise(int best, int[] killer_list, boolean inCheck, boolean extended, boolean frontier) {
		state.initialise(best, killer_list, inCheck, extended, frontier);
		getNextMovesAtPly();
		return this;
	}

	public boolean hasNext() {
		if (next < count) {
			return true;
		} else {
			getNextMovesAtPly();
			return (next < count);
		}
	}

	public Integer next() {
		assert false; // use nextInt()
		return moves[next++];
	}

	@Override
	public void remove() {
	}

	@Override
	public void forEachRemaining(IntConsumer action) {
	}

	@Override
	public int nextInt() {
		if (hasNext()) {
			return moves[next++];
		} else {
			return Move.NULL_MOVE;
		}
	}

	private void set(int[] move_array, int length) {
		moves = move_array;
		count = length;
		next = 0;
	}
	
	private void set(int move) {
		moves[0] = move;
		count = 1;
		next = 0;
	}
	
	private void empty() {
		moves[0] = Move.NULL_MOVE;
		count = 0;
		next = 0;
	}
	
	public void getNextMovesAtPly() {
		state.moves_index = 0;

		switch (state.nextCheckPoint) {
		case 0:
			// Return best Move if valid
			state.nextCheckPoint = 1;
			state.isWhite = pm.onMoveIsWhite();
			if (isBestMoveValid()) {
				state.bestMove = Move.setBest(state.bestMove);
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert state.isWhite == Piece.isWhite(Move.getOriginPiece(state.bestMove));
				}
				set(state.bestMove);
				break;
			}
			// Note fall-through to next stage if no valid best move
		case 1:
			// Generate pawn promotions
			state.nextCheckPoint = 2;
			getPawnPromotions();
			if (state.moves_index != 0) {
				sortTacticalMoves();
				set(state.moves, state.moves_index);
				break;
			}
			// Note fall-through to next stage if no promotions
		case 2:
			// Generate all captures other than pawn promotions
			state.nextCheckPoint = 3;
			getNonPawnPromotionCaptures();
			if (state.moves_index != 0) {
				sortTacticalMoves();
				set(state.moves, state.moves_index);
				break;
			}
			// Note fall-through to next stage if no captures
		case 3:
			if (state.extendedSearch) {
				// Quiescent search shall terminate here
				empty();
				break;
			} else if (state.killers == null) {
//				// Fall-through into quiet moves if there are no killers
//				if (state.frontierNode) {
//					doSingleQuietMove();
//					break;
//				} else {
					doQuiet();
					break;
//				}
			} else {
				state.nextCheckPoint = 4;
				if (checkKiller(0)) break;
			}
			// Note fall-through to try next killer
		case 4:
			state.nextCheckPoint = 5;
			if (checkKiller(1)) break;
			// Note fall-through to try next killer
		case 5:
			state.nextCheckPoint = 6;
			if (checkKiller(2)) break;
			// Note fall-through to quiet moves
		case 6:
//			state.nextCheckPoint = 7;
//			if (state.frontierNode) {
//				doSingleQuietMove();
//				break;
//			}
		case 7:
			// Lastly, generate all quiet moves (i.e. that aren't best, killers, or tactical moves)
			state.nextCheckPoint = 8;
			doQuiet();
			break;
		default:
			empty();
			break;
		}
	}
	
	private void doSingleQuietMove() {
		getSingleQuietMove();
		if (state.moves_index != 0) {
			state.nextCheckPoint = 7;
			state.generated_piece = Move.getOriginPosition(state.moves[0]);
			set(state.moves, state.moves_index);
			return;
		}
		state.nextCheckPoint = 8;
		empty();
	}
	
	private void doQuiet() {
		state.nextCheckPoint = 8;
		getQuietMoves();
		if (state.moves_index != 0) {
			set(state.moves, state.moves_index);
			return;
		}
		empty();
	}

	private boolean checkKiller(int killerNum) {
		if (!Move.areEqual(state.bestMove, state.killers[killerNum]) && 
			pm.getTheBoard().isPlayableMove(state.killers[killerNum], state.needToEscapeMate, pm.castling)) {
			set(state.killers[killerNum]);
			return true;
		}
		return false;
	}

	private boolean isBestMoveValid() {
		if (state.bestMove != Move.NULL_MOVE) { 
			if ((!state.extendedSearch || isValidBestMoveForExtendedSearch()) &&
				(Move.isBest(state.bestMove) || bestMoveIsPlayable())) {
				return true;
			}
			state.bestMove = Move.NULL_MOVE; // If it wasn't valid, invalidate it
		}
		return false;
	}
	
	private boolean bestMoveIsPlayable() {
		return pm.getTheBoard().isPlayableMove(state.bestMove, state.needToEscapeMate, pm.castling);
	}
	
	private boolean isValidBestMoveForExtendedSearch() {
		return state.extendedSearch && 
			(Move.isQueenPromotion(state.bestMove) || Move.isCapture(state.bestMove));
	}

	private IAddMoves setupQuietMoveAdder() {
		IAddMoves moveAdder = null;
		if (state.killers == null) {
			ma_quietNoKillers.reset();
			moveAdder = ma_quietNoKillers;
		} else {
			ma_quietConsumeKillers.reset();
			moveAdder = ma_quietConsumeKillers;
		}
		return moveAdder;
	}
	
	private void getPawnPromotions() {
		pm.getTheBoard().getPawnPromotionMovesForSide(ma_promotions, state.isWhite);
	}

	private void getNonPawnPromotionCaptures() {
		pm.getTheBoard().getCapturesExcludingPromotions(ma_captures, state.isWhite);
	}
	
	private void getSingleQuietMove() {
		IAddMoves moveAdder = setupQuietMoveAdder();
		pm.getTheBoard().getSingleQuietMove(moveAdder, state.isWhite);
	}

	private void getQuietMoves() {
		IAddMoves moveAdder = setupQuietMoveAdder();
		if (state.frontierNode) {
			pm.getTheBoard().getRegularPieceMovesExceptingOnePiece(moveAdder, state.isWhite, state.generated_piece);
		} else {
			pm.getTheBoard().getRegularPieceMoves(moveAdder, state.isWhite);
		}
		if (!state.needToEscapeMate) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(state.isWhite, moveAdder);
		}
		sortQuietMoves();
	}

	private void sortQuietMoves() {
		IntArrays.quickSort(state.moves, 0, state.moves_index, ml.history.moveHistoryComparator);
	}
	
	private void sortTacticalMoves() {
		if (ply == 0) {
			// At the root node use different mechanisms for move ordering in different threads
			switch (ordering) {
			case 0:
				/* Don't order the move list in this case. */
				break;
			case 1:
				IntArrays.quickSort(state.moves, 0, state.moves_index, Move.mvvLvaComparator);
				break;
			case 2:
				IntArrays.reverse(state.moves, 0, state.moves_index);
				IntArrays.quickSort(state.moves, 0, state.moves_index, moveTypeComparator);
				break;
			case 3:
				IntArrays.reverse(state.moves, 0, state.moves_index);
				IntArrays.quickSort(state.moves, 0, state.moves_index, Move.mvvLvaComparator);
				break;
			case 4:
				IntArrays.quickSort(state.moves, 0, state.moves_index, moveTypeComparator);
				break;
			default:
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert false : String.format("Bad move ordering scheme %d!", ordering);
				break;
			}
		} else {
			// At all other nodes use MVV/LVA
			IntArrays.quickSort(state.moves, 0, state.moves_index, Move.mvvLvaComparator);
		}
	}	
	
	public class MoveAdderPromotions implements IAddMoves {
		public void addPrio(int move) {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert !Piece.isKing(Move.getTargetPiece(move));
			}
			if (Move.areEqual(move, state.bestMove)) {
				// Silently consume
			} else {
				state.moves[state.moves_index++] = move;
			}
			handleUnderPromotions(move);
		}

		public void addNormal(int move) {
		} // Doesn't deal with quiet moves by design

		public boolean isLegalMoveFound() {
			// This is only used by the legal move checker, which is for detecting quiescent positions
			return false;
		}

		@SuppressWarnings("unused")
		protected void handleUnderPromotions(int move) {
			if ((EubosEngineMain.ENABLE_PERFT || ply == 0) && Move.isQueenPromotion(move)) {
				// Add them in the order they will be sorted into
				int under1 = Move.setPromotion(move, Piece.BISHOP);
				int under2 = Move.setPromotion(move, Piece.KNIGHT);
				int under3 = Move.setPromotion(move, Piece.ROOK);
				state.moves[state.moves_index++] = under1;
				state.moves[state.moves_index++] = under2;
				state.moves[state.moves_index++] = under3;
			}
		}
	}

	public class MoveAdderCaptures extends MoveAdderPromotions implements IAddMoves {
		@Override
		public void addPrio(int move) {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert !Piece.isKing(Move.getTargetPiece(move));
			}
			if (Move.areEqual(move, state.bestMove)) {
				// Silently consume
			} else {
				state.moves[state.moves_index++] = move;
			}
		}
	}

	public class QuietMovesWithNoKillers extends MoveAdderPromotions implements IAddMoves {
		protected boolean legalQuietMoveAdded = false;
		
		@Override
		public void addPrio(int move) {
		} // Doesn't deal with tactical moves by design

		@Override
		public void addNormal(int move) {
			if (Move.areEqual(move, state.bestMove))
				return;
			state.moves[state.moves_index++] = move;
			legalQuietMoveAdded = true;
		}
		
		public void reset() {
			legalQuietMoveAdded = false;
			// Need to set move count correctly when we reset?
		}
		
		@Override
		public boolean isLegalMoveFound() {
			return legalQuietMoveAdded;
		}
	}

	public class QuietMovesConsumingKillers extends QuietMovesWithNoKillers implements IAddMoves {
		@Override
		public void addNormal(int move) {
			if (Move.areEqual(move, state.bestMove))
				return;
			if (KillerList.isMoveOnListAtPly(state.killers, move))
				return;
			state.moves[state.moves_index++] = move;
			legalQuietMoveAdded = true;
		}
	}
}
