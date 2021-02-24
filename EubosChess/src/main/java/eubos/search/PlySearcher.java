package eubos.search;

import java.util.Iterator;
import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.IScoreMate;
import eubos.score.MateScoreGenerator;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.ITransposition;
import eubos.search.transposition.TranspositionEvaluation;

public class PlySearcher {
	
	private IChangePosition pm;
	IPositionAccessors pos;
	
	ScoreTracker st;
	private IEvaluate pe;
	private IScoreMate sg;
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchDebugAgent sda;
	
	private boolean terminate = false;
	
	private List<Integer> lastPc;
	private ITranspositionAccessor tt;
	private SearchMetricsReporter sr;
	private KillerList killers;
	
	byte currPly = 0;
	byte currDepthSearchedInPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	private byte extendedSearchLimitInPly = 0;
	private int moveListOrdering = 1;
	
	public PlySearcher(
			ITranspositionAccessor hashMap,
			ScoreTracker st,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			byte searchDepthPly,
			IChangePosition pm,
			IPositionAccessors pos,
			List<Integer> lastPc,
			IEvaluate pe,
			KillerList killers,
			SearchDebugAgent sda) {
		currPly = 0;
		currDepthSearchedInPly = 0;
		
		this.pc = pc;
		this.sm = sm;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.sr = sr;
		this.lastPc = lastPc;
		this.sda = sda;
		originalSearchDepthRequiredInPly = searchDepthPly;
		extendedSearchLimitInPly = setExtSearchDepth();
		
		tt = hashMap;
		sg = new MateScoreGenerator(pos, pe);
		this.killers = killers;
	}
	
	private byte setExtSearchDepth() {
		int variableDepthPly = originalSearchDepthRequiredInPly * 4;
		byte extDepthLimitPly = (byte)Math.min(MiniMaxMoveGenerator.EXTENDED_SEARCH_PLY_LIMIT, variableDepthPly);
		extDepthLimitPly += originalSearchDepthRequiredInPly;
		return extDepthLimitPly;
	}
	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { 
		EubosEngineMain.logger.info("Terminating PlySearcher");
		terminate = true;
	}
	private synchronized boolean isTerminated() { return terminate; }	

	public void alternativeMoveListOrdering(int orderingScheme) {
		moveListOrdering  = orderingScheme;		
	}
	
	public int searchPly() throws InvalidPieceException {
		short score = 0;
		currPly = 0;
		extendedSearchDeepestPly = 0;		
		score = (short) search(Short.MIN_VALUE+1, Short.MAX_VALUE, originalSearchDepthRequiredInPly);
		return Score.valueOf(score, Score.exact);
	}
	
