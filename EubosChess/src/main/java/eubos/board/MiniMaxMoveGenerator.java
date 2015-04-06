package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Bishop;
import eubos.pieces.King;
import eubos.pieces.Knight;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;
import eubos.pieces.Queen;
import eubos.pieces.Rook;

public class MiniMaxMoveGenerator extends MoveGenerator implements
		IMoveGenerator {
	
	private static final int SEARCH_DEPTH_IN_PLY = 4;
	private int scores[];
	private GenericMove pc[][];
	private static final boolean isDebugOn = true;
	private Piece.Colour initialOnMove;
	private boolean mateFound = false;
	private boolean stalemateFound = false;
	
	public class moveGenDebugAgent {
		private String indent = "";
		private boolean isActive=false;

		public moveGenDebugAgent( int currPly, boolean active ) {
			isActive = active;
			for (int i=0; i<currPly; i++) {
				indent += "\t";
			}
		}

		public void printPerformMove(int currPly, GenericMove currMove) {
			if (isActive)
				System.out.println(indent+"performMove("+currMove.toString()+") at Ply="+currPly);
		}

		private void printSearchPly(int currPly) {
			if (isActive)
				System.out.println(indent+"searchPly("+currPly+", "+bm.onMove.toString()+")");
		}

		private void printUndoMove(int currPly, GenericMove currMove) {
			if (isActive)
				System.out.println(indent+"undoMove("+currMove.toString()+") at Ply="+currPly);
		}

		private void printBackUpScore(int currPly, int positionScore) {
			if (isActive)
				System.out.println(indent+"backedUpScore:"+positionScore+" at Ply="+currPly);
		}

		private void printPrincipalContinuation(int currPly) {
			if (isActive) {
				System.out.print(indent+"principal continuation found: "+pc[currPly][currPly]);
				for ( int nextPly = currPly+1; nextPly < SEARCH_DEPTH_IN_PLY; nextPly++) {
					System.out.print(", "+pc[currPly][nextPly]);
				}
				System.out.print("\n");
			}
		}
		
		private void printMateFound( int currPly) {
			if (isActive)
				System.out.println(indent+"possible Checkmate found at Ply="+currPly);	
		}
		
		private void printRefutationFound( int currPly) {
			if (isActive)
				System.out.println(indent+"refutation found (cut-off search) at Ply="+currPly);	
		}
		
		private void printAlphaBetaCutOffLimit(int currPly, int score) {
			if (isActive)
				System.out.println(indent+"alpha beta brought down score:"+score+" at Ply="+currPly);				
		}
	}

	public MiniMaxMoveGenerator( BoardManager bm ) {
		super( bm );
		scores = new int[SEARCH_DEPTH_IN_PLY];
		pc = new GenericMove[SEARCH_DEPTH_IN_PLY][SEARCH_DEPTH_IN_PLY];
	}
	
	private int evaluatePosition(Board theBoard ) {
		// First effort does only the most simple calculation based on material
		Iterator<Piece> iter_p = theBoard.iterator();
		int materialEvaluation = 0;
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			int currValue = 0;
			if ( currPiece instanceof Pawn ) 
				currValue = 100;
			else if ( currPiece instanceof Rook )
				currValue = 500;
			else if ( currPiece instanceof Bishop )
				currValue = 320;
			else if ( currPiece instanceof Knight )
				currValue = 300;
			else if ( currPiece instanceof Queen )
				currValue = 900;
			else if ( currPiece instanceof King )
				currValue = 300000;
			if (currPiece.isBlack()) currValue = -currValue;
			materialEvaluation += currValue;
		}
		return materialEvaluation;
	}
	
	@Override
	public GenericMove findMove() throws NoLegalMoveException {
		// Register initialOnMove
		initialOnMove = bm.onMove;
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		searchPly(0);
		// Report the principal continuation and select the best move
		moveGenDebugAgent debug = new moveGenDebugAgent(0, true);
		debug.printPrincipalContinuation(0);
		GenericMove bestMove = pc[0][0];
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}

	private int searchPly(int currPly) {
		moveGenDebugAgent debug = new moveGenDebugAgent(currPly, isDebugOn);
		debug.printSearchPly(currPly);
		int alphaBetaCutOff = initNodeScoreAlphaBeta(debug, currPly);
		// Generate all moves at this position and test if the previous move in the
		// search tree led to either checkmate or stalemate.
		LinkedList<GenericMove> ml = generateMovesAtPosition();
		if (mateFound) {
			backupScoreForCheckmate(currPly);
			debug.printMateFound(currPly);
			mateFound = false;
		} else if (stalemateFound) {
			backupScoreForStalemate(currPly);
			stalemateFound = false;
		}		
		Iterator<GenericMove> move_iter = ml.iterator();
		// Iterate through all the moves for this ply; there will be none if a mate was detected...
		while( move_iter.hasNext()) {
			int positionScore = 0;
			// 1) Apply the next move in the list
			GenericMove currMove = move_iter.next();
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
			bm.undoPreviousMove();
			// 4) Back-up the position score and update the principal continuation
			if (backUpIsRequired(currPly, positionScore)) {
				scores[currPly]=positionScore;
				debug.printBackUpScore(currPly, positionScore);
				updatePrincipalContinuation(currPly, currMove);
				debug.printPrincipalContinuation(currPly);
			}
//			} else if ((alphaBetaCutOff != Integer.MAX_VALUE) && (alphaBetaCutOff != Integer.MIN_VALUE)) {
//				// Implement alpha beta cut-off, if a previously backed-up and bought down score was assigned.
//				debug.printRefutationFound(currPly);
//				break;
//			}
		}
		return scores[currPly];
	}

	private boolean backUpIsRequired(int currPly, int positionScore) {
		boolean backUpScore = false;
		if (bm.onMove == Colour.white) {
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

	private void backupScoreForStalemate(int currPly) {
		// Avoid stalemates by giving them a large penalty score.
		scores[currPly] = -300000;
		if (initialOnMove==Colour.black)
			scores[currPly] = -scores[currPly];
	}

	private void backupScoreForCheckmate(int currPly) {
		// Favour earlier mates (i.e. Mate-in-one over mate-in-three) by giving them a larger score.
		scores[currPly] = (SEARCH_DEPTH_IN_PLY-currPly)*300000;
		if (initialOnMove==Colour.black)
			scores[currPly] = -scores[currPly];
	}
	
	private boolean isTerminalNode(int currPly) {
		boolean isTerminalNode = false;
		if (currPly == (SEARCH_DEPTH_IN_PLY-1)) {
			isTerminalNode = true;
		}
		return isTerminalNode;
	}

	private void updatePrincipalContinuation(int currPly, GenericMove currMove) {
		// Update Principal Continuation
		pc[currPly][currPly]=currMove;
		for (int nextPly=currPly+1; nextPly < SEARCH_DEPTH_IN_PLY; nextPly++) {
			pc[currPly][nextPly]=pc[currPly+1][nextPly];
		}
	}

	private int initNodeScoreAlphaBeta(moveGenDebugAgent debug, int currPly) {
		// Initialise score at this node
		if (currPly==0 || currPly==1) {
			if (bm.onMove==Colour.white) {
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

	private LinkedList<GenericMove> generateMovesAtPosition() {
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		// Test if the King is in check at the start of the turn
		King ownKing = bm.getKing(bm.onMove);
		boolean kingIsInCheck = inCheck(ownKing);
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = bm.getTheBoard().iterateColour(bm.onMove);
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			entireMoveList.addAll( currPiece.generateMoves( bm ));
		}
		addCastlingMoves(entireMoveList);
		// Scratch any moves resulting in the king being in check
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			bm.performMove( currMove );
			if (inCheck(ownKing)) {
				iter_ml.remove();
			}
			bm.undoPreviousMove();
		}
		if (entireMoveList.isEmpty()) {
			if (kingIsInCheck && initialOnMove==Piece.Colour.getOpposite(bm.onMove)) {
				// Indicates checkmate! Perform an immediate backup of score and abort the 
				// search of any moves deeper than the previous node in the search tree. 
				// However, search the rest of the tree, as this may yield earlier forced mates.
				mateFound = true;
			} else {
				// Indicates a stalemate position.
				stalemateFound = true;
			}
		}
		return entireMoveList;
	}

}
