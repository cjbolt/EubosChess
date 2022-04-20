package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntChessman;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import it.unimi.dsi.fastutil.ints.IntComparator;

/* This class represents a move as a integer primitive value. */
public final class Move {
	
	private static final int TARGET_PIECE_SHIFT = 0;
	private static final int TARGET_PIECE_MASK = Piece.PIECE_WHOLE_MASK << TARGET_PIECE_SHIFT;
	private static final int TARGET_PIECE_NO_COLOUR_MASK = Piece.PIECE_NO_COLOUR_MASK << TARGET_PIECE_SHIFT;
	
	private static final int TARGETPOSITION_SHIFT = TARGET_PIECE_SHIFT+Long.bitCount(Piece.PIECE_WHOLE_MASK);
	private static final int TARGETPOSITION_MASK = Position.MASK << TARGETPOSITION_SHIFT;
	
	private static final int ORIGINPOSITION_SHIFT = TARGETPOSITION_SHIFT+Long.bitCount(Position.MASK);
	private static final int ORIGINPOSITION_MASK = Position.MASK << ORIGINPOSITION_SHIFT;
	
	private static final int PROMOTION_SHIFT = ORIGINPOSITION_SHIFT+Long.bitCount(Position.MASK);
	private static final int PROMOTION_MASK = Piece.PIECE_NO_COLOUR_MASK << PROMOTION_SHIFT;
	
	private static final int ORIGIN_PIECE_SHIFT = PROMOTION_SHIFT+Long.bitCount(Piece.PIECE_NO_COLOUR_MASK);
	private static final int ORIGIN_PIECE_MASK = Piece.PIECE_WHOLE_MASK << ORIGIN_PIECE_SHIFT;
	private static final int ORIGIN_PIECE_MASK_NO_COLOUR = Piece.PIECE_NO_COLOUR_MASK << ORIGIN_PIECE_SHIFT;
	
	// Move ordering
	public static final int TYPE_REGULAR_NONE = 0;
	public static final int TYPE_KILLER_BIT = 0;
	public static final int TYPE_CAPTURE_BIT = 1;
	public static final int TYPE_PROMOTION_BIT = 2;
	public static final int TYPE_BEST_BIT = 3;
	public static final int TYPE_WIDTH = TYPE_BEST_BIT + 1;

	public static final int TYPE_KILLER_MASK = (0x1 << TYPE_KILLER_BIT);
	public static final int TYPE_CAPTURE_MASK = (0x1 << TYPE_CAPTURE_BIT);
	public static final int TYPE_PROMOTION_MASK = (0x1 << TYPE_PROMOTION_BIT);
	public static final int TYPE_BEST_MASK = (0x1 << TYPE_BEST_BIT);
	
	public static final int MOVE_ORDERING_MASK = (TYPE_KILLER_MASK | TYPE_CAPTURE_MASK | TYPE_PROMOTION_MASK | TYPE_BEST_MASK);
	public static final int KILLER_EXCLUSION_MASK = (TYPE_CAPTURE_MASK | TYPE_PROMOTION_MASK);
	
	public static final int TYPE_SHIFT = ORIGIN_PIECE_SHIFT + Long.bitCount(Piece.PIECE_WHOLE_MASK);
	private static final int TYPE_MASK = ((1<<TYPE_WIDTH)-1) << TYPE_SHIFT;
	
	// Misc flags
	public static final int MISC_EN_PASSANT_CAPTURE_BIT = 0;
	private static final int MISC_SHIFT = TYPE_SHIFT + Long.bitCount(TYPE_MASK);
	public static final int MISC_EN_PASSANT_CAPTURE_MASK = (0x1 << (MISC_EN_PASSANT_CAPTURE_BIT+ MISC_SHIFT));
	
	public static final int NULL_MOVE =
			valueOf(TYPE_REGULAR_NONE, Position.a1, Piece.NONE, Position.a1, Piece.NONE, Piece.NONE);
	
