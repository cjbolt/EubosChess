package eubos.board;

import java.util.Arrays;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntRank;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.Position;

public abstract class Piece {
	public enum Colour { 
		white, black;
		
		public static Colour getOpposite( Colour arg ) { return (arg == white) ? black : white; }
		public static boolean isWhite( Colour arg ) { return arg == white; }
		public static boolean isBlack( Colour arg ) { return arg == black; }
	};
	
	public static final int NONE = 0x0;
	public static final int KING = 0x1;
	public static final int QUEEN = 0x2;
	public static final int ROOK = 0x3;
	public static final int BISHOP = 0x4;
	public static final int KNIGHT = 0x5;
	public static final int PAWN = 0x6;
	public static final int DONT_CARE = 0x7;
	
	public static final int BLACK = 0x8;
	public static final int COLOUR_BIT_SHIFT = 3;
	
	public static final int PIECE_NO_COLOUR_MASK = 0x7;
	public static final int PIECE_WHOLE_MASK = 0xf;

	public static final int WHITE_QUEEN = QUEEN;
	public static final int WHITE_BISHOP = BISHOP;
	public static final int WHITE_KING = KING;
	public static final int WHITE_KNIGHT = KNIGHT;
	public static final int WHITE_ROOK = ROOK;
	public static final int WHITE_PAWN = PAWN;
	
	public static final int BLACK_QUEEN = (BLACK|QUEEN);
	public static final int BLACK_BISHOP = (BLACK|BISHOP);
	public static final int BLACK_KING = (BLACK|KING);
	public static final int BLACK_KNIGHT = (BLACK|KNIGHT);
	public static final int BLACK_ROOK = (BLACK|ROOK);
	public static final int BLACK_PAWN = (BLACK|PAWN);
	
	public static boolean isPawn(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PAWN; }
	public static boolean isKing(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == KING; }
	public static boolean isQueen(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == QUEEN; }
	public static boolean isRook(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == ROOK; }
	public static boolean isBishop(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == BISHOP; }
	public static boolean isKnight(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == KNIGHT; }
	
	public static boolean isOppositeColour(Colour ownColour, int toCheck) {
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert (toCheck & PIECE_NO_COLOUR_MASK) != NONE;
		return Colour.isWhite(ownColour) ? isBlack(toCheck) : isWhite(toCheck);
	}
	public static boolean isOppositeColourOrNone(Colour ownColour, int toCheck) {
		boolean retVal = true;
		if (toCheck != Piece.NONE) {
			retVal = Colour.isWhite(ownColour) ? isBlack(toCheck) : isWhite(toCheck);
		}
		return retVal;
	}
	public static boolean isWhite(int arg) {
		return (arg&BLACK) == 0;
	}
	public static boolean isBlack(int arg) {
		return (arg&BLACK) == BLACK;
	}
	public static Colour getOpposite(int arg) {
		return isWhite(arg) ? Colour.black : Colour.white;
	} 
	
	public static int convertChessmanToPiece(int chessman, boolean isWhite) {
		int eubosPiece = Piece.NONE;
		if (chessman==IntChessman.KNIGHT)
			eubosPiece = isWhite ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
		else if (chessman==IntChessman.BISHOP)
			eubosPiece = isWhite ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
		else if (chessman==IntChessman.ROOK)
			eubosPiece = isWhite? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
		else if (chessman==IntChessman.QUEEN)
			eubosPiece = isWhite ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
		return eubosPiece;
	}
	
	public static int convertPieceToChessman(int piece) {
		int chessman = IntChessman.NOCHESSMAN;
		if (Piece.isKnight(piece))
			chessman = IntChessman.KNIGHT;
		else if (Piece.isBishop(piece))
			chessman = IntChessman.BISHOP;
		else if (Piece.isRook(piece))
			chessman = IntChessman.ROOK;
		else if (Piece.isQueen(piece))
			chessman = IntChessman.QUEEN;
		else if (Piece.isKing(piece))
			chessman = IntChessman.KING;
		else if (Piece.isPawn(piece))
			chessman = IntChessman.PAWN;		
		return chessman;
	}
	
