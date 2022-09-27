package eubos.search.generators;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.PawnEvalHashTable;
import eubos.score.ReferenceScore;
import eubos.score.ReferenceScore.Reference;
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
	private MoveList ml;

	private PlySearcher ps;
	private IEvaluate pe;
	private TranspositionTableAccessor tta;
	private short score;
	
	private KillerList killers;
	private int alternativeMoveListOrderingScheme = 1;
	public SearchDebugAgent sda;
	private Reference ref;

	// Used for unit tests
	MiniMaxMoveGenerator( FixedSizeTranspositionTable hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		score = 0;
		this.ref = new ReferenceScore(null).getReference();
		commonInit(hashMap, pm, pos);
	}

	// Used with Arena, Lichess
	public MiniMaxMoveGenerator(FixedSizeTranspositionTable hashMap,
			PawnEvalHashTable pawnHash,
			String fen,
			DrawChecker dc,
			SearchMetricsReporter sr,
			Reference ref) {
		PositionManager pm = new PositionManager(fen, dc, pawnHash);
		commonInit(hashMap, pm, pm);
		this.ref = ref;
		score = ref.score;
		if (sr != null)
			sr.register(sm);
	}

	private void commonInit(FixedSizeTranspositionTable hashMap, IChangePosition pm, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		
		pe = pos.getPositionEvaluator();
		sm = new SearchMetrics(pos);
		killers = new KillerList();
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.getOnMove() == Piece.Colour.white);
		tta = new TranspositionTableAccessor(hashMap, pos, sda);
		pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
		ml = new MoveList((PositionManager)pm, alternativeMoveListOrderingScheme);
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
		short scoreToUse = (searchDepth > ref.depth) ? score : ref.score;
		sm.setDepth(searchDepth);
		ps = new PlySearcher(tta, pc, sm, sr, searchDepth, pm, pos, pe, killers, sda, ml, scoreToUse);
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		try {
			score = (short) ps.searchPly(scoreToUse);
		} catch (Exception e) {
			Writer buffer = new StringWriter();
			PrintWriter pw = new PrintWriter(buffer);
			e.printStackTrace(pw);
			String error = String.format("PlySearcher threw an exception: %s\n%s\n%s",
					e.getMessage(), pos.unwindMoveStack(), buffer.toString());
			System.err.println(error);
			EubosEngineMain.logger.severe(error);
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
		ml = new MoveList((PositionManager)pm, alternativeMoveListOrderingScheme);		
	}
	
	public void reportLazyStatistics() {
		this.pe.reportLazyStatistics();
	}

	public boolean lastAspirationFailed() {
		return this.ps.lastAspirationFailed();
	}
}
