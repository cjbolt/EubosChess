package eubos.search.generators;

import eubos.evaluation.IEvaluate;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.search.KillerList;

import eubos.search.PlySearcher;
import eubos.search.PrincipalContinuation;
import eubos.search.ReferenceScore;
import eubos.search.Score;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.ReferenceScore.Reference;
import eubos.search.transposition.ITranspositionAccessor;

public class MiniMaxMoveGenerator implements
		IMoveGenerator {

	private IChangePosition pm;
	public IPositionAccessors pos;
	public PrincipalContinuation pc;
	public SearchMetrics sm;
	private MoveList ml;

	private PlySearcher ps;
	private IEvaluate pe;
	private ITranspositionAccessor tta;
	private short score;
	
	private KillerList killers;
	private int alternativeMoveListOrderingScheme = 1;
	public SearchDebugAgent sda;
	private Reference ref;

	// Used for unit tests
	public MiniMaxMoveGenerator(ITranspositionAccessor hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		score = 0;
		this.ref = new ReferenceScore(null).getReference();
		commonInit(hashMap, pm, pos);
		ps = new PlySearcher(tta, pc, sm, null, (byte)0, pm, pos, pe, killers, sda, ml);
	}

	// Used with Arena, Lichess
	public MiniMaxMoveGenerator(ITranspositionAccessor hashMap,
			PositionManager pm,
			SearchMetricsReporter sr,
			Reference ref) {
		commonInit(hashMap, pm, pm);
		this.ref = ref;
		score = ref.score;
		if (sr != null)
			sr.register(sm);
		ps = new PlySearcher(tta, pc, sm, sr, (byte)0, pm, pos, pe, killers, sda, ml);
	}

	private void commonInit(ITranspositionAccessor hashMap, IChangePosition pm, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		
		tta = hashMap;
		pe = pos.getPositionEvaluator();
		sm = new SearchMetrics(pos);
		killers = new KillerList();
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.onMoveIsWhite());
		pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
		ml = new MoveList((PositionManager)pm, alternativeMoveListOrderingScheme);
	}
	
	public short getScore() { return score; }
	
	@Override
	public SearchResult findMove(byte searchDepth)  {
		return this.findMove(searchDepth, new SearchMetricsReporter());
	}
	
	@Override
	public SearchResult findMove(
			byte searchDepth, 
			SearchMetricsReporter sr)  {
		boolean foundMate = false;
		short scoreToUse = (searchDepth > ref.depth) ? score : ref.score;
		sm.setDepth(searchDepth);
		ps.reinitialise(searchDepth, sr);
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		try {
			score = (short) ps.searchPly(scoreToUse);
		} catch (Exception e) {
			EubosEngineMain.handleFatalError(e, "PlySearcher threw an exception", pos);
			System.exit(0);
		} catch (AssertionError e) {
			EubosEngineMain.handleFatalError(e, "PlySearcher hit an assertion error", pos);
			System.exit(0);
		}
		if (Score.isMate(score)) {
			foundMate = true;
		}
		// Select the best move
		return new SearchResult(pc.toPvList(0), foundMate, ps.rootTransposition, searchDepth, ps.certain, score);
	}
	
	public void terminateFindMove() {
		ps.terminateFindMove();
	}

	public void alternativeMoveListOrdering(int schemeToUse) {
		ml = new MoveList((PositionManager)pm, alternativeMoveListOrderingScheme);		
	}

	public boolean lastAspirationFailed() {
		return ps.lastAspirationFailed();
	}
	
	public void setEngineCallback(EubosEngineMain eubos) {
		ps.setEubosEngine(eubos);
	}
}