	int search(int alpha, int beta, int depth) throws InvalidPieceException {
		int plyScore = Short.MIN_VALUE+1;
		byte plyBound = pos.onMoveIsWhite() ? Score.lowerBound : Score.upperBound;
		int prevBestMove = ((lastPc != null) && (lastPc.size() > currPly)) ? lastPc.get(currPly) : Move.NULL_MOVE;
		
		sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
		sda.printNormalSearch(alpha, beta);
		
		TranspositionEvaluation eval = tt.getTransposition(depth, beta);
		switch (eval.status) {
		case sufficientRefutation:
			// Add refuting move to killer list
			killers.addMove(currPly, eval.trans.getBestMove());
			sda.printHashIsRefutation(pos.getHash(), eval.trans);
			plyScore = handleRefutationOrTerminalNodeFromHash(plyScore, eval.trans);
			return Score.getScore(plyScore);
		case sufficientTerminalNode:
			sda.printHashIsTerminalNode(eval.trans, pos.getHash());
			plyScore = handleRefutationOrTerminalNodeFromHash(plyScore, eval.trans);
			updatePrincipalContinuation(eval.trans.getBestMove(), Score.getScore(plyScore));
			return Score.getScore(plyScore);
		case sufficientSeedMoveList:
			sda.printHashIsSeedMoveList(pos.getHash(), eval.trans);
			prevBestMove = eval.trans.getBestMove();
			// intentional drop through
		case insufficientNoData:
		default:
			break;
		}
		
		if (depth == 0) {
			return extendedSearch(alpha,beta);
		}

		// Handle draws by three-fold repetition
		if (!atRootNode() && pos.isThreefoldRepetitionPossible()) {
			return 0;
		}

		// Absolute depth limit
		if (currPly >= extendedSearchLimitInPly - 1) {
			return Score.getScore(pe.evaluatePosition());
		}

		// Extend search for in-check scenarios, treated outside of quiescence search 
		if (pos.isKingInCheck()) {
			++depth;
		}
		
		MoveList ml = new MoveList((PositionManager) pm, prevBestMove, killers.getMoves(currPly), moveListOrdering, false, Position.NOPOSITION);
		Iterator<Integer> move_iter = ml.getStandardIterator(false, Position.NOPOSITION);
		if (!move_iter.hasNext()) {
			return sg.scoreMate(currPly);
		}
		
		int currMove = move_iter.next();
		pc.initialise(currPly, currMove);
		
		int moveNumber = 1;
		while (!isTerminated()) {
			if (atRootNode()) {
				sm.setCurrentMove(Move.toGenericMove(currMove), moveNumber);
			}
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING)
				pc.clearContinuationBeyondPly(currPly);
			
			// Apply move and score
			sda.printPerformMove(currMove);
			sda.nextPly();
			currPly++;
			pm.performMove(currMove);
			int positionScore = -search(-beta, -alpha, depth-1);
			pm.unperformMove();
			currPly--;
			sda.prevPly();
			sda.printUndoMove(currMove, plyScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING)
				sm.incrementNodesSearched();
			
			if (isTerminated()) {
				// don't update PV if out of time for search, instead return last fully searched PV.
				return 0;
			}
			
			// Handle score backed up to this node
			if (positionScore > alpha) {	
				killers.addMove(currPly, currMove);
				sda.printRefutationFound(plyScore);
				
				if (positionScore >= beta) {
					eval.trans = updateTranspositionTable(eval.trans, (byte) depth, currMove, (short) beta, plyBound);
					return beta;
				}
				
				alpha = positionScore;
				eval.trans = updateTranspositionTable(eval.trans, (byte) depth, currMove, (short) alpha, plyBound);
				updatePrincipalContinuation(currMove,(short) alpha);
				
			} else if (positionScore > plyScore) {
				plyScore = positionScore;
				eval.trans = updateTranspositionTable(eval.trans, (byte) depth, currMove, (short) plyScore, plyBound);
			}
			
			// Break-out when out of moves
			if (move_iter.hasNext()) {
				sda.printNormalSearch(alpha, beta);
				currMove = move_iter.next();
				moveNumber += 1;
			} else {
				break;
			}
		}

