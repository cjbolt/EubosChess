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
	
	private static final int SEARCH_DEPTH_IN_PLY = 8;
	private int scores[];
	private GenericMove pc[][];
	private static final boolean isDebugOn = false;
	
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
		
		private void printSearchPly(int nextPly, Piece.Colour colourAtNextPly) {
			if (isActive)
				System.out.println(indent+"searchPly("+nextPly+", "+colourAtNextPly.toString()+")");
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
				currPly+=1;
				while ( currPly < SEARCH_DEPTH_IN_PLY) {
					System.out.print(", "+pc[0][currPly++]);
				}
				System.out.print("\n");
			}
		}
	}
	
	public MiniMaxMoveGenerator( BoardManager bm, Piece.Colour sideToMove ) {
		super( bm, sideToMove);
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
				currValue = 30000;
			if (currPiece.isBlack()) currValue = -currValue;
			materialEvaluation += currValue;
		}
		return materialEvaluation;
	}
	
	@Override
	public GenericMove findMove() throws NoLegalMoveException {
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		searchPly(0, onMove);
		// Report the principal continuation and select the best move
		moveGenDebugAgent debug = new moveGenDebugAgent(0, true /*isDebugOn*/);
		debug.printPrincipalContinuation(0);
		GenericMove bestMove = pc[0][0];
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}

	private boolean searchPly(int currPly, Piece.Colour toPlay) {
		boolean everBackedUpScore = false;
		moveGenDebugAgent debug = new moveGenDebugAgent(currPly, isDebugOn);
		boolean isTerminalNode = false;
		if (currPly == (SEARCH_DEPTH_IN_PLY-1)) {
			// Signal if this is a terminal node in the search tree, as different processing occurs
			isTerminalNode = true;
		}
		initNodeScore(currPly, toPlay);
		LinkedList<GenericMove> ml = generateMovesAtPosition(toPlay);
		Iterator<GenericMove> move_iter = ml.iterator();
		// Iterate through all the moves for this ply
		while( move_iter.hasNext()) {
			boolean backUpScore = false;
			int positionScore = 0;
			// 1) Apply the move
			GenericMove currMove = move_iter.next();
			debug.printPerformMove(currPly, currMove);
			bm.performMove(currMove);
			// 2) Either recurse or evaluate position and check for back-up of score
			if ( isTerminalNode ) {
				positionScore = evaluatePosition(bm.getTheBoard());
				if (toPlay == Colour.white) {
					if (positionScore > scores[currPly]) {
						backUpScore = true;
					}
				} else {
					if (positionScore < scores[currPly]) {
						backUpScore = true;
					}
				}
			} else {
				int nextPly = currPly+1;
				Piece.Colour colourAtNextPly = Piece.Colour.getOpposite(toPlay);
				debug.printSearchPly(nextPly, colourAtNextPly);
				backUpScore = searchPly(nextPly, colourAtNextPly);
			}
			// 3) Undo the move
			debug.printUndoMove(currPly, currMove);
			bm.undoPreviousMove();
			if (backUpScore) {
				boolean writeScore = false;
				everBackedUpScore = true;
				if (!isTerminalNode) {
					positionScore=scores[currPly+1];
					if (toPlay == Colour.white) {
						if (positionScore > scores[currPly]) {
							writeScore = true;
						}
					} else {
						if (positionScore < scores[currPly]) {
							writeScore = true;
						}
					}
				} else {
					writeScore = true;
				}
				if (writeScore) {
					scores[currPly]=positionScore;
					debug.printBackUpScore(currPly, positionScore);
					updatePrincipalContinuation(currPly, isTerminalNode, currMove);
					debug.printPrincipalContinuation(currPly);
				}
			}
		}
		return everBackedUpScore;
	}

	private void updatePrincipalContinuation(int currPly,
			boolean isTerminalNode, GenericMove currMove) {
		// Update Principal Continuation
		pc[currPly][currPly]=currMove;
		if (!isTerminalNode){
			// back up the rest of the pc array...
			for (int nextPly=currPly+1; nextPly < SEARCH_DEPTH_IN_PLY; nextPly++) {
				pc[currPly][nextPly]=pc[nextPly][nextPly];
			}
		}
	}

	private void initNodeScore(int currPly, Piece.Colour toPlay) {
		// Initialise score at this node
		if (toPlay==Colour.white) {
			scores[currPly] = Integer.MIN_VALUE;
		} else {
			scores[currPly] = Integer.MAX_VALUE;
		}
	}

	private LinkedList<GenericMove> generateMovesAtPosition(Piece.Colour colour) {
		// First step is to generate all positions in deepest position in tree and back them up...
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = bm.getTheBoard().iterateColour(colour);
		while ( iter_p.hasNext() ) {
			entireMoveList.addAll( iter_p.next().generateMoves( bm ));
		}
		addCastlingMoves(entireMoveList);
		// Scratch any moves resulting in the king being in check
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			bm.performMove( currMove );
			if (inCheck()) {
				iter_ml.remove();
			}
			bm.undoPreviousMove();
		}
		return entireMoveList;
	}

}