	public static final int EQUALITY_MASK = ORIGINPOSITION_MASK | TARGETPOSITION_MASK | PROMOTION_MASK;
	public static final int BEST_KILLER_EQUALITY_MASK = ORIGINPOSITION_MASK | ORIGIN_PIECE_MASK | TARGETPOSITION_MASK | TARGET_PIECE_MASK | PROMOTION_MASK;
	
	public static int valueOf(int originPosition, int originPiece, int targetPosition, int targetPiece) {
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (targetPiece & ~Piece.PIECE_WHOLE_MASK) == 0;
		int move = targetPiece;
		
		// Encode Target Piece and classification if a capture
		if (targetPiece != Piece.NONE) {
			move |= Move.TYPE_CAPTURE_MASK << TYPE_SHIFT;
		}

		// Encode origin position
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (originPosition & 0x88) == 0;
		move |= originPosition << ORIGINPOSITION_SHIFT;
		
		// Encode Origin Piece
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (originPiece & ~Piece.PIECE_WHOLE_MASK) == 0;
		move |= originPiece << ORIGIN_PIECE_SHIFT;
		
		// Encode target position
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;
		
		return move;
	}
	
	public static int valueOfEnPassant(int enPassant, int type, int originPosition, int originPiece, int targetPosition, int targetPiece, int promotion) {
		int move = Move.valueOf(type, originPosition, originPiece, targetPosition, targetPiece, promotion);
		
		// Encode enPassant - single bit, doesn't need masking
		move |= enPassant;
		
		return move;
	}

	public static int valueOf(int type, int originPosition, int originPiece, int targetPosition, int targetPiece, int promotion) {
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (targetPiece & ~Piece.PIECE_WHOLE_MASK) == 0;
		int move = targetPiece;

		// Encode Target Piece and classification if a capture
		if (targetPiece != Piece.NONE) {
			move |= (type | Move.TYPE_CAPTURE_MASK) << TYPE_SHIFT;

		} else {
			// Encode move classification
			if (EubosEngineMain.ENABLE_ASSERTS)
				assert (type & ~(Move.TYPE_MASK >>> TYPE_SHIFT)) == 0;
			move |= type << TYPE_SHIFT;
		}
			
		// Encode Origin Piece
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (originPiece & ~Piece.PIECE_WHOLE_MASK) == 0;
		move |= originPiece << ORIGIN_PIECE_SHIFT;

		// Encode Origin position
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (originPosition & 0x88) == 0;
		move |= originPosition << ORIGINPOSITION_SHIFT;
		
		// Encode target position
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;
				
		// Encode promotion
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert promotion != Piece.KING && promotion != Piece.PAWN && (promotion & ~Piece.PIECE_NO_COLOUR_MASK) == 0;
		}
		move |= promotion << PROMOTION_SHIFT;

