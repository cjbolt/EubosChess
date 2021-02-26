package eubos.search.generators;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.KillerList;
import eubos.search.NoLegalMoveException;
import eubos.search.PlySearcher;
import eubos.search.PrincipalContinuation;
import eubos.search.Score;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.TranspositionTableAccessor;

public class MiniMaxMoveGenerator implements
		IMoveGenerator {

	private IChangePosition pm;
	public IPositionAccessors pos;
	public PrincipalContinuation pc;
	public SearchMetrics sm;

	private PlySearcher ps;
	private IEvaluate pe;
	private FixedSizeTranspositionTable tt;
	private TranspositionTableAccessor tta;
	private short score;
	
	private KillerList killers;
	private int alternativeMoveListOrderingScheme = 1;
	public SearchDebugAgent sda;
	
	public static final int EXTENDED_SEARCH_PLY_LIMIT = 32;

	// Used for unit tests
	MiniMaxMoveGenerator( FixedSizeTranspositionTable hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		commonInit(hashMap, pm, pos);
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.getOnMove() == Piece.Colour.white);
	}

	// Used with Arena, Lichess
	public MiniMaxMoveGenerator(FixedSizeTranspositionTable hashMap,
			String fen,
			DrawChecker dc,
			SearchMetricsReporter sr,
			ReferenceScore refScore) {
		PositionManager pm = new PositionManager(fen, dc, refScore);
		commonInit(hashMap, pm, pm);
		sr.register(sm);
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.getOnMove() == Piece.Colour.white);
	}

	private void commonInit(FixedSizeTranspositionTable hashMap, IChangePosition pm, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		
		pe = pos.getPositionEvaluator();
		sm = new SearchMetrics(pos);
		tt = hashMap;
		score = 0;
		killers = new KillerList(EubosEngineMain.SEARCH_DEPTH_IN_PLY);
	}
	
	public short getScore() { return score; }
	
	private void initialiseSearchDepthDependentObjects(int searchDepth, IChangePosition pm, SearchMetrics sm) {
		pc = new PrincipalContinuation(searchDepth+EXTENDED_SEARCH_PLY_LIMIT, sda);
		sm.setDepth(searchDepth);
		sm.setPrincipalVariation(pc.toPvList(0));
		tta = new TranspositionTableAccessor(tt, pos, sda);
	}
	
	@Override
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, null);
	}
	
	public SearchResult findMove(
			byte searchDepth, 
			List<Integer> lastPc) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, lastPc, new SearchMetricsReporter(null, tt, new ReferenceScore(tt)));
	}
	
	@Override
	public SearchResult findMove(
			byte searchDepth, 
			List<Integer> lastPc,
			SearchMetricsReporter sr) throws NoLegalMoveException, InvalidPieceException {
		boolean foundMate = false;
		boolean isExactScore = false;
		initialiseSearchDepthDependentObjects(searchDepth, pm, sm);
		ps = new PlySearcher(tta, pc, sm, sr, searchDepth, pm, pos, lastPc, pe, killers, sda);
		if (alternativeMoveListOrderingScheme > 0) {
			ps.alternativeMoveListOrdering(alternativeMoveListOrderingScheme);
		}
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		try {
			int eubos_score = ps.searchPly();
			score = Score.getScore(eubos_score);
			isExactScore = Score.isExact(eubos_score);
		} catch (AssertionError e) {
			e.printStackTrace();
			System.exit(0);
		}
		if (Score.isMate(score) && isExactScore) {
			foundMate = true;
		}
		// Select the best move
		GenericMove bestMove = Move.toGenericMove(pc.getBestMove((byte)0));
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return new SearchResult(bestMove,foundMate);
	}
	
	public synchronized void terminateFindMove() {
		if (ps != null)
			ps.terminateFindMove();
	}

	public void alternativeMoveListOrdering(int schemeToUse) {
		alternativeMoveListOrderingScheme = schemeToUse;		
	}
}
