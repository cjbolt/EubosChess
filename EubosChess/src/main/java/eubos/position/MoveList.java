package eubos.position;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;
import com.fluxchess.jcpi.models.IntFile;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class MoveList implements Iterable<Integer> {
	
	public class MoveListPly {
		// Internal state
		int[] normal_search_moves;
		int[] priority_moves;
		int normal_fill_index;
		int priority_fill_index;
		int moveCount;
		int nextCheckPoint;
		int generated_piece;
		boolean isWhite;

		// Cached data provided from PlySearcher
		boolean needToEscapeMate;
		boolean extendedSearch;
		int bestMove;
		int[] killers;
		
		public MoveListPly()
		{
			normal_search_moves = new int[110];
			priority_moves = new int[100];
			generated_piece = BitBoard.INVALID;
		}
		
		public void initialise(int best, int[] killer_list, boolean inCheck, boolean extended) {
			// Initialise working variables for building the MoveList at this ply
			needToEscapeMate = inCheck;
			extendedSearch = extended;
			killers = killer_list;
			bestMove = best;
			
			nextCheckPoint = 0;
			moveCount = 0;
			normal_fill_index = 0;
			priority_fill_index = 0;
			generated_piece = BitBoard.INVALID;
		}
	}
	
	private static final MoveTypeComparator moveTypeComparator = new MoveTypeComparator();

	static class MoveTypeComparator implements IntComparator {
		@Override
		public int compare(int move1, int move2) {
			boolean gt = Move.getType(move1) < Move.getType(move2);
			boolean eq = Move.getType(move1) == Move.getType(move2);
			return gt ? 1 : (eq ? 0 : -1);
		}
	}
	
	private static MoveListIterator empty = new MoveListIterator(new int [] {}, 0);

	private MoveListPly[] state;
	private int ply;
	private MoveListIterator[] ml;

	private PositionManager pm;
	private int ordering;

	public MoveAdderPromotions ma_promotions;
	public MoveAdderCaptures ma_captures;
	public QuietMovesWithNoKillers ma_quietNoKillers;
	public QuietMovesConsumingKillers ma_quietConsumeKillers;

	public MoveList(PositionManager pm, int orderMoveList) {
		setupMoveListArrays();
		this.pm = pm;
		ordering = orderMoveList;

		// Create Move Adders
		ma_promotions = new MoveAdderPromotions();
		ma_captures = new MoveAdderCaptures();
		ma_quietNoKillers = new QuietMovesWithNoKillers();
		ma_quietConsumeKillers = new QuietMovesConsumingKillers();
	}

	public void initialiseAtPly(int bestMove, int[] killers, boolean inCheck, boolean extended, int ply) {
		state[ply].initialise(bestMove, killers, inCheck, extended);
	}
	
	public MoveListIterator getNextMovesAtPly(int ply) {
		MoveListIterator iter = null;
		this.ply = ply;
		state[ply].normal_fill_index = 0;
		state[ply].priority_fill_index = 0;
		state[ply].moveCount = 0;

		switch (state[ply].nextCheckPoint) {
		case 0:
			// Return best Move if valid
			state[ply].nextCheckPoint = 1;
			if (isBestMoveValid()) {
				state[ply].bestMove = Move.setBest(state[ply].bestMove);
				return singleMoveIterator(state[ply].bestMove);
			}
			// Note fall-through to next stage if no valid best move
		case 1:
			// Generate pawn promotions
			state[ply].isWhite = pm.onMoveIsWhite();
			state[ply].nextCheckPoint = 2;
			getPawnPromotions();
			if (state[ply].moveCount != 0) {
				return priorityIterator();
			}
			// Note fall-through to next stage if no promotions
		case 2:
			// Generate all captures other than pawn promotions
			state[ply].nextCheckPoint = 3;
			getNonPawnPromotionCaptures();
			if (state[ply].moveCount != 0) {
				return priorityIterator();
			}
			// Note fall-through to next stage if no captures
		case 3:
			if (state[ply].extendedSearch) {
				// Quiescent search shall terminate here
				return emptyIterator();
			} else if (state[ply].killers == null) {
				// Fall-through into quiet moves if there are no killers
				return doSingleQuietMove();
			} else {
				state[ply].nextCheckPoint = 4;
				iter = checkKiller(0);
				if (iter != null) {
					return iter;
				}
			}
			// Note fall-through to try next killer
		case 4:
			state[ply].nextCheckPoint = 5;
			iter = checkKiller(1);
			if (iter != null) {
				return iter;
			}
			// Note fall-through to try next killer
		case 5:
			state[ply].nextCheckPoint = 6;
			iter = checkKiller(2);
			if (iter != null) {
				return iter;
			}
			// Note fall-through to quiet moves
		case 6:
			state[ply].nextCheckPoint = 7;
			return doSingleQuietMove();
		case 7:
			// Lastly, generate all quiet moves (i.e. that aren't best, killers, or tactical moves)
			state[ply].nextCheckPoint = 8;
			return doQuiet();
		default:
			return emptyIterator();
		}
	}
	
	private void setupMoveListArrays() {
		// Create the move list arrays for this threads move list
		state = new MoveListPly[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		ml = new MoveListIterator[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
	
		// Create the list at each ply
		for (int i = 0; i < EubosEngineMain.SEARCH_DEPTH_IN_PLY; i++) {
			state[i] = new MoveListPly();
			ml[i] = new MoveListIterator();
		}
	}
	
	private MoveListIterator doSingleQuietMove() {
		if (getSingleQuietMove()) {
			state[ply].nextCheckPoint = 7;
			state[ply].generated_piece = Move.getOriginPosition(state[ply].normal_search_moves[0]);
			return iterator();
		} else {
			state[ply].nextCheckPoint = 8;
			return emptyIterator();
		}
	}
	
	private MoveListIterator doQuiet() {
		state[ply].nextCheckPoint = 8;
		getQuietMoves();
		if (state[ply].moveCount != 0) {
			return iterator();
		}
		return empty;
	}

	private MoveListIterator checkKiller(int killerNum) {
		MoveListIterator iter = null;
		if (!Move.areEqualForBestKiller(state[ply].bestMove, state[ply].killers[killerNum])
				&& pm.getTheBoard().isPlayableMove(state[ply].killers[killerNum], state[ply].needToEscapeMate, pm.castling)) {
			iter = singleMoveIterator(state[ply].killers[killerNum]);
		}
		return iter;
	}

	private boolean isBestMoveValid() {
		if (state[ply].bestMove != Move.NULL_MOVE) { 
			if ((!state[ply].extendedSearch || isValidBestMoveForExtendedSearch()) &&
				(Move.isBest(state[ply].bestMove) || bestMoveIsPlayable())) {
				return true;
			}
			state[ply].bestMove = Move.NULL_MOVE; // If it wasn't valid, invalidate it
		}
		return false;
	}
	
	private boolean bestMoveIsPlayable() {
		return pm.getTheBoard().isPlayableMove(state[ply].bestMove, state[ply].needToEscapeMate, pm.castling);
	}
	
	private boolean isValidBestMoveForExtendedSearch() {
		return state[ply].extendedSearch && 
			(Move.isQueenPromotion(state[ply].bestMove) || Move.isCapture(state[ply].bestMove));
	}

	private void getPawnPromotions() {
		pm.getTheBoard().getPawnPromotionMovesForSide(ma_promotions, state[ply].isWhite);
	}

	private void getNonPawnPromotionCaptures() {
		pm.getTheBoard().getCapturesExcludingPromotions(ma_captures, state[ply].isWhite);
	}
	
	private boolean getSingleQuietMove() {
		IAddMoves moveAdder = null;
		if (state[ply].killers == null) {
			ma_quietNoKillers.reset();
			moveAdder = ma_quietNoKillers;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority
			// part of the move list
			ma_quietConsumeKillers.reset();
			moveAdder = ma_quietConsumeKillers;
		}
		return pm.getTheBoard().getSingleQuietMove(moveAdder, state[ply].isWhite);
	}

	private void getQuietMoves() {
		IAddMoves moveAdder = null;
		if (state[ply].killers == null) {
			ma_quietNoKillers.reset();
			moveAdder = ma_quietNoKillers;
		} else {
			// Set-up move adder to filter the moves from attacked pieces into the priority
			// part of the move list
			ma_quietConsumeKillers.reset();
			moveAdder = ma_quietConsumeKillers;
		}
		if (state[ply].generated_piece == BitBoard.INVALID) {
			pm.getTheBoard().getRegularPieceMoves(moveAdder, state[ply].isWhite);
		} else {
			pm.getTheBoard().getRegularPieceMovesExceptingOnePiece(moveAdder, state[ply].isWhite, state[ply].generated_piece);
		}
		if (!state[ply].needToEscapeMate) {
			// Can't castle out of check and don't care in extended search
			pm.castling.addCastlingMoves(state[ply].isWhite, moveAdder);
		}
	}

	private void sortPriorityList() {
		if (ply == 0) {
			// At the root node use different mechanisms for move ordering in different threads
			switch (ordering) {
			case 0:
				/* Don't order the move list in this case. */
				break;
			case 1:
				IntArrays.quickSort(state[ply].priority_moves, 0, state[ply].priority_fill_index, Move.mvvLvaComparator);
				break;
			case 2:
				IntArrays.reverse(state[ply].priority_moves, 0, state[ply].priority_fill_index);
				IntArrays.quickSort(state[ply].priority_moves, 0, state[ply].priority_fill_index, moveTypeComparator);
				break;
			case 3:
				IntArrays.reverse(state[ply].priority_moves, 0, state[ply].priority_fill_index);
				IntArrays.quickSort(state[ply].priority_moves, 0, state[ply].priority_fill_index, Move.mvvLvaComparator);
				break;
			case 4:
				IntArrays.quickSort(state[ply].priority_moves, 0, state[ply].priority_fill_index, moveTypeComparator);
				break;
			default:
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert false : String.format("Bad move ordering scheme %d!", ordering);
				break;
			}
		} else {
			// At all other nodes use MVV/LVA
			IntArrays.quickSort(state[ply].priority_moves, 0, state[ply].priority_fill_index, Move.mvvLvaComparator);
		}
	}

	@Override
	public MoveListIterator iterator() {
		return ml[ply].set(state[ply].normal_search_moves, state[ply].moveCount);
	}
	
	public MoveListIterator emptyIterator() {
		return empty;
	}
	
	public MoveListIterator priorityIterator() {
		sortPriorityList();
		return ml[ply].set(state[ply].priority_moves, state[ply].priority_fill_index);
	}

	public MoveListIterator singleMoveIterator(int move) {
		return ml[ply].set(move);
	}

	public int getRandomMove() {
		int randomMove = Move.NULL_MOVE;
		return randomMove;
	}

	public class MoveAdderPromotions implements IAddMoves {
		public void addPrio(int move) {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert !Piece.isKing(Move.getTargetPiece(move));
			}
			if (!pm.getTheBoard().isIllegalMove(move, state[ply].needToEscapeMate)) {
				if (Move.areEqualForBestKiller(move, state[ply].bestMove)) {
					// Silently consume
				} else {
					state[ply].priority_moves[state[ply].priority_fill_index++] = move;
					state[ply].moveCount++;
				}
				handleUnderPromotions(move);
			}
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
				state[ply].priority_moves[state[ply].priority_fill_index++] = under1;
				state[ply].priority_moves[state[ply].priority_fill_index++] = under2;
				state[ply].priority_moves[state[ply].priority_fill_index++] = under3;
				state[ply].moveCount += 3;
			}
		}
	}

	public class MoveAdderCaptures extends MoveAdderPromotions implements IAddMoves {
		@Override
		public void addPrio(int move) {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert !Piece.isKing(Move.getTargetPiece(move));
			}
			if (!pm.getTheBoard().isIllegalMove(move, state[ply].needToEscapeMate)) {
				state[ply].priority_moves[state[ply].priority_fill_index++] = move;
				state[ply].moveCount++;
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
			if (Move.areEqualForBestKiller(move, state[ply].bestMove))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, state[ply].needToEscapeMate)) {
				state[ply].normal_search_moves[state[ply].normal_fill_index++] = move;
				state[ply].moveCount++;
				legalQuietMoveAdded = true;
			}
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
			if (Move.areEqualForBestKiller(move, state[ply].bestMove))
				return;
			if (KillerList.isMoveOnListAtPly(state[ply].killers, move))
				return;
			if (!pm.getTheBoard().isIllegalMove(move, state[ply].needToEscapeMate)) {
				state[ply].normal_search_moves[state[ply].normal_fill_index++] = move;
				state[ply].moveCount++;
				legalQuietMoveAdded = true;
			}
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Test APIs
	//
	public List<Integer> getList() {
		List<Integer> ml = new ArrayList<Integer>();
		MoveListIterator it = getNextMovesAtPly(0);
		if (it.hasNext()) {
			do {
				while (it.hasNext()) {
					int currMove = it.nextInt();
					ml.add(currMove);
				}
				it = getNextMovesAtPly(0);
			} while (it.hasNext());
		}
		return ml;
	}
	
	static private int getPosition(String notation) {
		if (notation.length() == 2) {
			GenericFile file;
			GenericRank rank;
			if (GenericFile.isValid(notation.charAt(0))) {
				file = GenericFile.valueOf(notation.charAt(0));
				if (GenericRank.isValid(notation.charAt(1))) {
				    rank = GenericRank.valueOf(notation.charAt(1));
				    return Position.valueOf(GenericPosition.valueOf(file, rank));
			    }
			}
		}
		return Position.NOPOSITION;
	}
	
	static private int getMoveDisambiguatedByFile(String notation, List<Integer> moveList, int originPiece) {
		if (GenericFile.isValid(notation.charAt(1))) {
			GenericFile file = GenericFile.valueOf(notation.charAt(0));
			int targetSquare = getPosition(notation.substring(1));
			for (int move : moveList) {
		    	if ((BitBoard.bitToPosition_Lut[Move.getTargetPosition(move)] == targetSquare) &&
		    		(Move.getOriginPiece(move) == originPiece) && 
		    		(BitBoard.getFile(Move.getOriginPosition(move)) == IntFile.valueOf(file)))
		    		return move;
		    }
		}
		return Move.NULL_MOVE;
	}
	
	static private int getMove(String notation, List<Integer> moveList, int originPiece) {
		int targetSquare = getPosition(notation);
	    for (int move : moveList) {
	    	if ((BitBoard.bitToPosition_Lut[Move.getTargetPosition(move)] == targetSquare) &&
	    		(Move.getOriginPiece(move) == originPiece))
	    		return move;
	    }
	    return Move.NULL_MOVE;
	}
	
	static private int getPromotionMove(List<Integer> moveList, String notation, int originPiece) {
		int targetSquare = getPosition(notation.substring(0,2));
		char promoPiece = notation.charAt(2);
		int promo = Piece.NONE;
		switch(promoPiece) {
		case 'Q':
			promo = Piece.QUEEN;
			break;
		case 'R':
			promo = Piece.ROOK;
			break;
		case 'B':
			promo = Piece.BISHOP;
			break;
		case 'N':
			promo = Piece.KNIGHT;
			break;
		default:
			break;
		}
		for (int move : moveList) {
	    	if ((BitBoard.bitToPosition_Lut[Move.getTargetPosition(move)] == targetSquare) &&
	    		(Move.getOriginPiece(move) == originPiece) && 
	    		(Move.getPromotion(move) == promo))
	    		return move;
	    }
		return Move.NULL_MOVE;
	}
	
	static private int getPieceMove(List<Integer> moveList, String notation, int originPiece) {
		if (notation.length() == 3) {
			return getMoveDisambiguatedByFile(notation, moveList, originPiece);
		} else {
			return getMove(notation, moveList, originPiece);
		}
	}
	
	static private int getPawnMove(List<Integer> moveList, String notation, int originPiece) {
		if (notation.length() == 2) {
			return getMove(notation, moveList, originPiece);
		} else if (notation.length() == 3) {
			int disambiguated_move = getMoveDisambiguatedByFile(notation, moveList, originPiece);
			if (disambiguated_move != Move.NULL_MOVE) {
				return disambiguated_move;
			} else {
				return getPromotionMove(moveList, notation, originPiece);
			}
		}
		return Move.NULL_MOVE;
	}
	
	static private String preprocessSAN(String notation) {
	    notation = notation.trim();
	    notation = notation.replaceAll(" ", "");
	    notation = notation.replaceAll("x", "");
	    notation = notation.replaceAll(":", "");
	    notation = notation.replaceAll("=", "");
	    notation = notation.replaceAll("\\+", "");
	    notation = notation.replaceAll("#", "");
	    notation = notation.replaceAll("-", "");
	    return notation;
	}
	
	public static int getNativeMove(boolean isWhite, List<Integer> moveList, String notation) {
		int move = Move.NULL_MOVE;		
		notation = preprocessSAN(notation);
		switch(notation.charAt(0)) {
		case 'K':
			move = getPieceMove(moveList, notation.replaceAll("K", ""), isWhite ? Piece.WHITE_KING: Piece.BLACK_KING);
			break;
		case 'Q':
			move = getPieceMove(moveList, notation.replaceAll("Q", ""), isWhite ? Piece.WHITE_QUEEN: Piece.BLACK_QUEEN);
			break;
		case 'R':
			move = getPieceMove(moveList, notation.replaceAll("R", ""), isWhite ? Piece.WHITE_ROOK: Piece.BLACK_ROOK);
			break;
		case 'B':
			move = getPieceMove(moveList, notation.replaceAll("B", ""), isWhite ? Piece.WHITE_BISHOP: Piece.BLACK_BISHOP);
			break;
		case 'N':
			move = getPieceMove(moveList, notation.replaceAll("N", ""), isWhite ? Piece.WHITE_KNIGHT: Piece.BLACK_KNIGHT);
			break;
			// Pawn moves
		case 'a': case 'b':	case 'c':case 'd': case 'e': case 'f': case 'g': case 'h':
			move = getPawnMove(moveList, notation, isWhite ? Piece.WHITE_PAWN: Piece.BLACK_PAWN);
			break;
		case 'O':
			// Castling
			if (notation.matches("OOO")) {
				move = isWhite ? CastlingManager.wqsc : CastlingManager.bqsc;
			} else {
				move = isWhite ? CastlingManager.wksc : CastlingManager.bksc;
			}
			if (moveList.indexOf(move) == -1) move = Move.NULL_MOVE;
			break;
		default:
			break;
		}
		return move;
	}
}