		return move;
	}
	
	public static int toMove(GenericMove move, Board theBoard, int type) {
		int intMove = 0;
		int targetPosition = Position.valueOf(move.to);
		int originPosition = Position.valueOf(move.from);
		int promotion = Piece.NONE;
		int originPiece = Piece.NONE;
		int targetPiece = Piece.NONE;
		int misc = 0;
		if (theBoard != null) {
			// Some unit tests don't specify the board, when we don't care about some move field content
			originPiece = theBoard.getPieceAtSquare(originPosition);
			if (Piece.isPawn(originPiece) && targetPosition==theBoard.getEnPassantTargetSq()) {
				// En Passant capture move
				int enPassantCaptureSquare = theBoard.generateCapturePositionForEnPassant(originPiece, targetPosition);
				targetPiece = theBoard.getPieceAtSquare(enPassantCaptureSquare);
				if (Piece.isPawn(targetPiece)) {
					misc |= Move.MISC_EN_PASSANT_CAPTURE_MASK;
				}
			} else {
				// Normal move
				targetPiece = theBoard.getPieceAtSquare(targetPosition);
			}
		}
		if (move.promotion != null) {
			promotion = Piece.convertChessmanToPiece(IntChessman.valueOf(move.promotion), false);
			promotion &= Piece.PIECE_NO_COLOUR_MASK;
			intMove = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, targetPiece, promotion);
		} else {
			intMove = Move.valueOfEnPassant(misc, type, originPosition, originPiece, targetPosition, targetPiece, promotion);
		}
		return intMove;
	}
	
	public static int toMove(GenericMove move, Board theBoard) {
		return Move.toMove(move, theBoard, Move.TYPE_REGULAR_NONE);
	}
	
	public static int toMove(GenericMove move) {
		return Move.toMove(move, null, Move.TYPE_REGULAR_NONE);
	}
	
	public static boolean isPromotion(int move) {
		return (move & (Move.TYPE_PROMOTION_MASK << TYPE_SHIFT)) != 0;
	}
	
	public static boolean isCapture(int move) {
		return (move & ((Move.TYPE_CAPTURE_MASK) << TYPE_SHIFT)) != 0;
	}
	
	public static boolean isBest(int move) {
		return (move & ((Move.TYPE_BEST_MASK) << TYPE_SHIFT)) != 0;
	}
	
	public static boolean isRegular(int move) { 
		return (getType(move) == 0);
	}
	
	public static boolean isPawnMove(int move) {
		return Piece.isPawn(getOriginPiece(move));
	}

	public static GenericMove toGenericMove(int move) {
		if (move == Move.NULL_MOVE)
			return null;
		
		int originPosition = getOriginPosition(move);
		int targetPosition = getTargetPosition(move);

		if (isPromotion(move)) {
			return new GenericMove(
					Position.toGenericPosition(originPosition),
					Position.toGenericPosition(targetPosition),
					IntChessman.toGenericChessman(Piece.convertPieceToChessman(getPromotion(move))));
		} else {
			return new GenericMove(
					Position.toGenericPosition(originPosition),
					Position.toGenericPosition(targetPosition));
		}
	}
	
	public static boolean areEqual(int move1, int move2) {
		return (move1 & EQUALITY_MASK) == (move2 & EQUALITY_MASK);
	}
	
	public static boolean areEqualForBestKiller(int move1, int move2) {
		return (move1 & BEST_KILLER_EQUALITY_MASK) == (move2 & BEST_KILLER_EQUALITY_MASK);
	}
	
	public static int getType(int move) {
		int type = (move & TYPE_MASK) >>> TYPE_SHIFT;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (type & ~(Move.TYPE_MASK >>> TYPE_SHIFT)) == 0;

		return type;
	}

	public static int getOriginPosition(int move) {
		int originPosition = (move & ORIGINPOSITION_MASK) >>> ORIGINPOSITION_SHIFT;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (originPosition & 0x88) == 0;

		return originPosition;
	}
	
	private static int setOriginPosition(int move, int originPosition) {
		// Zero out origin position
		move &= ~ORIGINPOSITION_MASK;

		// Encode origin position
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (originPosition & 0x88) == 0;
		move |= originPosition << ORIGINPOSITION_SHIFT;

		return move;
	}

	public static int getTargetPosition(int move) {
		int targetPosition = (move & TARGETPOSITION_MASK) >>> TARGETPOSITION_SHIFT;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (targetPosition & 0x88) == 0;

		return targetPosition;
	}

	public static int setTargetPosition(int move, int targetPosition) {
		// Zero out target position
		move &= ~TARGETPOSITION_MASK;

		// Encode target position
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;

		return move;
	}

	public static int getPromotion(int move) {
		int promotion = (move & PROMOTION_MASK) >>> PROMOTION_SHIFT;
		if (EubosEngineMain.ENABLE_ASSERTS) {
			//assert (/* Valid promotion*/) || promotion == Piece.NONE;
		}
		return promotion;
	}
	
	public static int comparePromotions(int move1, int move2 ) {
		// Order better promotions first, uses natural ordering of Piece
		int promo1 = move1 & Move.PROMOTION_MASK;
		int promo2 = move2 & Move.PROMOTION_MASK;
		
    	if (promo1 > promo2) {
    		return 1;
    	} else if (promo1 == promo2) {
    		return 0;
    	} else {
    		return -1;
    	}
	}

	public static int setPromotion(int move, int promotion) {
		// Zero out promotion chessman
		move &= ~PROMOTION_MASK;

		// Encode promotion
		if (EubosEngineMain.ENABLE_ASSERTS) {
			//assert (/* Valid promotion*/) || promotion == Piece.NONE;
		}
		move |= promotion << PROMOTION_SHIFT;

		return move;
	}
    
	public static int compareCaptures(int move1, int move2) {
    	// mvv lva used for tie breaking move type comparison, if it is a capture
    	int victim1 = Piece.PIECE_TO_MATERIAL_LUT[Move.getTargetPieceNoColour(move1)];
    	int attacker1 = Piece.PIECE_TO_MATERIAL_LUT[Move.getOriginPieceNoColour(move1)];
    	int victim2 = Piece.PIECE_TO_MATERIAL_LUT[Move.getTargetPieceNoColour(move2)];
    	int attacker2 = Piece.PIECE_TO_MATERIAL_LUT[Move.getOriginPieceNoColour(move2)];
    	int mvvLvaRankingForMove1 = victim1-attacker1;
    	int mvvLvaRankingForMove2 = victim2-attacker2;
    	
    	if (mvvLvaRankingForMove1 < mvvLvaRankingForMove2) {
    		return 1;
    	} else if (mvvLvaRankingForMove1 == mvvLvaRankingForMove2) {
    		return 0;
    	} else {
    		return -1;
    	}
	}
	
    public static final MoveMvvLvaComparator mvvLvaComparator = new MoveMvvLvaComparator();
    
    private static class MoveMvvLvaComparator implements IntComparator {
        @Override public int compare(int move1, int move2) {
        	int type1 = move1 & Move.TYPE_MASK;
        	int type2 = move2 & Move.TYPE_MASK;
            if (type1 < type2) {
            	return 1;
            } else if (type1 == type2) {
            	boolean isPromotion = (type1 & (Move.TYPE_PROMOTION_MASK << Move.TYPE_SHIFT)) != 0;
            	if (isPromotion) {
            		// Note, promotion captures are always winning by definition, no need to check that
            		return Move.comparePromotions(move1, move2);
             	} else {
               		boolean isCapture = ((move1 & (Move.TYPE_CAPTURE_MASK << Move.TYPE_SHIFT)) != 0);
            		// MVV LVA comparison only valid for captures, otherwise return equal
                	return (isCapture) ? Move.compareCaptures(move1, move2) : 0;
            	}
            } else {
            	return -1;
            }
        }
    }
	
	@SuppressWarnings("unused")
	public static int getOriginPiece(int move) {
		int piece = (move & ORIGIN_PIECE_MASK) >>> ORIGIN_PIECE_SHIFT;
		if (EubosEngineMain.ENABLE_ASSERTS && !EubosEngineMain.ENABLE_STAGED_MOVE_GENERATION)
			assert piece != Piece.NONE;
		return piece;
	}
	
	public static int getOriginPieceNoColour(int move) {
		int piece = (move & ORIGIN_PIECE_MASK_NO_COLOUR) >>> ORIGIN_PIECE_SHIFT;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert piece != Piece.NONE;
		return piece;
	}

	public static int setOriginPiece(int move, int piece) {
		move &= ~ORIGIN_PIECE_MASK;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert piece != Piece.NONE;
		move |= piece << ORIGIN_PIECE_SHIFT;
		return move;
	}
	
	public static int getTargetPiece(int move) {
		int piece = (move & TARGET_PIECE_MASK) >>> TARGET_PIECE_SHIFT;		
		return piece;
	}
	
	public static int getTargetPieceNoColour(int move) {
		int piece = (move & TARGET_PIECE_NO_COLOUR_MASK) >>> TARGET_PIECE_SHIFT;		
		return piece;
	}

	public static int setTargetPiece(int move, int piece) {
		move &= ~TARGET_PIECE_MASK;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert piece != Piece.NONE;
		move |= piece << TARGET_PIECE_SHIFT;
		return move;
	}
	
	public static int setCapture(int move, int piece) {
		move &= ~TARGET_PIECE_MASK;
		move |= ((Move.TYPE_CAPTURE_MASK << Move.TYPE_SHIFT) | (piece << TARGET_PIECE_SHIFT)) ;
		return move;
	}
	
	public static String toString(int move) {
		StringBuilder string = new StringBuilder();
		if (move != Move.NULL_MOVE) {
			string.append(toGenericMove(move).toString());
			
			if (isPromotion(move)) {
				string.append(":");
				string.append(IntChessman.toGenericChessman(Piece.convertPieceToChessman(getPromotion(move))));
			}		
			if (getOriginPiece(move) != Piece.NONE) {
				string.append(":");
				string.append(Piece.toFenChar(getOriginPiece(move)));
			}
			if (getTargetPiece(move) != Piece.NONE) {
				string.append(":");
				string.append(Piece.toFenChar(getTargetPiece(move)));
			}
			if (Move.isEnPassantCapture(move)) {
				string.append(":ep");
			}
			if ((Move.getType(move) & Move.TYPE_BEST_MASK) != 0) {
				string.append(":best");
			}
			if ((Move.getType(move) & Move.TYPE_KILLER_MASK) != 0) {
				string.append(":killer");
			}
		}
		return string.toString();
	}

	public static int setType(int move, int type) {
		move &= ~TYPE_MASK;
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert ((type<<TYPE_SHIFT) & ~Move.TYPE_MASK) == 0;
		return move |= type << TYPE_SHIFT;
	}
	
	public static int reverse(int move) {
		int reversedMove = move;
		reversedMove = Move.setTargetPosition(reversedMove, Move.getOriginPosition(move));
		reversedMove = Move.setOriginPosition(reversedMove, Move.getTargetPosition(move));
		return reversedMove;
	}

	public static boolean isQueenPromotion(int move) {
		return (((move & (Move.TYPE_PROMOTION_MASK) << TYPE_SHIFT) != 0) && Piece.isQueen(Move.getPromotion(move)));
	}
	
	public static boolean isPawnCapture(int move) {
		return Piece.isPawn(getOriginPiece(move)) && Move.isCapture(move);
	}

	public static boolean invalidatesPawnCache(int move) {
		return (Move.isPromotion(move) || Move.isPawnCapture(move) || Piece.isPawn(Move.getTargetPiece(move)));
	}
	
	public static int setKiller(int move) {
		return (move |= (Move.TYPE_KILLER_MASK << TYPE_SHIFT));
	}
	
	public static int setBest(int move) {
		return (move |= (Move.TYPE_BEST_MASK << TYPE_SHIFT));
	}
	
	public static int clearBest(int move) {
		return (move &= ~(Move.TYPE_BEST_MASK << TYPE_SHIFT));
	}

	public static boolean isEnPassantCapture(int move) {
		return (move & Move.MISC_EN_PASSANT_CAPTURE_MASK) != 0;
	}

	public static boolean isNotCaptureOrPromotion(int move) {
		return (move & (Move.KILLER_EXCLUSION_MASK << Move.TYPE_SHIFT)) == 0;
	}

	public static boolean isWinningorNeutralCapture(int move) {
		boolean isWinningOrNeutralCapture = false;
		if (Move.isCapture(move)) {
	    	// mvv lva used for tie breaking move type comparison, if it is a capture
	    	int victim1 = Piece.PIECE_TO_MATERIAL_LUT[Move.getTargetPieceNoColour(move)];
	    	int attacker1 = Piece.PIECE_TO_MATERIAL_LUT[Move.getOriginPieceNoColour(move)];
	    	int mvvLvaRankingForMove1 = victim1-attacker1;
	    	if (mvvLvaRankingForMove1 >= 0) {
	    		isWinningOrNeutralCapture = true;
	    	}
		}
		return isWinningOrNeutralCapture;
	}
}
