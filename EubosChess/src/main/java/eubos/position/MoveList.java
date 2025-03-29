package eubos.position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;
import com.fluxchess.jcpi.models.IntFile;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.search.History;

public class MoveList {

	private MoveListIterator[] ml;
	
	public History history;

	public MoveList(PositionManager pm, int orderMoveList) {
		// Create the move list arrays for this threads move list
		ml = new MoveListIterator[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		
		// Create history array
		history = new History();
	
		// Create the list at each ply
		for (int i = 0; i < EubosEngineMain.SEARCH_DEPTH_IN_PLY; i++) {
			ml[i] = new MoveListIterator(history, pm, orderMoveList, i);
		}
	}
	
	public MoveListIterator initialiseAtPly(int bestMove, int[] killers, boolean inCheck, boolean extended, int ply) {
		return ml[ply].initialise(bestMove, killers, inCheck, extended);
	}

	static public int getRandomMove(PositionManager pm) {
		MoveListIterator it = new MoveListIterator(new History(), pm, 0, 0);
		it.initialise(Move.NULL_MOVE, null, pm.isKingInCheck(), false);
		int randomMove = Move.NULL_MOVE;
		int moveCount = 0;
		int moves[] = new int[110];
		
		while (it.hasNext()) {
			int currentMove = it.nextInt();
			if (pm.performMove(currentMove)) {
				pm.unperformMove();
				moves[moveCount] = currentMove;
				moveCount += 1;
			}
		}
		if (moveCount != 0) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(moveCount);
			randomMove = moves[indexToGet];			
		}
		return randomMove;
	}
	
	static public int getForcedMove(PositionManager pm) {
		MoveListIterator it = new MoveListIterator(new History(), pm, 0, 0);
		int currMove = Move.NULL_MOVE;
		int forcedMove = Move.NULL_MOVE;
		
		while (it.hasNext()) {
			currMove = it.nextInt();
			if (pm.performMove(currMove)) {
				pm.unperformMove();
				if (forcedMove == Move.NULL_MOVE) {
					forcedMove = currMove;
				} else {
					// If we already have found a valid move, then it can't be a forced move,
					// as at least one other valid move is present in the position
					forcedMove = Move.NULL_MOVE;
					break;
				}
			}
		}
		return forcedMove;
	}

	// ---------------------------------------------------------------------------------------------
	// Test APIs
	//
	public List<Integer> getList(MoveListIterator it) {
		List<Integer> ml = new ArrayList<Integer>();
		while (it.hasNext()) {
			ml.add(it.nextInt());
		}
		return ml;
	}
	
	static private int getPosition(String notation) {
		if (notation.length() == 2) {
			GenericFile file;
			GenericRank rank;
			if (GenericFile.isValid(notation.charAt(0))) {
				file = GenericFile.valueOf(notation.charAt(0));
				if (GenericRank.isValid(notation.charAt(1))) {
				    rank = GenericRank.valueOf(notation.charAt(1));
				    return Position.valueOf(GenericPosition.valueOf(file, rank));
			    }
			}
		}
		return Position.NOPOSITION;
	}
	
	static private int getMoveDisambiguatedByFile(String notation, List<Integer> moveList, int originPiece) {
		if (GenericFile.isValid(notation.charAt(1))) {
			GenericFile file = GenericFile.valueOf(notation.charAt(0));
			int targetSquare = getPosition(notation.substring(1));
			for (int move : moveList) {
		    	if ((BitBoard.bitToPosition_Lut[Move.getTargetPosition(move)] == targetSquare) &&
		    		(Move.getOriginPiece(move) == originPiece) && 
		    		(BitBoard.getFile(Move.getOriginPosition(move)) == IntFile.valueOf(file)))
		    		return move;
		    }
		}
		return Move.NULL_MOVE;
	}
	
	static private int getMove(String notation, List<Integer> moveList, int originPiece) {
		int targetSquare = getPosition(notation);
	    for (int move : moveList) {
	    	if ((BitBoard.bitToPosition_Lut[Move.getTargetPosition(move)] == targetSquare) &&
	    		(Move.getOriginPiece(move) == originPiece))
	    		return move;
	    }
	    return Move.NULL_MOVE;
	}
	
