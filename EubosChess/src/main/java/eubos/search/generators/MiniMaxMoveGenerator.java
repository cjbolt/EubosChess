package eubos.search.generators;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.KillerList;

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
	private TranspositionTableAccessor tta;
	private short score;
	
	private KillerList killers;
	private int alternativeMoveListOrderingScheme = 1;
	public SearchDebugAgent sda;

	// Used for unit tests
	MiniMaxMoveGenerator( FixedSizeTranspositionTable hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		commonInit(hashMap, pm, pos);
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
	}

	private void commonInit(FixedSizeTranspositionTable hashMap, IChangePosition pm, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		
		pe = pos.getPositionEvaluator();
		sm = new SearchMetrics(pos);
		score = 0;
		killers = new KillerList();
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.getOnMove() == Piece.Colour.white);
		tta = new TranspositionTableAccessor(hashMap, pos, sda);
		pc = new PrincipalContinuation(Byte.MAX_VALUE, sda);
	}
	
	public short getScore() { return score; }
	
	@Override
	public SearchResult findMove(byte searchDepth)  {
		return this.findMove(searchDepth, new SearchMetricsReporter(null, null, new ReferenceScore(null)));
	}
	
	@Override
	public SearchResult findMove(
			byte searchDepth, 
			SearchMetricsReporter sr)  {
		boolean foundMate = false;
		sm.setDepth(searchDepth);
		sm.setPrincipalVariation(pc.toPvList(0));
		ps = new PlySearcher(tta, pc, sm, sr, searchDepth, pm, pos, pe, killers, sda);
		if (alternativeMoveListOrderingScheme > 0) {
			ps.alternativeMoveListOrdering(alternativeMoveListOrderingScheme);
		}
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		try {
			if (EubosEngineMain.ENABLE_ASPIRATION_WINDOWS) {
				score = (short) ps.searchPly(score);
			} else {
				score = (short) ps.searchPly();
			}
		} catch (AssertionError e) {
			EubosEngineMain.logger.severe(String.format("Assert fail: %s", e));
			System.exit(0);
		} catch (Exception e) {
			EubosEngineMain.logger.severe(String.format("PlySearcher threw an exception: %s", e));
			System.exit(0);
		}
		if (Score.isMate(score)) {
			foundMate = true;
		}
		// Select the best move
		return new SearchResult(pc.getBestMove((byte)0), foundMate);
	}
	
	public synchronized void terminateFindMove() {
		if (ps != null)
			ps.terminateFindMove();
	}

	public void alternativeMoveListOrdering(int schemeToUse) {
		alternativeMoveListOrderingScheme = schemeToUse;		
	}
}
