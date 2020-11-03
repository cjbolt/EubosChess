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
	
	private boolean terminate = false;
	
	private List<Integer> lastPc;
	private byte dynamicSearchLevelInPly;
	private ITranspositionAccessor tt;
	private SearchMetricsReporter sr;
	
	byte currPly = 0;
	byte currDepthSearchedInPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	private byte extendedSearchLimitInPly = 0;
	
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
			IEvaluate pe) {
		currPly = 0;
		currDepthSearchedInPly = 0;
		
		this.pc = pc;
		this.sm = sm;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.sr = sr;
		this.lastPc = lastPc;
		dynamicSearchLevelInPly = searchDepthPly;
		originalSearchDepthRequiredInPly = searchDepthPly;
		extendedSearchLimitInPly = setExtSearchDepth();
		pos.registerPositionEvaluator(pe);
		
		this.st = st;
		tt = hashMap;
		sg = new MateScoreGenerator(pos, pe);
	}
	
	private byte setExtSearchDepth() {
		int variableDepthPly = originalSearchDepthRequiredInPly * 4;
		byte extDepthLimitPly = (byte)Math.min(MiniMaxMoveGenerator.EXTENDED_SEARCH_PLY_LIMIT, variableDepthPly);
		extDepthLimitPly += originalSearchDepthRequiredInPly;
		return extDepthLimitPly;
	}
	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	public Score searchPly() throws InvalidPieceException {
		Score theScore = null;
		int prevBestMove = ((lastPc != null) && (lastPc.size() > currPly)) ? lastPc.get(currPly) : Move.NULL_MOVE;
		
		st.setProvisionalScoreAtPly(currPly);
		SearchDebugAgent.printStartPlyInfo(st, pos, originalSearchDepthRequiredInPly);
		
		byte depthRequiredForTerminalNode = initialiseSearchAtPly();
		TranspositionEvaluation eval = tt.getTransposition(currPly, depthRequiredForTerminalNode);		
		switch (eval.status) {
		case sufficientTerminalNode:
		case sufficientRefutation:
			// Check score for hashed position causing a search cut-off is still valid (i.e. best move doesn't lead to a draw)
			// If hashed score is a draw score, check it is still a draw, if not, search position
			boolean isThreefold = checkForRepetitionDueToPositionInSearchTree(eval.trans.getBestMove());
			if (isThreefold || (!isThreefold && (eval.trans.getScore() == 0))) {
				// Assume it is now a draw, so re-search
				SearchDebugAgent.printHashIsSeedMoveList(eval.trans.getBestMove(), pos.getHash());
				theScore = searchMoves( eval.trans.getBestMove(), eval.trans);
				break;
			} else {
				short adjustedScoreForThisPositionInTree = st.adjustHashTableMateInXScore(currPly, eval.trans.getScore());
				if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
					pc.update(currPly, eval.trans.getPv());
				} else {
					pc.set(currPly, eval.trans.getBestMove());
				}
				theScore = new Score(adjustedScoreForThisPositionInTree, eval.trans.getType());
				pe.invalidatePawnCache();
			}
			if (EubosEngineMain.UCI_INFO_ENABLED)
				sm.incrementNodesSearched();
			break;
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(eval.trans.getBestMove(), pos.getHash());
			prevBestMove = eval.trans.getBestMove();
			// intentional drop through
		case insufficientNoData:
			theScore = searchMoves( prevBestMove, eval.trans);
			break;	
		default:
			break;
		}
		handleEarlyTermination();
		clearUpSearchAtPly();
		
		return theScore;
	}
	
	private boolean checkForRepetitionDueToPositionInSearchTree(int move) throws InvalidPieceException {
		boolean retVal = false;
		if (move != Move.NULL_MOVE) {
			pm.performMove(move);
			SearchDebugAgent.nextPly();
			if (pos.isThreefoldRepetitionPossible()) {
				SearchDebugAgent.printRepeatedPositionHash(pos.getHash(), pos.getFen());
				retVal = true;
			}
			pm.unperformMove();
			SearchDebugAgent.prevPly();
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
	
	private Score searchMoves(int prevBestMove, ITransposition trans) throws InvalidPieceException {
		Score theScore = null;	
		MoveList ml = getMoveList(prevBestMove);
        if (ml.isMateOccurred()) {
            theScore = new Score(sg.scoreMate(currPly), Score.exact);
            st.setBackedUpScoreAtPly(currPly, theScore);
            // We will now de-recurse, so should make sure the depth searched is correct
            setDepthSearchedInPly();
            short mateScoreForTable = (short)((theScore.getScore() < 0) ? Short.MIN_VALUE + 1 : Short.MAX_VALUE - 1);
			trans = tt.setTransposition(trans, getTransDepth(), mateScoreForTable, theScore.getType(), Move.NULL_MOVE);
        } else {
    		Iterator<Integer> move_iter = ml.getStandardIterator(isInExtendedSearch());
    		if (move_iter.hasNext()) {
    			theScore = actuallySearchMoves(ml, move_iter, trans);
    		} else {
    			// It is effectively a terminal node in extended search, so update the trans with null best move
    			// and return a *safe* exact position score back down the tree. (i.e. not a check).			
    			theScore = applySafestNormalMoveAndScore(ml);
    			SearchDebugAgent.printExtSearchNoMoves(theScore);
    			trans = tt.setTransposition(trans, (byte)0, theScore.getScore(), theScore.getType(), Move.NULL_MOVE);
    		}
        }
        return theScore;
    }

	private Score actuallySearchMoves(MoveList ml, Iterator<Integer> move_iter, ITransposition trans) throws InvalidPieceException {
		boolean backedUpScoreWasExact = false;
		boolean refutationFound = false;

		byte plyBound = pos.onMoveIsWhite() ? Score.lowerBound : Score.upperBound;
		Score plyScore = new Score(plyBound);
		int currMove = move_iter.next();
		pc.initialise(currPly, currMove);

		plyScore = initialiseScoreForSingularCaptureInExtendedSearch(ml, move_iter, plyBound, plyScore);

		while(!isTerminated()) {
			if (EubosEngineMain.UCI_INFO_ENABLED)
				pc.clearContinuationBeyondPly(currPly);
			Score positionScore = applyMoveAndScore(currMove);
			if (!isTerminated()) {
				// Rationale: this is when a score was backed up - at this instant update the depth searched
				setDepthSearchedInPly();
				
				if (st.isAlphaBetaCutOff(currPly, positionScore)) {
					plyScore = positionScore;
					trans = updateTranspositionTable(trans, plyBound, currMove, positionScore);
					refutationFound = true;
					SearchDebugAgent.printRefutationFound();
					break;    
				}
				
				if (doScoreBackup(positionScore)) {
					backedUpScoreWasExact = positionScore.getType() == Score.exact;
					plyScore = positionScore;
					updatePrincipalContinuation(currMove, positionScore.getScore());
					trans = updateTranspositionTable(trans, plyBound, currMove, positionScore);
					
				} else {
					// Update the position hash if the move is better than that previously stored at this position
					if (shouldUpdatePositionBoundScoreAndBestMove(plyBound, plyScore.getScore(), positionScore.getScore())) {
						plyScore = positionScore;
						trans = updateTranspositionTable(trans, plyBound, currMove, positionScore);
					}
				}
			}
			
			if (move_iter.hasNext()) {
				currMove = move_iter.next();
			} else {
				break;
			}
		}
		
		if (!isTerminated() && isInNormalSearch() && backedUpScoreWasExact && !refutationFound && trans != null) {
			promoteToExactScore(trans, plyScore);
		}
		
		return plyScore;
	}

	private void promoteToExactScore(ITransposition trans, Score plyScore) {
		// This is the only way a hash and score can be exact.
		if (trans.getDepthSearchedInPly() <= getTransDepth()) {
			// however we need to be careful that the depth is appropriate, we don't set exact for wrong depth...
			trans.setType(Score.exact);

			// found to be needed due to score discrepancies caused by refutations coming out of extended search...
			// Still needed 22nd October 2020.
			trans.setBestMove(pc.getBestMove(currPly));
			short scoreFromDownTree = updateMateScoresForEncodingMateDistanceInHashTable(plyScore);
			trans.setScore(scoreFromDownTree);

			SearchDebugAgent.printExactTrans(pos.getHash(), trans);
		}
		plyScore.setExact();
	}

	private ITransposition updateTranspositionTable(ITransposition trans, byte plyBound, int currMove,
			Score positionScore) {
		short scoreFromDownTree = updateMateScoresForEncodingMateDistanceInHashTable(positionScore);
		if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
			trans = tt.setTransposition(trans, getTransDepth(), scoreFromDownTree, plyBound, currMove, pc.toPvList(currPly));
		} else {
			trans = tt.setTransposition(trans, getTransDepth(), scoreFromDownTree, plyBound, currMove);
		}
		return trans;
	}

	private short updateMateScoresForEncodingMateDistanceInHashTable(Score positionScore) {
		//
		// Modify mate score (which is expressed in distance from root node in ply) to
		// distance from leaf node (which is what needs to be stored in the hash table)
		//
		short scoreFromDownTree = positionScore.getScore();
		if (positionScore.isMate()) {
			if (scoreFromDownTree < 0) {
				scoreFromDownTree = (short) (scoreFromDownTree - currPly);
			} else {
				scoreFromDownTree = (short) (scoreFromDownTree + currPly);
			}
		}
		return scoreFromDownTree;
	}

	private Score initialiseScoreForSingularCaptureInExtendedSearch(MoveList ml, Iterator<Integer> move_iter,
			byte plyBound, Score plyScore) throws InvalidPieceException {
		if (isInExtendedSearch() && !move_iter.hasNext()) {
			/*
			 * The idea is that if we are in an extended search, if there are normal moves available 
			 * and only a single "forced" capture, we shouldn't necessarily be forced into making that capture.
			 * The capture needs to improve the position score to get searched, otherwise this position can be treated as terminal.
			 * Note: This is only a problem for the PV search, all others will bring down alpha/beta score and won't 
			 * back up if the score for the capture is worse than that.
			 */
			Score provScore = st.getBackedUpScoreAtPly(currPly);
			boolean isProvisional = (provScore.getScore() == Short.MIN_VALUE || provScore.getScore() == Short.MAX_VALUE);
			if (isProvisional && ml.hasMultipleRegularMoves()) {
				plyScore = applySafestNormalMoveAndScore(ml);
				plyScore.type = plyBound;
				st.setBackedUpScoreAtPly(currPly, plyScore);
			}
		}
		return plyScore;
	}

	private void setDepthSearchedInPly() {
		currDepthSearchedInPly = isInNormalSearch() ? (byte)(originalSearchDepthRequiredInPly - currPly) : 1;
	}
	
	private void updatePrincipalContinuation(int currMove, short positionScore)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (EubosEngineMain.UCI_INFO_ENABLED && atRootNode() && sr != null) {
			sm.setHashFull(tt.getHashUtilisation());
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), positionScore);
			sr.reportPrincipalVariation();
		}
	}
	
	private void handleEarlyTermination() throws InvalidPieceException {
		if (atRootNode() && isTerminated()) {
			int pcBestMove = pc.getBestMove((byte)0);
			TranspositionEvaluation eval = tt.getTransposition(currPly, dynamicSearchLevelInPly);
			if (eval != null && eval.trans != null && eval.trans.getBestMove() != Move.NULL_MOVE) {
				int transBestMove = eval.trans.getBestMove();
				// Use current best knowledge about the position from the transposition table
				EubosEngineMain.logger.info(
						String.format("best is trans=%s", eval.trans.report()));
				if (!Move.areEqual(pcBestMove, transBestMove)) {
					/* This should be an assert mitigation, it should never occur and could be removed. */
					EubosEngineMain.logger.info(
							String.format("early term problem - trans and pc moves not equal: %s != %s", 
									Move.toString(transBestMove),
									Move.toString(pcBestMove)));
					if (!checkForRepetitionDueToPositionInSearchTree(transBestMove)) {
						// Check we don't return a drawing move!
						pc.set(0, transBestMove);
					}
				}
			}
			else if (lastPc != null) {
				// Set best move to the previous iteration search result
				EubosEngineMain.logger.info(
						String.format("best is lastPc=%s", Move.toString(lastPc.get(0))));
				pc.update(0, lastPc.get(0));
			} else {
				// Just return the current pc
				EubosEngineMain.logger.info(
						String.format("best is pc=%s", Move.toString(pcBestMove)));
			}
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

	private byte getTransDepth() {
		/* By design, extended searches always use depth zero; therefore ensuring partially 
           searched transpositions can only be used for seeding move lists */
		return isInNormalSearch() ? currDepthSearchedInPly: 0;
	}

	private boolean shouldUpdatePositionBoundScoreAndBestMove(
			byte plyBound, short plyScore, short positionScore) {
		boolean doUpdate = false;
		if (plyBound == Score.lowerBound) {
			if (positionScore > plyScore && positionScore != Short.MAX_VALUE)
				doUpdate = true;
		} else {
			if (positionScore < plyScore && positionScore != Short.MIN_VALUE)
				doUpdate = true;
		}
		return doUpdate;
	}

	private boolean doScoreBackup(Score positionScore) {
		boolean backupRequired = false;
		if (st.isBackUpRequired(currPly, positionScore)) {
			st.setBackedUpScoreAtPly(currPly, positionScore);
			backupRequired = true;
		}
		return backupRequired;
	}
		
	private MoveList getMoveList(int transBestMove) throws InvalidPieceException {
		MoveList ml = null;
		if (transBestMove != Move.NULL_MOVE) {
			// Use transposition best move or last principal continuation for seeding move list
			ml = new MoveList((PositionManager) pm, transBestMove);
		} else {
			ml = new MoveList((PositionManager) pm);
		}
		return ml;
	}
	
	private Score applyMoveAndScore(int currMove) throws InvalidPieceException {
		SearchDebugAgent.printPerformMove(currMove);
		pm.performMove(currMove, true);
		currPly++;
		SearchDebugAgent.nextPly();
		Score positionScore = assessNewPosition(currMove);
		pm.unperformMove(true);
		currPly--;
		SearchDebugAgent.prevPly();
		SearchDebugAgent.printUndoMove(currMove);
		
		if (EubosEngineMain.UCI_INFO_ENABLED)
			sm.incrementNodesSearched();
		return positionScore;
	}
	
	private Score applySafestNormalMoveAndScore(MoveList ml) throws InvalidPieceException {
		int currMove = ml.getSafestMove();
		if (EubosEngineMain.ASSERTS_ENABLED)
			assert currMove != Move.NULL_MOVE;
		SearchDebugAgent.printPerformMove(currMove);
		pm.performMove(currMove, false);
		currPly++;
		SearchDebugAgent.nextPly();
		// exact because it is a terminal node
		Score positionScore = pe.evaluatePosition();
		
		pm.unperformMove(false);
		currPly--;
		SearchDebugAgent.prevPly();
		SearchDebugAgent.printUndoMove(currMove);
		
		pc.update(currPly, currMove);
		
		if (EubosEngineMain.UCI_INFO_ENABLED)
			sm.incrementNodesSearched();
		return positionScore;
	}
	
	private Score assessNewPosition(int lastMove) throws InvalidPieceException {
		Score positionScore = null;
		if ( isTerminalNode(lastMove) ) {
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
			SearchDebugAgent.printRepeatedPositionSearch(pos.getHash(), pos.getFen());
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
}
