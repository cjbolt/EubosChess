package eubos.search.generators;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.ReferenceScore;
import eubos.score.ReferenceScore.Reference;
import eubos.search.KillerList;

import eubos.search.PlySearcher;
import eubos.search.PrincipalContinuation;
import eubos.search.Score;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.Transposition;

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
	MiniMaxMoveGenerator(ITranspositionAccessor hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		score = 0;
		this.ref = new ReferenceScore(null).getReference();
		commonInit(hashMap, pm, pos);
		ps = new PlySearcher(tta, pc, sm, null, (byte)0, pm, pos, pe, killers, sda, ml, (short)0);
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
		ps = new PlySearcher(tta, pc, sm, sr, (byte)0, pm, pos, pe, killers, sda, ml, (short)0);
	}

	private void commonInit(ITranspositionAccessor hashMap, IChangePosition pm, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		
		tta = hashMap;
		pe = pos.getPositionEvaluator();
		sm = new SearchMetrics(pos);
		killers = new KillerList();
		sda = new SearchDebugAgent(pos.getMoveNumber(), pos.getOnMove() == Piece.Colour.white);
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
		ps.reinitialise(searchDepth, sr, scoreToUse);
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
		return new SearchResult(pc.getBestMove((byte)0), foundMate, ps.rootTransposition);
	}
	
	public void terminateFindMove() {
		ps.terminateFindMove();
	}

	public void alternativeMoveListOrdering(int schemeToUse) {
		ml = new MoveList((PositionManager)pm, alternativeMoveListOrderingScheme);		
	}
	
	public void reportStatistics() {
		this.pe.reportLazyStatistics();
		this.pe.reportPawnStatistics();
	}

	public boolean lastAspirationFailed() {
		return ps.lastAspirationFailed();
	}
	
	public void preservePvInHashTable(long root_trans) {
		// Apply all the moves in the pv and check the resulting position is in the hash table
		byte i=0;
		
		if (root_trans == 0L) return;
		int move = pc.getBestMove(i);
		if (move == Move.NULL_MOVE)
			return;
		
		int searchDepth = Transposition.getDepthSearchedInPly(root_trans);
		short theScore = Transposition.getScore(root_trans);
		long trans = root_trans;
		pm.performMove(move);
		int movesApplied = 1;
		
		// new hash is following best move having been applied, now need to check we still have
		// transposition entries for the PV move positions. If they are not present create them.
		// If they are different, leave it be (the Transposition could be based on a deeper search)
		// and abort the checking.
		long new_hash = pos.getHash();
		trans = tta.getTransposition(new_hash);
				
		for (i=1; i < pc.length[0]; i++) {

			move = pc.getBestMove(i);
			if (move == Move.NULL_MOVE) break;
			if (!Move.areEqualForBestKiller(move, Transposition.getBestMove(trans))) break;
			
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert pos.getTheBoard().isPlayableMove(move, pos.isKingInCheck(), pos.getCastling()):
					String.format("%s not playable after %s fen=%s", Move.toString(move), pos.unwindMoveStack(), pos.getFen());
			}
			
			if (trans == 0L) {
				byte depth = (byte)(searchDepth-i);
				trans = tta.setTransposition(new_hash, trans, depth, theScore, Score.upperBound, move, pos.getMoveNumber());
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.info(
							String.format("At ply %d, hash table entry lost, regenerating with bestMove from pc=%s",
							i, Move.toString(move), Transposition.report(trans)));
				}
			}
			
			pm.performMove(move);
			movesApplied += 1;
			new_hash = pos.getHash();
			trans = tta.getTransposition(new_hash);
		}
		while (movesApplied > 0) {
			pm.unperformMove();
			movesApplied--;
		}
	}
}
