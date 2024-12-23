package eubos.main;

import java.util.Arrays;
import java.util.logging.Level;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.search.DrawChecker;
import eubos.search.KillerList;
import eubos.search.PlySearcher;
import eubos.search.PrincipalContinuation;
import eubos.search.Score;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.SearchResult;
import eubos.search.transposition.Transposition;

public class Validate {
	
	PositionManager rootPosition;
	EubosEngineMain eubos;
	
	public Validate(EubosEngineMain eubos) {
		this.rootPosition = eubos.rootPosition;
		this.eubos = eubos;
	}
	
	public int validate(boolean trustedMoveWasFromTrans, long tableRootTrans, SearchResult result, int trusted_move) {
		boolean override_trusted_move = false;
		/* Do a short validation search, it has to be shallow because at longer time controls we can't hope to match
		   the main search depth without using a Transposition table, which we suspect may be corrupted.  */
		int trusted_depth = trustedMoveWasFromTrans ? Transposition.getDepthSearchedInPly(tableRootTrans): result.depth;
		int researchDepth = Math.min((trusted_depth*4)/5, 20);
		short trusted_score = (short)(trustedMoveWasFromTrans ? Transposition.getScore(tableRootTrans): result.score);
		
		String rootReport = result.report(rootPosition.getTheBoard());
		String rootFen = rootPosition.getFen();
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert eubos.lastFen.equals(rootFen) : String.format("Fen mismatch after search.\n%s\n%s", rootFen, eubos.lastFen);
		}
		
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.info(String.format("Started validation search trusted_score=%d", trusted_score));
		}
		
		// Operate on a copy of the rootPosition to prevent reentrant issues at tight time controls
		PositionManager pm = new PositionManager(rootFen, rootPosition.getHash(), new DrawChecker(), new PawnEvalHashTable());
		SearchDebugAgent sda = new SearchDebugAgent(rootPosition.getMoveNumber(), rootPosition.onMoveIsWhite());
		PrincipalContinuation pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
		SearchResult validation_result = doValidationSearch(pm, pc, sda, trusted_score, researchDepth);
		if (validation_result.foundMate) {
			return trusted_move;
		}
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.info(String.format("Completed validation search %s", validation_result.report(pm.getTheBoard())));
		}
		
		boolean valid = pm.performMove(trusted_move);
		assert valid;
		SearchResult opponent_result = verifyTrustedMoveScore(pm, pc, sda, trusted_score, researchDepth, trusted_move);

		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.info(String.format("Opponent result after trusted move %s", opponent_result.report(pm.getTheBoard())));
		}
		if (opponent_result.pv == null || opponent_result.pv[0] == Move.NULL_MOVE || opponent_result.foundMate) {
			return trusted_move;
		}
		
		int our_valid_move = validation_result.pv[0];
		int our_valid_score = validation_result.score;
		int delta = Math.abs(trusted_score-our_valid_score);
		
		// Note: these are for if we actually applied the trusted move
		int opponent_next_move = opponent_result.pv[0];
		//int opponent_score = opponent_result.score;
		
		// For now this is meant to catch crude piece blunders only... like not moving en-prise attacked piece
		// we can check this by checking opponents next move is not a capture, and there is not a high score delta
		if (!Move.areEqual(our_valid_move, trusted_move) &&
			Move.isCapture(opponent_next_move) &&
			(delta >= Piece.MATERIAL_VALUE_KNIGHT /*|| opponent_score > (trusted_score+150)*/)) {
			
			if (EubosEngineMain.error_logger.getLevel() != Level.SEVERE) {
				EubosEngineMain.createErrorLog();
			}
			StringBuilder string = new StringBuilder();
			string.append(String.format(
					"\nvalidation_score=%d trusted_score=%d (DELTA=%d) validation_move=%s trusted_move=%s",
					our_valid_score, trusted_score, delta, 
					Move.toString(our_valid_move), Move.toString(trusted_move)));		
			string.append(String.format(
					"\nThe best move was %s at root position %s\noriginal search result was %s",
					Move.toString(trusted_move),
					rootFen, rootReport));
			string.append(String.format("\nvalidation search to depth %d result is %s",
					researchDepth, validation_result.report(pm.getTheBoard())));
			string.append(String.format("\nOpponent's search result after the trusted move was applied is %s",
					opponent_result.report(pm.getTheBoard())));
			EubosEngineMain.error_logger.severe(string.toString());
		}
		
		return override_trusted_move ? our_valid_move : trusted_move;
	}
	
	public void checkForDraws(DrawChecker dc, String fen, int trustedMove) {
		String rootFen = rootPosition.getFen();
		// Operate on a copy of the rootPosition to prevent reentrant issues at tight time controls
		PositionManager pm = new PositionManager(rootFen, rootPosition.getHash(), dc, new PawnEvalHashTable());
		SearchDebugAgent sda = new SearchDebugAgent(rootPosition.getMoveNumber(), rootPosition.onMoveIsWhite());
		PrincipalContinuation pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
		byte search_depth = (byte)1;
		PlySearcher ps = new PlySearcher(
				eubos.hashMap,
				pc, 
				new SearchMetrics(pm), 
				null, 
				search_depth,
				pm,
				pm,
				pm.getPositionEvaluator(),
				new KillerList(),
				sda,
				new MoveList(pm, 0));
		int score = ps.searchRoot(1,Score.PROVISIONAL_ALPHA,Score.PROVISIONAL_BETA);
		SearchResult result =  new SearchResult(pc.toPvList(0), false, 0L, 1, true, score);
		pm.performMove(pc.toPvList(0)[0]);
		boolean confirmDrawn = pm.isThreefoldRepetitionPossible();
		if (confirmDrawn && score == 0) {
			eubos.sendInfoString(
				String.format("from %s draw could be claimed after %s at %s because 1ply search %s",
						fen, Move.toString(trustedMove), pm.getFen(), result.report(pm.getTheBoard())));
			eubos.sendInfoString(
					String.format("hash %d, %s",
							pm.getHash(), dc.report(pm.getPlyNumber())));
			System.err.println(
					String.format("from %s draw could be claimed after %s at %s because 1ply search %s",
					fen, Move.toString(trustedMove), pm.getFen(), result.report(pm.getTheBoard())));
			System.err.println(String.format("hash %d, %s",
					pm.getHash(), dc.report(pm.getPlyNumber())));
		}
		pm.unperformMove();
	}
	
	private SearchResult doValidationSearch(PositionManager pm, PrincipalContinuation pc, SearchDebugAgent sda, int trusted_score, int depth)
	{
		byte search_depth = (byte)depth;
		PlySearcher ps = new PlySearcher(
				eubos.hashMap, //new DummyTranspositionTable(),
				pc, 
				new SearchMetrics(pm), 
				null, 
				search_depth,
				pm,
				pm,
				pm.getPositionEvaluator(),
				new KillerList(),
				sda,
				new MoveList(pm, 0));
		
		int score = ps.searchRoot(depth,
				Math.max(Score.PROVISIONAL_ALPHA, trusted_score-2200),
				Math.min(Score.PROVISIONAL_BETA, trusted_score+2200));
		
		return new SearchResult(pc.toPvList(0), false, 0L, depth, true, score);
	}
	
	private SearchResult verifyTrustedMoveScore(PositionManager pm,  PrincipalContinuation pc, SearchDebugAgent sda, int trusted_score, int researchDepth, int trusted_move) {
		byte search_depth = (byte)(researchDepth-1);
		
		// Set up a best move for each ply of validation search
		PrincipalContinuation seeded_pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
		int [] list = pc.toPvList(0);
		if (list.length > 1) {
			int [] next_ply_list = Arrays.copyOfRange(list, 1, EubosEngineMain.SEARCH_DEPTH_IN_PLY);
			next_ply_list[0] = trusted_move;
			seeded_pc.setArray(next_ply_list);
		}
		
		PlySearcher ps = new PlySearcher(
				eubos.hashMap, //new DummyTranspositionTable(),
				seeded_pc, 
				new SearchMetrics(pm), 
				null, 
				search_depth,
				pm,
				pm,
				pm.getPositionEvaluator(),
				new KillerList(),
				sda,
				new MoveList(pm, 0));
		
		int score = -ps.searchRoot(
				search_depth,
				-Math.min(Score.PROVISIONAL_BETA, trusted_score+2200),
				-Math.max(Score.PROVISIONAL_ALPHA, trusted_score-2200));
		
		return new SearchResult(seeded_pc.toPvList(0), false, 0L, search_depth, true, score);
	}
	
	void validateEubosPv(PositionManager pm, String str, int[] pv) {
		try {
			if (pv != null) {
				int moves_applied = 0;
				for (int move : pv) {
					boolean valid = pm.performMove(move);
					assert valid;
					++moves_applied;
				}
				
				// Now, at this point, get a full evaluation from pe and report it
				int eval = pm.getPositionEvaluator().getFullEvaluation();
				EubosEngineMain.error_logger.severe(String.format("%s getFullEvaluation of PV is %d at %s", str, eval, pm.getFen()));
				
				for (int i=0; i<moves_applied; i++) {
					pm.unperformMove();
				}
			}
		} catch (AssertionError e) {
			EubosEngineMain.handleFatalError(e, "Error validating PV", pm);
			System.exit(0);
		}
	}
}
