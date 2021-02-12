package eubos.search;

import java.util.Iterator;
import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
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
	private byte dynamicSearchLevelInPly;
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
		dynamicSearchLevelInPly = searchDepthPly;
		originalSearchDepthRequiredInPly = searchDepthPly;
		extendedSearchLimitInPly = setExtSearchDepth();
		
		this.st = st;
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
	
	public int searchPly() throws InvalidPieceException {
		int theScore = 0;
		int prevBestMove = ((lastPc != null) && (lastPc.size() > currPly)) ? lastPc.get(currPly) : Move.NULL_MOVE;
		
		st.setProvisionalScoreAtPly(currPly);
		sda.printStartPlyInfo(st, pos, originalSearchDepthRequiredInPly);
		
		byte depthRequiredForTerminalNode = initialiseSearchAtPly();
		TranspositionEvaluation eval = tt.getTransposition(currPly, depthRequiredForTerminalNode);
		switch (eval.status) {
		case sufficientRefutation:
			// Add refuting move to killer list
			killers.addMove(currPly, eval.trans.getBestMove());
			sda.printHashIsRefutation(pos.getHash(), eval.trans);
			theScore = handleRefutationOrTerminalNodeFromHash(theScore, eval.trans);
			break;
		case sufficientTerminalNode:
			sda.printHashIsTerminalNode(eval.trans, pos.getHash());
			theScore = handleRefutationOrTerminalNodeFromHash(theScore, eval.trans);
			updatePrincipalContinuation(eval.trans.getBestMove(), Score.getScore(theScore));
			break;
		case sufficientSeedMoveList:
			sda.printHashIsSeedMoveList(pos.getHash(), eval.trans);
			prevBestMove = eval.trans.getBestMove();
			// intentional drop through
		case insufficientNoData:
			theScore = searchMoves( prevBestMove, eval.trans);
			break;	
		default:
			break;
		}
		clearUpSearchAtPly();
		
		return theScore;
	}
	
	private int handleRefutationOrTerminalNodeFromHash(int theScore, ITransposition trans) 
					throws InvalidPieceException {
		int trans_move;
		byte trans_bound;
		short trans_score;
		synchronized (trans) {
			trans_move = trans.getBestMove();
			trans_bound = trans.getType();
			trans_score = trans.getScore();
		}
		// Check score for hashed position causing a search cut-off is still valid (i.e. best move doesn't lead to a draw)
		// If hashed score is a draw score, check it is still a draw, if not, search position
		boolean isThreefold = checkForRepetitionDueToPositionInSearchTree(trans_move);
		if (isThreefold || (!isThreefold && (trans_score == 0))) {
			// Assume it is now a draw, so re-search
			sda.printHashIsSeedMoveList(pos.getHash(), trans);
			theScore = searchMoves( trans_move, trans);
		} else {
			short adjustedScoreForThisPositionInTree = st.adjustHashTableMateInXScore(currPly, trans_score);
			if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
				pc.update(currPly, trans.getPv());
			} else {
				pc.set(currPly, trans_move);
			}
			theScore = Score.valueOf(adjustedScoreForThisPositionInTree, trans_bound);
		}
		if (EubosEngineMain.UCI_INFO_ENABLED)
			sm.incrementNodesSearched();
		return theScore;
	}
	
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
	
	private byte initialiseSearchAtPly() {
		byte transDepthRequiredForTerminalNode = 0;
		if (currPly >= originalSearchDepthRequiredInPly) {
			dynamicSearchLevelInPly++;
		}
		if (this.isInExtendedSearch()) {
			transDepthRequiredForTerminalNode = extendedSearchLimitInPly;
		} else {
			transDepthRequiredForTerminalNode = (byte)(originalSearchDepthRequiredInPly - currPly);
		}
		return transDepthRequiredForTerminalNode;
	}
	
	private void clearUpSearchAtPly() {
		if (dynamicSearchLevelInPly > originalSearchDepthRequiredInPly) {
			dynamicSearchLevelInPly--;
		}
	}
	
	private int searchMoves(int prevBestMove, ITransposition trans) throws InvalidPieceException {
		int theScore;	
		MoveList ml = getMoveList(prevBestMove);
        if (ml.isMateOccurred()) {
        	if (isInExtendedSearch()) {
        		ml = new MoveList((PositionManager) pm, 0); // don't bother to sort
        		if (!ml.isMateOccurred()) {
	        		// It isn't actually a mate, stand PAT
	        		Byte plyBound = pos.onMoveIsWhite() ? Score.lowerBound : Score.upperBound;
	    			theScore = Score.setType(pe.evaluatePosition(), plyBound);
	    			sda.printExtSearchNoMoves(theScore);
        		} else {
        			// This was an extended search that was a mate position
            		theScore = Score.valueOf(sg.scoreMate(currPly), Score.exact);
        		}
        	} else {
        		// In normal search it is guaranteed to be a mate, so score accordingly
        		theScore = Score.valueOf(sg.scoreMate(currPly), Score.exact);
                // We will now de-recurse, so should make sure the depth searched is correct
                setDepthSearchedInPly();
                // Only update the transposition table in normal search, it isn't useful for extended search
                short mateScoreForTable = (short)((Score.getScore(theScore) < 0) ? Short.MIN_VALUE + 1 : Short.MAX_VALUE - 1);
    			trans = tt.setTransposition(trans, currDepthSearchedInPly, mateScoreForTable, Score.exact, Move.NULL_MOVE);
        	}
        	
            st.setBackedUpScoreAtPly(currPly, theScore);

        } else {
    		Iterator<Integer> move_iter = ml.getStandardIterator(isInExtendedSearch(), pos.lastMoveTargetSquare());
   			theScore = actuallySearchMoves(ml, move_iter, trans);
        }
        return theScore;
    }

	private int actuallySearchMoves(MoveList ml, Iterator<Integer> move_iter, ITransposition trans) throws InvalidPieceException {
		boolean backedUpScoreWasExact = false;
		boolean refutationFound = false;

		byte plyBound = pos.onMoveIsWhite() ? Score.lowerBound : Score.upperBound;
		short plyScore = (plyBound == Score.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;
		plyScore = establishStandPatInExtendedSearch(ml, plyBound, plyScore);
		
		if (!move_iter.hasNext()) {
			sda.printExtSearchNoMoves(plyScore);
			return plyScore;
		}
		
		int currMove = move_iter.next();
		pc.initialise(currPly, currMove);

		while(!isTerminated()) {
			if (EubosEngineMain.UCI_INFO_ENABLED)
				pc.clearContinuationBeyondPly(currPly);
			
			int positionScore = applyMoveAndScore(currMove);
			short justPositionScore = Score.getScore(positionScore);
			
			if (!isTerminated()) {
				// Rationale: this is when a score was received from lower down the tree - at this instant update the depth searched
				setDepthSearchedInPly();
				
				if (st.isAlphaBetaCutOff(currPly, justPositionScore)) {
					plyScore = justPositionScore;
					trans = updateTranspositionTable(trans, currMove, plyScore, plyBound);
					refutationFound = true;
					killers.addMove(currPly, currMove);
					sda.printRefutationFound();
					break;    
				}
				
				if (st.isBackUpRequired(currPly, justPositionScore, plyBound)) {
					// If backed up, update state and set flag if backup was exact
					if (Score.getType(positionScore) == Score.exact) {
						backedUpScoreWasExact = true;
					}
					plyScore = justPositionScore;
					st.setBackedUpScoreAtPly(currPly, justPositionScore);
					
					updatePrincipalContinuation(currMove, justPositionScore);
					trans = updateTranspositionTable(trans, currMove, plyScore, plyBound);

				} else if (shouldUpdatePositionBoundScoreAndBestMove(plyScore, plyBound, justPositionScore)) {
					// Update the hash entry if the move is better than that previously stored at this position
					plyScore = justPositionScore;
					trans = updateTranspositionTable(trans, currMove, plyScore, plyBound);
				} else {
					// skip any worse score that isn't a refutation, a back-up or an improvement of plyScore
				}
			}
			
			if (move_iter.hasNext()) {
				currMove = move_iter.next();
			} else {
				break;
			}
		}
		
		if (EubosEngineMain.ENABLE_YIELD_IN_WORKER_THREADS) {
			if ((currPly % 2) == 0) {
				Thread.yield();
			}
		}
		
		if (!isTerminated() && isInNormalSearch() && backedUpScoreWasExact && !refutationFound && trans != null) {
			checkToPromoteHashTableToExact(trans, plyScore);
			plyBound = Score.exact;
		}
		return Score.valueOf(plyScore, plyBound);
	}

	private void checkToPromoteHashTableToExact(ITransposition trans, short plyScore) {
		if (EubosEngineMain.ASSERTS_ENABLED)
			assert isInNormalSearch();
		
		short scoreFromDownTree = updateMateScoresForEncodingMateDistanceInHashTable(plyScore);
		
		// This is the only way a hash and score can be exact.
		// found to be needed due to score discrepancies caused by refutations coming out of extended search...
		// Still needed 22nd October 2020
		if (trans.checkUpdateToExact(currDepthSearchedInPly, scoreFromDownTree, pc.getBestMove(currPly)))
		{
			sda.printExactTrans(pos.getHash(), trans);			
		}
	}

	private ITransposition updateTranspositionTable(ITransposition trans, int currMove, short plyScore, byte plyBound) {
		if (isInNormalSearch()) {
			short scoreFromDownTree = updateMateScoresForEncodingMateDistanceInHashTable(plyScore);
			if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
				trans = tt.setTransposition(trans, currDepthSearchedInPly, scoreFromDownTree, plyBound, currMove, pc.toPvList(currPly));
			} else {
				trans = tt.setTransposition(trans, currDepthSearchedInPly, scoreFromDownTree, plyBound, currMove);
			}
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

	private short establishStandPatInExtendedSearch(MoveList ml, byte plyBound, short plyScore) throws InvalidPieceException {
		short theScore = plyScore;
		if (isInExtendedSearch()) {
			/*
			 * The idea is that if we are in an extended search, if there are normal moves available 
			 * and only a single "forced" capture, we shouldn't necessarily be forced into making that capture.
			 * The capture needs to improve the position score to get searched, otherwise this position can be treated as terminal.
			 * Note: This is only a problem for the PV search, all others will bring down alpha/beta score and won't 
			 * back up if the score for the capture is worse than that.
			 */
			theScore = Score.getScore(pe.evaluatePosition());
			if (st.isBackUpRequired(currPly, theScore, plyBound)) {
				st.setBackedUpScoreAtPly(currPly, theScore);
			}
		}
		return theScore;
	}

	private void setDepthSearchedInPly() {
		currDepthSearchedInPly = isInNormalSearch() ? (byte)(originalSearchDepthRequiredInPly - currPly) : 1;
	}
	
	private void updatePrincipalContinuation(int currMove, short positionScore)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (EubosEngineMain.UCI_INFO_ENABLED && atRootNode() && sr != null) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), positionScore);
			sr.reportPrincipalVariation(sm);
		}
	}
	
	private boolean isInExtendedSearch() {
		return dynamicSearchLevelInPly > originalSearchDepthRequiredInPly;
	}
	
	private boolean isInNormalSearch() {
		if (EubosEngineMain.ASSERTS_ENABLED)
			assert dynamicSearchLevelInPly >= originalSearchDepthRequiredInPly;
		return dynamicSearchLevelInPly == originalSearchDepthRequiredInPly;
	}

	private boolean shouldUpdatePositionBoundScoreAndBestMove(short plyScore, byte plyBound, short positionScore) {
		boolean doUpdate = false;
		if (plyBound == Score.lowerBound) {
			if (positionScore > plyScore && positionScore != Short.MAX_VALUE)
				doUpdate = true;
		} else if (plyBound == Score.upperBound) {
			if (positionScore < plyScore && positionScore != Short.MIN_VALUE)
				doUpdate = true;
		} else {
			if (EubosEngineMain.ASSERTS_ENABLED)
				assert false;
		}
		return doUpdate;
	}

	private MoveList getMoveList(int transBestMove) throws InvalidPieceException {
		MoveList ml = null;
		int[] killer_moves = killers.getMoves(currPly);
		if (isInExtendedSearch()) {
			int targetSq = pos.lastMoveTargetSquare();
			ml = new MoveList((PositionManager) pm, transBestMove, killer_moves[0], killer_moves[1], moveListOrdering, targetSq);
		} else {
			ml = new MoveList((PositionManager) pm, transBestMove, killer_moves[0], killer_moves[1], moveListOrdering);
		}
		return ml;
	}
	
	private int applyMoveAndScore(int currMove) throws InvalidPieceException {
		sda.printPerformMove(currMove);
		pm.performMove(currMove);
		currPly++;
		sda.nextPly();
		int positionScore = assessNewPosition(currMove);
		pm.unperformMove();
		currPly--;
		sda.prevPly();
		sda.printUndoMove(currMove);
		
		if (EubosEngineMain.UCI_INFO_ENABLED)
			sm.incrementNodesSearched();
		return positionScore;
	}
	
	private int assessNewPosition(int lastMove) throws InvalidPieceException {
		int positionScore = 0;
		if (isTerminalNode(lastMove)) {
			positionScore = pe.evaluatePosition();
			currDepthSearchedInPly = 1; // We applied a move in order to generate this score
		} else {
			positionScore = searchPly();
		}
		return positionScore;
	}
	
	private boolean isTerminalNode(int lastMove) {
		boolean terminalNode = false;
		if (pos.isThreefoldRepetitionPossible()) {
			sda.printRepeatedPositionSearch(pos.getHash(), pos.getFen());
			terminalNode = true;
		} else if (pos.getTheBoard().isInsufficientMaterial()) {
			terminalNode = true;
		} else if (currPly == originalSearchDepthRequiredInPly) {
			if (pe.isQuiescent(lastMove) || MiniMaxMoveGenerator.EXTENDED_SEARCH_PLY_LIMIT == 0) {
				terminalNode = true;
			}
		} else if (currPly > originalSearchDepthRequiredInPly) {
			if (pe.isQuiescent(lastMove) || isExtendedSearchLimitReached()) {
				if (currPly > extendedSearchDeepestPly) {
					extendedSearchDeepestPly = currPly;
				}
				terminalNode = true;
			}
		} else {
			// is not a terminal node
		}
		return terminalNode;
	}

	private boolean isExtendedSearchLimitReached() {
		boolean limitReached = false;
		if (currPly%2 == 0) {
			// means that initial onMove side is back on move
			if (currPly > extendedSearchLimitInPly-2) {
				// -2 always leaves room for one more move for each side without overflowing array...
				limitReached = true;
			}
		}
		return limitReached;
	}

	public void alternativeMoveListOrdering(int orderingScheme) {
		moveListOrdering  = orderingScheme;		
	}
}