		if (!isTerminated() && eval.trans != null) {
			checkToPromoteHashTableToExact(eval.trans, depth, (short) alpha);
		}
		return alpha;
	}
	
	public int extendedSearch(int alpha, int beta) throws InvalidPieceException {
		sda.printExtSearch(alpha, beta);
		MoveList ml = new MoveList((PositionManager) pm, Move.NULL_MOVE, killers.getMoves(currPly), moveListOrdering, true, Position.NOPOSITION);
		Iterator<Integer> move_iter = ml.getStandardIterator(true, Position.NOPOSITION);
		
        if (ml.isMateOccurred()) {
        	MoveList new_ml = new MoveList((PositionManager) pm, 0); // don't bother to sort the list
    		if (new_ml.isMateOccurred()) {
    			short mateScore = sg.scoreMate(currPly);
    			sda.printMateFound(mateScore);
        		return mateScore;
    		}
        }
        
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		
		short plyScore = Score.getScore(pe.evaluatePosition());	
		if (currPly >= extendedSearchLimitInPly - 1)
			return plyScore;
		
		if (!move_iter.hasNext()) {
			sda.printExtSearchNoMoves(plyScore);
			return plyScore;
		}
		
		// Stand Pat in extended search
		if (plyScore >= beta) {
			sda.printRefutationFound(plyScore);
			return beta;
		}
		if (plyScore > alpha)
			alpha = plyScore;
		
		int currMove = move_iter.next();
		pc.initialise(currPly, currMove);

		while(true) {
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING)
				pc.clearContinuationBeyondPly(currPly);
			
			// Apply capture and score
			sda.printPerformMove(currMove);			
			sda.nextPly();
			currPly++;
			pm.performMove(currMove, false);
			short lastScore = (short) -extendedSearch(-beta, -alpha);
			pm.unperformMove(false);
			currPly--;
			sda.prevPly();
			sda.printUndoMove(currMove, plyScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING)
				sm.incrementNodesSearched();
			
			if (lastScore > alpha) {
				if (lastScore >= beta) {
					killers.addMove(currPly, currMove);
					sda.printRefutationFound(lastScore);
					return beta;
				}
				alpha = plyScore;
				updatePrincipalContinuation(currMove, plyScore);
			} else if (lastScore > plyScore) {
				plyScore = lastScore;
			}
			
			if (move_iter.hasNext()) {
				currMove = move_iter.next();
			} else {
				break;
			}
		}
		return alpha;
	}
	
	ITransposition updateTranspositionTable(ITransposition trans, byte depth, int currMove, short plyScore, byte plyBound) {
		short scoreFromDownTree = updateMateScoresForEncodingMateDistanceInHashTable(plyScore);
		if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
			trans = tt.setTransposition(trans, depth, scoreFromDownTree, plyBound, currMove, pc.toPvList(currPly));
		} else {
			trans = tt.setTransposition(trans, depth, scoreFromDownTree, plyBound, currMove);
		}
		return trans;
	}
	
	private short updateMateScoresForEncodingMateDistanceInHashTable(short plyScore) {
		//
		// Modify mate score (which is expressed in distance from root node in ply) to
		// distance from leaf node (which is what needs to be stored in the hash table)
		//
		if (Score.isMate(plyScore)) {
			if (plyScore < 0) {
				plyScore = (short)(plyScore - currPly);
			} else {
				plyScore = (short)(plyScore + currPly);
			}
		}
		return plyScore;
	}
	
	private int handleRefutationOrTerminalNodeFromHash(int theScore, ITransposition trans) throws InvalidPieceException {
		int trans_move;
		short trans_score;
		synchronized (trans) {
			trans_move = trans.getBestMove();
			trans_score = trans.getScore();
		}
		// Check score for hashed position causing a search cut-off is still valid (i.e. best move doesn't lead to a draw)
		// If hashed score is a draw score, check it is still a draw, if not, search position
		boolean isThreefold = checkForRepetitionDueToPositionInSearchTree(trans_move);
		if (isThreefold || (!isThreefold && (trans_score == 0))) {
			// Assume it is now a draw, so re-search
			sda.printHashIsSeedMoveList(pos.getHash(), trans);
			theScore = 0;
		} else {
			short adjustedScoreForThisPositionInTree = trans_score;
			if (Score.isMate(trans_score)) {
				// The score stored in the hash table encodes the distance to the mate from the hashed position,
				// not the root node, so adjust for the position in search tree.
				adjustedScoreForThisPositionInTree = (short) ((trans_score < 0 ) ? trans_score+currPly : trans_score-currPly);
			}
			if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
				pc.update(currPly, trans.getPv());
			} else {
				pc.set(currPly, trans_move);
			}
			theScore = adjustedScoreForThisPositionInTree; //Score.valueOf(adjustedScoreForThisPositionInTree, trans_bound);
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING)
			sm.incrementNodesSearched();
		return theScore;
	}

	@SuppressWarnings("unused")
	private boolean checkForRepetitionDueToPositionInSearchTree(int move) throws InvalidPieceException {
		boolean retVal = false;
		if (move != Move.NULL_MOVE) {
			pm.performMove(move);
			sda.nextPly();
			if (pos.isThreefoldRepetitionPossible()) {
				sda.printRepeatedPositionHash(pos.getHash(), pos.getFen());
				retVal = true;
			}
			pm.unperformMove();
			sda.prevPly();
		}
		return retVal;
	}

	void checkToPromoteHashTableToExact(ITransposition trans, int depth, short plyScore) {

		short scoreFromDownTree = updateMateScoresForEncodingMateDistanceInHashTable(plyScore);

		// This is the only way a hash and score can be exact.
		// found to be needed due to score discrepancies caused by refutations coming out of extended search...
		// Still needed 22nd October 2020
		if (trans.checkUpdateToExact((byte) depth, scoreFromDownTree, pc.getBestMove(currPly)))
		{
			sda.printExactTrans(pos.getHash(), trans);			
		}
	}


	private void updatePrincipalContinuation(int currMove, short positionScore)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && atRootNode() && sr != null) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), positionScore);
			sr.reportPrincipalVariation(sm);
			extendedSearchDeepestPly = 0;
		}
	}

	boolean shouldUpdatePositionBoundScoreAndBestMove(short plyScore, byte plyBound, short positionScore) {
		boolean doUpdate = false;
		if (plyBound == Score.lowerBound) {
			if (positionScore > plyScore && positionScore != Short.MAX_VALUE)
				doUpdate = true;
		} else if (plyBound == Score.upperBound) {
			if (positionScore < plyScore && positionScore != Short.MIN_VALUE)
				doUpdate = true;
		} else {
			if (EubosEngineMain.ENABLE_ASSERTS)
				assert false;
		}
		return doUpdate;
	}
}