	public static char toFenChar(int piece) {
		char chessman = 0;
		if (piece==Piece.WHITE_PAWN)
			chessman = 'P';
		else if (piece==Piece.WHITE_KNIGHT)
			chessman = 'N';
		else if (piece==Piece.WHITE_BISHOP)
			chessman = 'B';
		else if (piece==Piece.WHITE_ROOK)
			chessman = 'R';
		else if (piece==Piece.WHITE_QUEEN)
			chessman = 'Q';
		else if (piece==Piece.WHITE_KING)
			chessman = 'K';
		else if (piece==Piece.BLACK_PAWN)
			chessman = 'p';
		else if (piece==Piece.BLACK_KNIGHT)
			chessman = 'n';
		else if (piece==Piece.BLACK_BISHOP)
			chessman = 'b';
		else if (piece==Piece.BLACK_ROOK)
			chessman = 'r';
		else if (piece==Piece.BLACK_QUEEN)
			chessman = 'q';
		else if (piece==Piece.BLACK_KING)
			chessman = 'k';
		return chessman;
	}
	
	public static String reportStaticDataSizes() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("KnightMove_Lut_Size %d bytes\n", KnightMove_Lut_Size*4));
		s.append(String.format("KingMove_Lut_Size %d bytes\n", KingMove_Lut_Size*4));
		s.append(String.format("QueenMove_Lut_Size %d bytes\n", QueenMove_Lut_Size*4));
		s.append(String.format("RookMove_Lut_Size %d bytes\n", RookMove_Lut_Size*4));
		s.append(String.format("BishopMove_Lut_Size %d bytes\n", BishopMove_Lut_Size*4));
		return s.toString();
	}
	
	public static int getStaticDataSize() {
		return (KnightMove_Lut_Size + KingMove_Lut_Size + QueenMove_Lut_Size + RookMove_Lut_Size + BishopMove_Lut_Size) * 4;
	}
	
	static int KnightMove_Lut_Size = 0;
	static final int[][] WhiteKnightMove_Lut = new int[128][];
	static {
		for (int square : Position.values) {
			WhiteKnightMove_Lut[square] = createKnightMovesFromOriginPosition(square, true);
		}
	}
	static final int[][] BlackKnightMove_Lut = new int[128][];
	static {
		for (int square : Position.values) {
			BlackKnightMove_Lut[square] = createKnightMovesFromOriginPosition(square, false);
		}
	}
	static int [] createKnightMovesFromOriginPosition(int originPosition, boolean isWhite) {
		int originPiece = isWhite ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
		int count = 0;
		int [] moves = new int[8];
		// Create reference moves (with target none)
		for (Direction dir: Direction.values()) {
			int targetPosition = Direction.getIndirectMoveSq(dir, originPosition);
			if (targetPosition != Position.NOPOSITION) {
				moves[count] = Move.valueOf(originPosition, originPiece, targetPosition, Piece.NONE);
				count++;
			}
		}
		// Copy to correctly sized array
		int [] ref_moves = Arrays.copyOf(moves, count);
		KnightMove_Lut_Size += ref_moves.length;
		return ref_moves;
	}
	
	static int KingMove_Lut_Size = 0;
	static final int[][] WhiteKingMove_Lut = new int[128][];
	static {
		for (int square : Position.values) {
			WhiteKingMove_Lut[square] = createKingMovesFromOriginPosition(square, true);
		}
	}
	static final int[][] BlackKingMove_Lut = new int[128][];
	static {
		for (int square : Position.values) {
			BlackKingMove_Lut[square] = createKingMovesFromOriginPosition(square, false);
		}
	}
	static int [] createKingMovesFromOriginPosition(int originPosition, boolean isWhite) {
		Direction [] allDirect = { Direction.up, Direction.upRight, Direction.right, Direction.downRight, Direction.down, Direction.downLeft, Direction.left, Direction.upLeft };
		int originPiece = isWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
		int count = 0;
		int [] moves = new int[8];
		// Create reference moves (with target none)
		for (Direction dir: allDirect) {
			int targetPosition = Direction.getDirectMoveSq(dir, originPosition);
			if (targetPosition != Position.NOPOSITION) {
				moves[count] = Move.valueOf(originPosition, originPiece, targetPosition, Piece.NONE);
				count++;
			}
		}
		// Copy to correctly sized array
		int [] ref_moves = Arrays.copyOf(moves, count);
		KingMove_Lut_Size += ref_moves.length;
		return ref_moves;
	}
	
	static int RookMove_Lut_Size = 0;
	static final int[][][] WhiteRookMove_Lut = new int[128][][]; // Position by direction by moves in that direction
	static {
		for (int square : Position.values) {
			WhiteRookMove_Lut[square] = createRookMovesFromOriginPosition(square, true);
		}
	}
	static final int[][][] BlackRookMove_Lut = new int[128][][];
	static {
		for (int square : Position.values) {
			BlackRookMove_Lut[square] = createRookMovesFromOriginPosition(square, false);
		}
	}
	static int [][] createRookMovesFromOriginPosition(int originPosition, boolean isWhite) {
		Direction [] rookDirect = SquareAttackEvaluator.rankFile;
		int originPiece = isWhite ? Piece.WHITE_ROOK: Piece.BLACK_ROOK;
		int [][] return_value = new int [4][];
		int direction_index = 0;
		// Create reference moves (with target none)
		for (Direction dir : rookDirect) {
			int count = 0;
			int [] moves = new int[7];
			// Walk down direction adding all squares to the edge of the board
			int targetPosition = Direction.getDirectMoveSq(dir, originPosition);
			while (targetPosition != Position.NOPOSITION) {
				if (targetPosition != Position.NOPOSITION) {
					moves[count] = Move.valueOf(originPosition, originPiece, targetPosition, Piece.NONE);
					count++;
				}
				targetPosition = Direction.getDirectMoveSq(dir, targetPosition);
			}
			// Copy to correctly sized array
			int [] ref_moves = Arrays.copyOf(moves, count);
			return_value[direction_index++] = ref_moves;
			RookMove_Lut_Size += ref_moves.length;
		}
		return return_value;
	}
	
	static int BishopMove_Lut_Size = 0;
	static final int[][][] WhiteBishopMove_Lut = new int[128][][]; // Position by direction by moves in that direction
	static {
		for (int square : Position.values) {
			WhiteBishopMove_Lut[square] = createBishopMovesFromOriginPosition(square, true);
		}
	}
	static final int[][][] BlackBishopMove_Lut = new int[128][][];
	static {
		for (int square : Position.values) {
			BlackBishopMove_Lut[square] = createBishopMovesFromOriginPosition(square, false);
		}
	}
	static int [][] createBishopMovesFromOriginPosition(int originPosition, boolean isWhite) {
		Direction [] bishopDirect = SquareAttackEvaluator.diagonals;
		int originPiece = isWhite ? Piece.WHITE_BISHOP: Piece.BLACK_BISHOP;
		int [][] return_value = new int [4][];
		int direction_index = 0;
		// Create reference moves (with target none)
		for (Direction dir : bishopDirect) {
			int count = 0;
			int [] moves = new int[7];
			// Walk down direction adding all squares to the edge of the board
			int targetPosition = Direction.getDirectMoveSq(dir, originPosition);
			while (targetPosition != Position.NOPOSITION) {
				if (targetPosition != Position.NOPOSITION) {
					moves[count] = Move.valueOf(originPosition, originPiece, targetPosition, Piece.NONE);
					count++;
				}
				targetPosition = Direction.getDirectMoveSq(dir, targetPosition);
			}
			// Copy to correctly sized array
			int [] ref_moves = Arrays.copyOf(moves, count);
			return_value[direction_index++] = ref_moves;
			BishopMove_Lut_Size += ref_moves.length;
		}
		return return_value;
	}
	
	static int QueenMove_Lut_Size = 0;
	static final int[][][] WhiteQueenMove_Lut = new int[128][][]; // Position by direction by moves in that direction
	static {
		for (int square : Position.values) {
			WhiteQueenMove_Lut[square] = createQueenMovesFromOriginPosition(square, true);
		}
	}
	static final int[][][] BlackQueenMove_Lut = new int[128][][];
	static {
		for (int square : Position.values) {
			BlackQueenMove_Lut[square] = createQueenMovesFromOriginPosition(square, false);
		}
	}
	static int [][] createQueenMovesFromOriginPosition(int originPosition, boolean isWhite) {
		Direction [] queenDirect = SquareAttackEvaluator.allDirect;
		int originPiece = isWhite ? Piece.WHITE_QUEEN: Piece.BLACK_QUEEN;
		int [][] return_value = new int [8][];
		int direction_index = 0;
		// Create reference moves (with target none)
		for (Direction dir : queenDirect) {
			int count = 0;
			int [] moves = new int[7];
			// Walk down direction adding all squares to the edge of the board
			int targetPosition = Direction.getDirectMoveSq(dir, originPosition);
			while (targetPosition != Position.NOPOSITION) {
				if (targetPosition != Position.NOPOSITION) {
					moves[count] = Move.valueOf(originPosition, originPiece, targetPosition, Piece.NONE);
					count++;
				}
				targetPosition = Direction.getDirectMoveSq(dir, targetPosition);
			}
			// Copy to correctly sized array
			int [] ref_moves = Arrays.copyOf(moves, count);
			return_value[direction_index++] = ref_moves;
			QueenMove_Lut_Size += ref_moves.length;
		}
		return return_value;
	}
	
	static void king_generateMoves(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int [] ref_moves = ownSideIsWhite ? WhiteKingMove_Lut[atSquare] : BlackKingMove_Lut[atSquare];
		single_addMoves(ownSideIsWhite, ml, theBoard, ref_moves);	
	}
	
	static void knight_generateMoves(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int [] ref_moves = ownSideIsWhite ? WhiteKnightMove_Lut[atSquare] : BlackKnightMove_Lut[atSquare];
		single_addMoves(ownSideIsWhite, ml, theBoard, ref_moves);
	}
	
	static void king_generateMovesExtSearch(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite, int targetSq) {
		int piece = ownSideIsWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
		int targetPiece = theBoard.getPieceAtSquareOptimise(targetSq, ownSideIsWhite);
		if (targetPiece != Piece.NONE && targetPiece != Piece.DONT_CARE) {
			ml.addPrio(Move.valueOf(atSquare, piece, targetSq, targetPiece));
		}
	}
	
	static void knight_generateMovesExtSearch(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite, int targetSq) {
		int piece = ownSideIsWhite ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
		int targetPiece = theBoard.getPieceAtSquareOptimise(targetSq, ownSideIsWhite);
		if (targetPiece != Piece.NONE && targetPiece != Piece.DONT_CARE) {
			ml.addPrio(Move.valueOf(atSquare, piece, targetSq, targetPiece));
		}
	}
	
	static void rook_generateMoves(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int [][] ref_moves = ownSideIsWhite ? WhiteRookMove_Lut[atSquare] : BlackRookMove_Lut[atSquare];
		multidirect_addMoves(ownSideIsWhite, ml, theBoard, ref_moves);
	}
	
	static void queen_generateMoves(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int [][] ref_moves = ownSideIsWhite ? WhiteQueenMove_Lut[atSquare] : BlackQueenMove_Lut[atSquare];
		multidirect_addMoves(ownSideIsWhite, ml, theBoard, ref_moves);	
	}
	
	static void bishop_generateMoves(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int [][] ref_moves = ownSideIsWhite ? WhiteBishopMove_Lut[atSquare] : BlackBishopMove_Lut[atSquare];
		multidirect_addMoves(ownSideIsWhite, ml, theBoard, ref_moves);	
	}
	
	static void rook_generateMovesExtSearch(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite, int targetSq) {
		int [][] ref_moves = ownSideIsWhite ? WhiteRookMove_Lut[atSquare] : BlackRookMove_Lut[atSquare];
		Direction dir = SquareAttackEvaluator.findDirectionToTarget(atSquare, targetSq, SquareAttackEvaluator.rankFile);
		if (dir != null) {
			int [] moves = ref_moves[SquareAttackEvaluator.rankFileDirectionIndex_Lut.get(dir)];
			multidirect_addMoves(ownSideIsWhite, ml, theBoard, moves, atSquare, targetSq);
		}
	}
	
	static void queen_generateMovesExtSearch(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite, int targetSq) {
		int [][] ref_moves = ownSideIsWhite ? WhiteQueenMove_Lut[atSquare] : BlackQueenMove_Lut[atSquare];
		Direction dir = SquareAttackEvaluator.findDirectionToTarget(atSquare, targetSq, SquareAttackEvaluator.allDirect);
		if (dir != null) {
			int [] moves = ref_moves[SquareAttackEvaluator.directionIndex_Lut.get(dir)];
			multidirect_addMoves(ownSideIsWhite, ml, theBoard, moves, atSquare, targetSq);
		}	
	}
	
	static void bishop_generateMovesExtSearch(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite, int targetSq) {
		int [][] ref_moves = ownSideIsWhite ? WhiteBishopMove_Lut[atSquare] : BlackBishopMove_Lut[atSquare];
		Direction dir = SquareAttackEvaluator.findDirectionToTarget(atSquare, targetSq, SquareAttackEvaluator.diagonals);
		if (dir != null) {
			int [] moves = ref_moves[SquareAttackEvaluator.diagonalsDirectionIndex_Lut.get(dir)];
			multidirect_addMoves(ownSideIsWhite, ml, theBoard, moves, atSquare, targetSq);
		}
	}

	private static void multidirect_addMoves(boolean ownSideIsWhite, MoveList ml, Board theBoard, int[][] moves) {
		for (int[] movesInDirection : moves) {
			for (int new_move : movesInDirection) {
				int targetPiece = theBoard.getPieceAtSquareOptimise(Move.getTargetPosition(new_move), ownSideIsWhite);
				switch(targetPiece) {
				case Piece.NONE:
					ml.addNormal(new_move);
					continue;
				case Piece.DONT_CARE:
					break; // i.e. blocked by own piece
				default:
					new_move = Move.setCapture(new_move, targetPiece);
					ml.addPrio(new_move);
					break;
				}
				break;
			}	
		}
	}
	
	private static void multidirect_addMoves(boolean ownSideIsWhite, MoveList ml, Board theBoard, int[] moves, int atSquare, int targetSq) {
		for (int new_move : moves) {
			int targetPiece = theBoard.getPieceAtSquareOptimise(Move.getTargetPosition(new_move), ownSideIsWhite);
			switch(targetPiece) {
			case Piece.NONE:
				continue;
			case Piece.DONT_CARE:
				break; // i.e. blocked by own piece
			default:
				new_move = Move.setCapture(new_move, targetPiece);
				ml.addPrio(new_move);
				break;
			}
			break;
		}	
	}
	
	private static void single_addMoves(boolean ownSideIsWhite, MoveList ml, Board theBoard, int[] moves) {
		for (int new_move : moves) {
			int targetPiece = theBoard.getPieceAtSquareOptimise(Move.getTargetPosition(new_move), ownSideIsWhite);
			switch(targetPiece) {
			case Piece.NONE:
				ml.addNormal(new_move);
				continue;
			case Piece.DONT_CARE:
				break; // i.e. blocked by own piece
			default:
				new_move = Move.setCapture(new_move, targetPiece);
				ml.addPrio(new_move);
				break;
			}
		}
	}
		
	private static boolean pawn_isAtInitialPosition(int atSquare, boolean ownSideIsWhite) {
		if (!ownSideIsWhite) {
			return (Position.getRank(atSquare) == IntRank.R7);
		} else {
			return (Position.getRank(atSquare) == IntRank.R2);
		}
	}

	private static int pawn_genOneSqTarget(int atSquare, boolean ownSideIsWhite) {
		if (!ownSideIsWhite) {
			return Direction.getDirectMoveSq(Direction.down, atSquare);
		} else {
			return Direction.getDirectMoveSq(Direction.up, atSquare);
		}
	}	
	
	private static int pawn_genTwoSqTarget(int atSquare, boolean ownSideIsWhite) {
		int moveTo = Position.NOPOSITION;
		if ( pawn_isAtInitialPosition(atSquare, ownSideIsWhite) ) {
			if (!ownSideIsWhite) {
				moveTo = Direction.getDirectMoveSq(Direction.down, Direction.getDirectMoveSq(Direction.down, atSquare));
			} else {
				moveTo = Direction.getDirectMoveSq(Direction.up, Direction.getDirectMoveSq(Direction.up, atSquare));
			}
		}
		return moveTo;
	}
	
	private static int pawn_genLeftCaptureTarget(int atSquare, boolean ownSideIsWhite) {
		if (!ownSideIsWhite) {
			return Direction.getDirectMoveSq(Direction.downRight, atSquare);
		} else {
			return Direction.getDirectMoveSq(Direction.upLeft, atSquare);
		}
	}
	
	private static int pawn_genRightCaptureTarget(int atSquare, boolean ownSideIsWhite) {
		if (!ownSideIsWhite) {
			return Direction.getDirectMoveSq(Direction.downLeft, atSquare);
		} else {
			return Direction.getDirectMoveSq(Direction.upRight, atSquare);
		}		
	}
	
	private static int pawn_isCapturable(boolean ownSideIsWhite, Board theBoard, int captureAt ) {
		int capturePiece = Piece.NONE;
		int queryPiece = theBoard.getPieceAtSquareOptimise(captureAt, ownSideIsWhite);
		if ( queryPiece != Piece.NONE && queryPiece != Piece.DONT_CARE) {
			capturePiece = queryPiece;
		}
		return capturePiece;
	}
	
	private static boolean pawn_checkPromotionPossible(boolean ownSideIsWhite, int targetSquare ) {
		return (( !ownSideIsWhite && Position.getRank(targetSquare) == IntRank.R1) || 
				( ownSideIsWhite && Position.getRank(targetSquare) == IntRank.R8));
	}
	
	private static void pawn_checkPromotionAddMove(int ownPiece, Board theBoard, int atSquare, boolean ownSideIsWhite, MoveList ml,
			int targetSquare, int targetPiece) {
		if ( pawn_checkPromotionPossible( ownSideIsWhite, targetSquare )) {
			// Add in order of prioritisation
			ml.addPrio(Move.valueOf(Move.TYPE_PROMOTION_MASK, atSquare, ownPiece, targetSquare, targetPiece, Piece.QUEEN ));
			ml.addPrio(Move.valueOf(Move.TYPE_PROMOTION_MASK, atSquare, ownPiece, targetSquare, targetPiece, Piece.ROOK ));
			ml.addPrio(Move.valueOf(Move.TYPE_PROMOTION_MASK, atSquare, ownPiece, targetSquare, targetPiece, Piece.BISHOP ));
			ml.addPrio(Move.valueOf(Move.TYPE_PROMOTION_MASK, atSquare, ownPiece, targetSquare, targetPiece, Piece.KNIGHT ));
		} else {
			if (targetPiece != Piece.NONE) {
				// add captures at head of list
				ml.addPrio(Move.valueOf(atSquare, ownPiece, targetSquare, targetPiece));
			} else {
				ml.addNormal(Move.valueOf(atSquare, ownPiece, targetSquare, targetPiece));
			}
		}
	}
	
	private static void pawn_checkQueenPromotionAddMove(int ownPiece, Board theBoard, int atSquare, boolean ownSideIsWhite, MoveList ml,
			int targetSquare, int targetPiece) {
		if ( pawn_checkPromotionPossible( ownSideIsWhite, targetSquare )) {
			// In extended search only generate the queen promotion move
			ml.addPrio(Move.valueOf(Move.TYPE_PROMOTION_MASK, atSquare, ownPiece, targetSquare, targetPiece, Piece.QUEEN ));
		} else {
			if (targetPiece != Piece.NONE) {
				// add captures at head of list
				ml.addPrio(Move.valueOf(atSquare, ownPiece, targetSquare, targetPiece));
			} else {
				ml.addNormal(Move.valueOf(atSquare, ownPiece, targetSquare, targetPiece));
			}
		}
	}
	
	static void pawn_generateMoves(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int ownPiece = ownSideIsWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
		int capturePiece = Piece.NONE;
		// Check for standard one and two square moves
		int moveTo = pawn_genOneSqTarget(atSquare, ownSideIsWhite);
		if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
			pawn_checkPromotionAddMove(ownPiece, theBoard, atSquare, ownSideIsWhite, ml, moveTo, Piece.NONE);
			moveTo = pawn_genTwoSqTarget(atSquare, ownSideIsWhite);
			if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
				// Can't be a promotion
				ml.addNormal(Move.valueOf(atSquare, ownPiece, moveTo , Piece.NONE));
			}	
		}
		// Check for capture moves, includes en passant
		int captureAt = pawn_genLeftCaptureTarget(atSquare, ownSideIsWhite);
		if ( captureAt != Position.NOPOSITION ) {
			capturePiece = pawn_isCapturable(ownSideIsWhite, theBoard, captureAt);
			if (capturePiece != Piece.NONE) {
				pawn_checkPromotionAddMove(ownPiece, theBoard, atSquare, ownSideIsWhite, ml, captureAt, capturePiece);
			} else if (captureAt == theBoard.getEnPassantTargetSq()) {
				capturePiece = !ownSideIsWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
				// promotion can't be possible if en passant capture
				ml.addPrio(Move.valueOf(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, atSquare, ownPiece, captureAt, capturePiece, Piece.NONE));
			}
		}
		captureAt = pawn_genRightCaptureTarget(atSquare, ownSideIsWhite);
		if ( captureAt != Position.NOPOSITION ) {
			capturePiece = pawn_isCapturable(ownSideIsWhite, theBoard, captureAt);
			if (capturePiece != Piece.NONE) {
				pawn_checkPromotionAddMove(ownPiece, theBoard, atSquare, ownSideIsWhite, ml, captureAt, capturePiece);
			} else if (captureAt == theBoard.getEnPassantTargetSq()) {
				capturePiece = !ownSideIsWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
				// promotion can't be possible if en passant capture
				ml.addPrio(Move.valueOf(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, atSquare, ownPiece, captureAt, capturePiece, Piece.NONE));
			}
		}
	}
	
	static void pawn_generateMovesForExtendedSearch(MoveList ml, Board theBoard, int atSquare, boolean ownSideIsWhite) {
		int ownPiece = ownSideIsWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
		int capturePiece = Piece.NONE;
		// Standard move
		int moveTo = pawn_genOneSqTarget(atSquare, ownSideIsWhite);
		if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
			pawn_checkQueenPromotionAddMove(ownPiece, theBoard, atSquare, ownSideIsWhite, ml, moveTo, Piece.NONE);
		}
		// Capture moves
		int captureAt = pawn_genLeftCaptureTarget(atSquare, ownSideIsWhite);
		if ( captureAt != Position.NOPOSITION ) {
			capturePiece = pawn_isCapturable(ownSideIsWhite, theBoard, captureAt);
			if (capturePiece != Piece.NONE) {
				pawn_checkQueenPromotionAddMove(ownPiece, theBoard, atSquare, ownSideIsWhite, ml, captureAt, capturePiece);
			}
		}
		captureAt = pawn_genRightCaptureTarget(atSquare, ownSideIsWhite);
		if ( captureAt != Position.NOPOSITION ) {
			capturePiece = pawn_isCapturable(ownSideIsWhite, theBoard, captureAt);
			if (capturePiece != Piece.NONE) {
				pawn_checkQueenPromotionAddMove(ownPiece, theBoard, atSquare, ownSideIsWhite, ml, captureAt, capturePiece);
			}
		}
	}
}
