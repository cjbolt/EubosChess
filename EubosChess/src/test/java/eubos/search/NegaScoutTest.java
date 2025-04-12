package eubos.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.neural_net.IEvaluate;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;
import eubos.position.PositionManager;
import eubos.search.PlySearcher.SearchState;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.ITranspositionAccessor;

public class NegaScoutTest {

	int currPly = 0;
	SearchState state[];
	private IChangePosition pm;
	private IPositionAccessors pos;
	private IEvaluate pe;
	private PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchDebugAgent sda;
	private ITranspositionAccessor tt;
	private KillerList killers;
	public long rootTransposition = 0L;
	private MoveList ml;
	PlySearcher ps;
	
	protected void setupPosition(String fen) {
		PositionManager posMgr = new PositionManager( fen );
		pm = posMgr;
		pos = posMgr;
		sm = new SearchMetrics(pos);
		killers = new KillerList();
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.onMoveIsWhite());
		pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
		ml = new MoveList((PositionManager)pm, 1);
		tt = new FixedSizeTranspositionTable();
		pe = posMgr.getPositionEvaluator();
		ps = new PlySearcher(tt, pc, sm, null, (byte) 6, pm, pos, pe, killers, sda, ml);
		currPly = 0;
		state = new SearchState[EubosEngineMain.SEARCH_DEPTH_IN_PLY+1];
		for (int i=0; i < state.length; i++) {
			state[i] = ps.new SearchState();
		}
	}
	
	private void debug(int score, int expectedMove, int bestMove) {
		System.out.println(String.format("score=%d expected=%s best=%s", score, Move.toString(expectedMove), Move.toString(bestMove)));
		
		int [] pv = pc.toPvList(0);
		for (int move: pv) {
			System.out.println(String.format("%s\n", Move.toString(move)));
		}
	}
	
	@Test
	public void test_mateInTwo() throws IllegalNotationException {
		setupPosition( "k1K5/b7/R7/1P6/1n6/8/8/8 w - - - 1");
		int score = negaScout(5, -Score.PROVISIONAL_BETA, -Score.PROVISIONAL_ALPHA);
		
		int expectedMove = Move.valueOfBit(BitBoard.b5, Piece.WHITE_PAWN, BitBoard.b6, Piece.NONE);
		int bestMove = pc.getBestMove((byte)0);
		debug(score, expectedMove, bestMove);
		
		assertTrue(Move.areEqual(expectedMove, bestMove));
		assertTrue(Score.isMate((short)score));
		assertEquals(Score.PROVISIONAL_BETA-3, score);
	}
	
	@Test
	public void test_mateInTwo_alt() throws IllegalNotationException {
		setupPosition( "rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - 0 1");
		int score = negaScout(5, -Score.PROVISIONAL_BETA, -Score.PROVISIONAL_ALPHA);
		
		int expectedMove = Move.valueOfBit(BitBoard.h7, Piece.WHITE_KNIGHT, BitBoard.f6, Piece.NONE);
		int bestMove = pc.getBestMove((byte)0);
		debug(score, expectedMove, bestMove);
		
		assertTrue(Move.areEqual(expectedMove, bestMove));
		assertTrue(Score.isMate((short)score));
		assertEquals(Score.PROVISIONAL_BETA-3, score);
	}
	
	@Test
	public void test_KQk_mate_in_3() throws IllegalNotationException {
		setupPosition( "2kr3r/ppp2ppp/8/8/1P5P/1K1b1P1N/P3P1P1/4qB1R b - - 3 24");
		int score = negaScout(7, -Score.PROVISIONAL_BETA, -Score.PROVISIONAL_ALPHA);
		
		int expectedMove = Move.valueOfBit(BitBoard.e1, Piece.BLACK_QUEEN, BitBoard.b1, Piece.NONE);
		int bestMove = pc.getBestMove((byte)0);
		debug(score, expectedMove, bestMove);
		
		assertTrue(Move.areEqual(expectedMove, bestMove));
		assertTrue(Score.isMate((short)score));
		assertEquals(Score.PROVISIONAL_BETA-5, score);
	}
	
	@Test
	public void test_findMove_WhitePawnCapture() throws IllegalNotationException {
		setupPosition("7k/8/3p2p1/2P5/8/8/8/7K w - - - 1");
		int score = negaScout(7, -Score.PROVISIONAL_BETA, -Score.PROVISIONAL_ALPHA);
		
		int expectedMove = Move.valueOfBit(BitBoard.c5, Piece.WHITE_PAWN, BitBoard.d6, Piece.BLACK_PAWN);
		int bestMove = pc.getBestMove((byte)0);
		debug(score, expectedMove, bestMove);
		assertTrue(Move.areEqual(expectedMove, bestMove));
	}
	
	
	private int negaScout(int depth, int alpha, int beta) {
		
		SearchState s = state[currPly];
		s.update();
		s.initialise(currPly, alpha, beta);
		
		// Check for absolute draws
		if (pos.isThreefoldRepetitionPossible() || pos.isInsufficientMaterial()) return 0;
		
		// Mate distance pruning
		int mating_value = Score.PROVISIONAL_BETA - currPly;
		if (mating_value < s.beta) {
			s.beta = mating_value;
		    if (s.alpha >= mating_value) return mating_value;
		}
		mating_value = Score.PROVISIONAL_ALPHA + currPly;
		if (mating_value > s.alpha) {
		    s.alpha = mating_value;
		    if (s.beta <= mating_value) return mating_value;
		}
		
		// Absolute depth limit
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			return pe.getFullEvaluation();
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		if (s.inCheck) {
			++depth;
		}
		
		if (depth <= 0) {
			//return ps.extendedSearch(s.alpha, s.beta, depth-1);
			return pe.getFullEvaluation();
		}
				
		// Main search loop for this ply
		int adaptiveBeta = s.beta;
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		boolean refuted = false;
		MoveListIterator move_iter = ml.initialiseAtPly(s.prevBestMove, killers.getMoves(currPly), s.inCheck, false, currPly);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !refuted) {
			
			if (!pm.performMove(currMove)) { continue; }
			
			s.moveNumber += 1;
			if (s.moveNumber == 1) {
				pc.initialise(currPly, currMove);
				bestMove = currMove;
			}
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(s.alpha, s.beta);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			currPly++;
			
			positionScore = -negaScout(depth-1, -adaptiveBeta, -s.alpha);
			
			pm.unperformMove();
			currPly--;
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			// Handle score backed up to this node
			if (positionScore > s.bestScore) {
				bestMove = currMove;
				if (adaptiveBeta == s.beta || depth < 2) {
					s.bestScore = positionScore;
				} else {
					currPly++;
					pm.performMove(currMove);
					s.bestScore = -negaScout(depth-1, -s.beta, -positionScore);
					pm.unperformMove();
					currPly--;
				}
				if (s.bestScore > s.alpha) {
					s.alpha = s.bestScore;
					pc.update(currPly, bestMove);
				}
				if (s.alpha >= s.beta) {
					killers.addMove(currPly, bestMove);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(s.bestScore);
					refuted = true;
					break;
				}
				adaptiveBeta = s.alpha + 1;
			}
		}		
		return s.moveNumber == 0 ? s.inCheck ? Score.getMateScore(currPly) : 0 : s.bestScore;
	}
}
