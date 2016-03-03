package eubos.search;

import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.PositionManager;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;
import eubos.main.EubosEngineMain;

class MiniMaxMoveGenerator implements
		IMoveGenerator {

	private PositionManager pm;
	private int searchDepthPly;
	private ScoreTracker st;
	private ScoreGenerator sg;
	private PrincipalContinuation pc;
	private Colour initialOnMove;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	private boolean sendInfo = false;
	private boolean terminate = false;
	private SearchDebugAgent debug;

	// Used for unit tests
	MiniMaxMoveGenerator( PositionManager pm, int searchDepth ) {
		this.pm = pm;
		st = new ScoreTracker(searchDepth);
		sg = new ScoreGenerator(searchDepth);
		searchDepthPly = searchDepth;
		pc = new PrincipalContinuation(searchDepth);
		sm = new SearchMetrics(searchDepth);
		debug = new SearchDebugAgent(0);
	}

	// Used with Arena
	MiniMaxMoveGenerator( EubosEngineMain eubos, PositionManager pm, int searchDepth ) {
		this(pm, searchDepth);
		sm.setPrincipalVariation(pc.toPvList());
		sr = new SearchMetricsReporter(eubos,sm);
		sendInfo = true;
	}	

	@Override
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException {
		// Register initialOnMove
		initialOnMove = pm.getOnMove();
		// Start the search reporter task
		if (sendInfo)
			sr.start();
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		searchPly(0);
		if (sendInfo) {
			sr.end();
			sr.reportNodeData();
		}
		// Select the best move
		GenericMove bestMove = pc.getBestMove();
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}

	private int searchPly(int currPly) throws InvalidPieceException {
		Colour onMove = pm.getOnMove();
		boolean isWhite = (onMove == Colour.white);
		debug.printSearchPly(currPly,onMove);
		int alphaBetaCutOff = st.initScore(currPly,isWhite);
		// Generate all moves at this position.
		List<GenericMove> ml = pm.getMoveList();
		if (ml.isEmpty()) {
			// Handle mates (indicated by no legal moves)
			if (pm.isKingInCheck()) {
				int mateScore = sg.generateScoreForCheckmate(currPly);
				mateScore = negateCheckmateScoreIfRequired(mateScore);
				st.backupScore(currPly, mateScore);
				debug.printMateFound(currPly);
			} else {
				int mateScore = sg.getScoreForStalemate();
				mateScore = negateStalemateScoreIfRequired(mateScore);
				st.backupScore(currPly, mateScore);
			}
		} else {
			// Iterate through all the moves for this ply
			Iterator<GenericMove> move_iter = ml.iterator();
			while(move_iter.hasNext() && !isTerminated()) {
				int positionScore = 0;
				// 1) Apply the next move in the list
				GenericMove currMove = move_iter.next();
				reportNextMove(currPly, currMove);
				debug.printPerformMove(currPly, currMove);
				pm.performMove(currMove);
				// 2) Either recurse or evaluate position and check for back-up of score
				if ( isTerminalNode(currPly) ) {
					positionScore = sg.generateScoreForPosition(pm.getTheBoard());
				} else {
					positionScore = searchPly(currPly+1);
				}
				// 3) Having assessed the position, undo the move
				debug.printUndoMove(currPly, currMove);
				pm.unperformMove();
				sm.incrementNodesSearched();
				// 4) Evaluate the score
				if (isBackUpRequired(currPly, positionScore)) {
					// 4a) Back-up the position score and update the principal continuation...
					st.backupScore(currPly, positionScore);
					debug.printBackUpScore(currPly, positionScore);
					pc.update(currPly, currMove);
					debug.printPrincipalContinuation(currPly,pc);
					reportPrincipalContinuation(currPly, positionScore);
				} else if (isAlphaBetaCutOff( alphaBetaCutOff, positionScore, currPly )) {
					// 4b) Perform an Alpha Beta algorithm cut-off
					debug.printRefutationFound(currPly);
					break;
				}
			}
		}
		return st.getBackedUpScore(currPly);
	}

	private void reportPrincipalContinuation(int currPly, int positionScore) {
		if (currPly == 0) {
			if (Math.abs(positionScore) > ScoreGenerator.KING_VALUE) {
				// If the positionScore indicates a mate, truncate the pc accordingly
				// TODO: possible bug here when considering black being mated?
				int matePly = Math.abs(positionScore)/ScoreGenerator.KING_VALUE;
				matePly *= ScoreGenerator.PLIES_PER_MOVE;
				matePly = searchDepthPly - matePly;
				if (initialOnMove == Colour.black) {
					matePly += 1;
				}
				pc.clearAfter(matePly);
			}
			sm.setPrincipalVariation(pc.toPvList());
			if (initialOnMove.equals(Colour.black))
				positionScore = -positionScore; // Negated due to UCI spec (from engine pov)
			sm.setCpScore(positionScore);
			if (sendInfo)
				sr.reportPrincipalVariation();
		}
	}

	private void reportNextMove(int currPly, GenericMove currMove) {
		if (currPly == 0) {
			sm.setCurrentMove(currMove);
			sm.incrementCurrentMoveNumber();
			if (sendInfo)
				sr.reportCurrentMove();
		}
	}
	
	private int negateStalemateScoreIfRequired(int mateScore) {
		if (initialOnMove==Colour.black)
			mateScore=-mateScore;
		return mateScore;
	}

	private int negateCheckmateScoreIfRequired(int mateScore) {
		// TODO: I am sceptical about whether this is correct!
		// Note the check on whether own king is checkmated (2nd expression in each &&). Ensures correct score backup.
		if ((initialOnMove==Colour.black && initialOnMove!=pm.getOnMove()) || 
			(initialOnMove==Colour.white && initialOnMove==pm.getOnMove()))
			mateScore=-mateScore;
		return mateScore;
	}

	private boolean isBackUpRequired(int currPly, int positionScore) {
		boolean backUpScore = false;
		if (pm.getOnMove() == Colour.white) {
			// if white, maximise score
			if (positionScore > st.getBackedUpScore(currPly))
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < st.getBackedUpScore(currPly))
				backUpScore = true;
		}
		return backUpScore;
	}
	
	private boolean isAlphaBetaCutOff(int cutOffValue, int positionScore, int currPly) {
		if ((cutOffValue != Integer.MAX_VALUE) && (cutOffValue != Integer.MIN_VALUE)) {
			int prevPlyScore = st.getBackedUpScore(currPly-1);
			Colour onMove = pm.getOnMove();
			if ((onMove == Colour.white && positionScore >= prevPlyScore) ||
				(onMove == Colour.black && positionScore <= prevPlyScore)) {
				// Indicates the search passed this node is refuted by an earlier move.
				return true;
			}
		}
		return false;
	}

	private boolean isTerminalNode(int currPly) {
		boolean isTerminalNode = false;
		if (currPly == (searchDepthPly-1)) {
			isTerminalNode = true;
		}
		return isTerminalNode;
	}
	
	synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }
}