	static private int getPromotionMove(List<Integer> moveList, String notation, int originPiece) {
		int targetSquare = getPosition(notation.substring(0,2));
		char promoPiece = notation.charAt(2);
		int promo = Piece.NONE;
		switch(promoPiece) {
		case 'Q':
			promo = Piece.QUEEN;
			break;
		case 'R':
			promo = Piece.ROOK;
			break;
		case 'B':
			promo = Piece.BISHOP;
			break;
		case 'N':
			promo = Piece.KNIGHT;
			break;
		default:
			break;
		}
		for (int move : moveList) {
	    	if ((BitBoard.bitToPosition_Lut[Move.getTargetPosition(move)] == targetSquare) &&
	    		(Move.getOriginPiece(move) == originPiece) && 
	    		(Move.getPromotion(move) == promo))
	    		return move;
	    }
		return Move.NULL_MOVE;
	}
	
	static private int getPieceMove(List<Integer> moveList, String notation, int originPiece) {
		if (notation.length() == 3) {
			return getMoveDisambiguatedByFile(notation, moveList, originPiece);
		} else {
			return getMove(notation, moveList, originPiece);
		}
	}
	
	static private int getPawnMove(List<Integer> moveList, String notation, int originPiece) {
		if (notation.length() == 2) {
			return getMove(notation, moveList, originPiece);
		} else if (notation.length() == 3) {
			int disambiguated_move = getMoveDisambiguatedByFile(notation, moveList, originPiece);
			if (disambiguated_move != Move.NULL_MOVE) {
				return disambiguated_move;
			} else {
				return getPromotionMove(moveList, notation, originPiece);
			}
		}
		return Move.NULL_MOVE;
	}
	
	static private String preprocessSAN(String notation) {
	    notation = notation.trim();
	    notation = notation.replaceAll(" ", "");
	    notation = notation.replaceAll("x", "");
	    notation = notation.replaceAll(":", "");
	    notation = notation.replaceAll("=", "");
	    notation = notation.replaceAll("\\+", "");
	    notation = notation.replaceAll("#", "");
	    notation = notation.replaceAll("-", "");
	    return notation;
	}
	
	public static int getNativeMove(boolean isWhite, List<Integer> moveList, String notation) {
		int move = Move.NULL_MOVE;		
		notation = preprocessSAN(notation);
		switch(notation.charAt(0)) {
		case 'K':
			move = getPieceMove(moveList, notation.replaceAll("K", ""), isWhite ? Piece.WHITE_KING: Piece.BLACK_KING);
			break;
		case 'Q':
			move = getPieceMove(moveList, notation.replaceAll("Q", ""), isWhite ? Piece.WHITE_QUEEN: Piece.BLACK_QUEEN);
			break;
		case 'R':
			move = getPieceMove(moveList, notation.replaceAll("R", ""), isWhite ? Piece.WHITE_ROOK: Piece.BLACK_ROOK);
			break;
		case 'B':
			move = getPieceMove(moveList, notation.replaceAll("B", ""), isWhite ? Piece.WHITE_BISHOP: Piece.BLACK_BISHOP);
			break;
		case 'N':
			move = getPieceMove(moveList, notation.replaceAll("N", ""), isWhite ? Piece.WHITE_KNIGHT: Piece.BLACK_KNIGHT);
			break;
			// Pawn moves
		case 'a': case 'b':	case 'c':case 'd': case 'e': case 'f': case 'g': case 'h':
			move = getPawnMove(moveList, notation, isWhite ? Piece.WHITE_PAWN: Piece.BLACK_PAWN);
			break;
		case 'O':
			// Castling
			if (notation.matches("OOO")) {
				move = isWhite ? CastlingManager.wqsc : CastlingManager.bqsc;
			} else {
				move = isWhite ? CastlingManager.wksc : CastlingManager.bksc;
			}
			if (moveList.indexOf(move) == -1) move = Move.NULL_MOVE;
			break;
		default:
			break;
		}
		return move;
	}
}
