package eubos.search;

import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;
import eubos.main.EubosEngineMain;

class MiniMaxMoveGenerator implements
		IMoveGenerator {

	private BoardManager bm;
	private int searchDepthPly;
	private int scores[];
	private PrincipalContinuation pc;
	private Colour initialOnMove;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	private boolean sendInfo = false;
	private boolean terminate = false;
	private SearchDebugAgent debug;
	
	private static final int KING_VALUE = 300000;
	private static final int QUEEN_VALUE = 900;
	private static final int ROOK_VALUE = 500;
	private static final int BISHOP_VALUE = 320;
	private static final int KNIGHT_VALUE = 300;
	private static final int PAWN_VALUE = 100;

	// Used for unit tests
	MiniMaxMoveGenerator( BoardManager bm, int searchDepth ) {
		this.bm = bm;
		scores = new int[searchDepth];
		searchDepthPly = searchDepth;
		pc = new PrincipalContinuation(searchDepth);
		sm = new SearchMetrics(searchDepth);
		debug = new SearchDebugAgent(0);
	}

	// Used with Arena
	MiniMaxMoveGenerator( EubosEngineMain eubos, BoardManager bm, int searchDepth ) {
		this(bm, searchDepth);
		sm.setPrincipalVariation(pc.toPvList());
		sr = new SearchMetricsReporter(eubos,sm);
		sendInfo = true;
	}	

	@Override
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException {
		// Register initialOnMove
		initialOnMove = bm.getOnMove();
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
		debug.printSearchPly(currPly,bm.getOnMove());
		int alphaBetaCutOff = initNodeScoreAlphaBeta(currPly);
		// Generate all moves at this position.
		List<GenericMove> ml = bm.getMoveList();
		if (ml.isEmpty()) {
			// Handle mates (indicated by no legal moves)
			if (bm.isKingInCheck()) {
				backupScoreForCheckmate(currPly);
				debug.printMateFound(currPly);
			} else {
				backupScoreForStalemate(currPly);
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
				bm.performMove(currMove);
				// 2) Either recurse or evaluate position and check for back-up of score
				if ( isTerminalNode(currPly) ) {
					positionScore = evaluatePosition(bm.getTheBoard());
				} else {
					positionScore = searchPly(currPly+1);
				}
				// 3) Having assessed the position, undo the move
				debug.printUndoMove(currPly, currMove);
				bm.unperformMove();
				sm.incrementNodesSearched();
				// 4a) Back-up the position score and update the principal continuation...
				if (backUpIsRequired(currPly, positionScore)) {
					scores[currPly]=positionScore;
					debug.printBackUpScore(currPly, positionScore);
					pc.update(currPly, currMove);
					debug.printPrincipalContinuation(currPly,pc);
					reportPrincipalContinuation(currPly, positionScore);
					// 4b) ...or test for an Alpha Beta algorithm cut-off
				} else if (testForAlphaBetaCutOff( alphaBetaCutOff, positionScore, currPly )) {
					debug.printRefutationFound(currPly);
					break;
				}
			}
		}
		return scores[currPly];
	}
	
	private void reportPrincipalContinuation(int currPly, int positionScore) {
		if (currPly == 0) {
			if (Math.abs(positionScore) > KING_VALUE) {
				// If the positionScore indicates a mate, truncate the pc accordingly
				int matePly = Math.abs(positionScore)/KING_VALUE;
				matePly *= 2;
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

	private boolean backUpIsRequired(int currPly, int positionScore) {
		boolean backUpScore = false;
		if (bm.getOnMove() == Colour.white) {
			// if white, maximise score
			if (positionScore > scores[currPly])
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < scores[currPly])
				backUpScore = true;
		}
		return backUpScore;
	}
	
	private boolean testForAlphaBetaCutOff(int cutOffValue, int positionScore, int currPly) {
		if ((cutOffValue != Integer.MAX_VALUE) && (cutOffValue != Integer.MIN_VALUE)) {
			if ((bm.getOnMove() == Colour.white && positionScore >= scores[currPly-1]) ||
					(bm.getOnMove() == Colour.black && positionScore <= scores[currPly-1])) {
				return true;
			}
		}
		return false;
	}

	private void backupScoreForStalemate(int currPly) {
		// Avoid stalemates by giving them a large penalty score.
		scores[currPly] = -KING_VALUE;
		if (initialOnMove==Colour.black)
			scores[currPly] = -scores[currPly];
	}

	private void backupScoreForCheckmate(int currPly) {
		// Perform an immediate backup of score and abort the search of any moves deeper
		// than the previous node in the search tree. However, search the rest of the tree,
		// as this may yield earlier forced mates.
		
		// Favour earlier mates (i.e. Mate-in-one over mate-in-three) by giving them a larger score.
		int totalMovesSearched = searchDepthPly/2; // factor of 2 because two plies in a move.
		int mateMoveNum = (currPly-1)/2; // currPly-1 because mate was caused by the move from the previousPly
		int multiplier = totalMovesSearched-mateMoveNum;
		scores[currPly] = multiplier*KING_VALUE;
		// Note the check on whether own king is checkmated (2nd expression). Ensures correct score backup.
		if (initialOnMove==Colour.black && initialOnMove!=bm.getOnMove())
			scores[currPly] = -scores[currPly];
	}

	private int initNodeScoreAlphaBeta(int currPly) {
		// Initialise score at this node
		if (currPly==0 || currPly==1) {
			if (bm.getOnMove()==Colour.white) {
				scores[currPly] = Integer.MIN_VALUE;
			} else {
				scores[currPly] = Integer.MAX_VALUE;
			}
		} else {
			// alpha beta algorithm: bring down score from 2 levels up tree
			debug.printAlphaBetaCutOffLimit(currPly, scores[currPly-2]);
			scores[currPly] = scores[currPly-2];
		}
		return scores[currPly];
	}

	private int evaluatePosition(Board theBoard ) {
		// First effort does only the most simple calculation based on material
		Iterator<Piece> iter_p = theBoard.iterator();
		int materialEvaluation = 0;
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			int currValue = 0;
			if ( currPiece instanceof Pawn ) 
				currValue = PAWN_VALUE;
			else if ( currPiece instanceof Rook )
				currValue = ROOK_VALUE;
			else if ( currPiece instanceof Bishop )
				currValue = BISHOP_VALUE;
			else if ( currPiece instanceof Knight )
				currValue = KNIGHT_VALUE;
			else if ( currPiece instanceof Queen )
				currValue = QUEEN_VALUE;
			else if ( currPiece instanceof King )
				currValue = KING_VALUE;
			if (currPiece.isBlack()) currValue = -currValue;
			materialEvaluation += currValue;
		}
		return materialEvaluation;
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
