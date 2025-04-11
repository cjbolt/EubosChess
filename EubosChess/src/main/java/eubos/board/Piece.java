package eubos.board;

import java.util.Arrays;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntRank;

import eubos.position.IAddMoves;
import eubos.position.Move;
import eubos.position.Position;

public abstract class Piece {
    public enum Colour { 
        white, black;
    };
    
    // Note: Piece values below are not completely arbitrary, they must match Zobrist indexes
    // e.g. int pieceType = (currPiece & Piece.PIECE_NO_COLOUR_MASK) - 1; // convert piece type to Zobrist index
    public static final int NONE = 0x0;
	public static final int KING = 0x1;
    public static final int PAWN = 0x2;
    public static final int ROOK = 0x3;
	public static final int KNIGHT = 0x4;
	public static final int QUEEN = 0x5;
	public static final int BISHOP = 0x6;	
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
    public static final int PIECE_LENGTH = (BLACK|DONT_CARE);
    
    public static final int [] Indexes = {
		WHITE_QUEEN, WHITE_BISHOP ,WHITE_KING, WHITE_KNIGHT, WHITE_ROOK, WHITE_PAWN, 
		BLACK_QUEEN, BLACK_BISHOP, BLACK_KING, BLACK_KNIGHT, BLACK_ROOK, BLACK_PAWN
    };
    
    public static boolean isPawn(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PAWN; }
    public static boolean isKing(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == KING; }
    public static boolean isQueen(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == QUEEN; }
    public static boolean isRook(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == ROOK; }
    public static boolean isBishop(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == BISHOP; }
    public static boolean isKnight(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == KNIGHT; }
    
    public static boolean isWhite(int arg) {
        return arg<BLACK;
    }
    public static boolean isBlack(int arg) {
        return arg>=BLACK;
    }
    
    public static final short MATERIAL_VALUE_KING = 4000;
    public static final short MATERIAL_VALUE_QUEEN = 1800;
    public static final short MATERIAL_VALUE_ROOK = 900;
    public static final short MATERIAL_VALUE_BISHOP = 600;
    public static final short MATERIAL_VALUE_KNIGHT = 580;
    public static final short MATERIAL_VALUE_PAWN = 100;
    
    public static final short EG_MATERIAL_VALUE_KING = 4000;
    public static final short EG_MATERIAL_VALUE_QUEEN = 1560;
    public static final short EG_MATERIAL_VALUE_ROOK = 845;
    public static final short EG_MATERIAL_VALUE_BISHOP = 550;
    public static final short EG_MATERIAL_VALUE_KNIGHT = 520;
    public static final short EG_MATERIAL_VALUE_PAWN = 130;
    
    public static final short [][] PIECE_TO_MATERIAL_LUT;
    static {
        PIECE_TO_MATERIAL_LUT = new short [2][];
        
        PIECE_TO_MATERIAL_LUT[0] = new short [PIECE_LENGTH];
        PIECE_TO_MATERIAL_LUT[0][WHITE_QUEEN] = MATERIAL_VALUE_QUEEN;
        PIECE_TO_MATERIAL_LUT[0][WHITE_ROOK] = MATERIAL_VALUE_ROOK;
        PIECE_TO_MATERIAL_LUT[0][WHITE_BISHOP] = MATERIAL_VALUE_BISHOP;
        PIECE_TO_MATERIAL_LUT[0][WHITE_KNIGHT] = MATERIAL_VALUE_KNIGHT;
        PIECE_TO_MATERIAL_LUT[0][WHITE_KING] = MATERIAL_VALUE_KING;
        PIECE_TO_MATERIAL_LUT[0][WHITE_PAWN] = MATERIAL_VALUE_PAWN;
        
        PIECE_TO_MATERIAL_LUT[0][BLACK_QUEEN] = -MATERIAL_VALUE_QUEEN;
        PIECE_TO_MATERIAL_LUT[0][BLACK_ROOK] = -MATERIAL_VALUE_ROOK;
        PIECE_TO_MATERIAL_LUT[0][BLACK_BISHOP] = -MATERIAL_VALUE_BISHOP;
        PIECE_TO_MATERIAL_LUT[0][BLACK_KNIGHT] = -MATERIAL_VALUE_KNIGHT;
        PIECE_TO_MATERIAL_LUT[0][BLACK_KING] = -MATERIAL_VALUE_KING;
        PIECE_TO_MATERIAL_LUT[0][BLACK_PAWN] = -MATERIAL_VALUE_PAWN;
        
        PIECE_TO_MATERIAL_LUT[1] = new short [PIECE_LENGTH];
        PIECE_TO_MATERIAL_LUT[1][WHITE_QUEEN] = EG_MATERIAL_VALUE_QUEEN;
        PIECE_TO_MATERIAL_LUT[1][WHITE_ROOK] = EG_MATERIAL_VALUE_ROOK;
        PIECE_TO_MATERIAL_LUT[1][WHITE_BISHOP] = EG_MATERIAL_VALUE_BISHOP;
        PIECE_TO_MATERIAL_LUT[1][WHITE_KNIGHT] = EG_MATERIAL_VALUE_KNIGHT;
        PIECE_TO_MATERIAL_LUT[1][WHITE_KING] = EG_MATERIAL_VALUE_KING;
        PIECE_TO_MATERIAL_LUT[1][WHITE_PAWN] = EG_MATERIAL_VALUE_PAWN;
        
        PIECE_TO_MATERIAL_LUT[1][BLACK_QUEEN] = -EG_MATERIAL_VALUE_QUEEN;
        PIECE_TO_MATERIAL_LUT[1][BLACK_ROOK] = -EG_MATERIAL_VALUE_ROOK;
        PIECE_TO_MATERIAL_LUT[1][BLACK_BISHOP] = -EG_MATERIAL_VALUE_BISHOP;
        PIECE_TO_MATERIAL_LUT[1][BLACK_KNIGHT] = -EG_MATERIAL_VALUE_KNIGHT;
        PIECE_TO_MATERIAL_LUT[1][BLACK_KING] = -EG_MATERIAL_VALUE_KING;
        PIECE_TO_MATERIAL_LUT[1][BLACK_PAWN] = -EG_MATERIAL_VALUE_PAWN;
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
    
    static int KnightMove_Lut_Size = 0;
    static final int[][] WhiteKnightMove_Lut = new int[64][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            WhiteKnightMove_Lut[bitOffset++] = createKnightMovesFromOriginPosition(square, true);
        }
    }
    static final int[][] BlackKnightMove_Lut = new int[64][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            BlackKnightMove_Lut[bitOffset++] = createKnightMovesFromOriginPosition(square, false);
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
    static final int[][] WhiteKingMove_Lut = new int[64][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            WhiteKingMove_Lut[bitOffset++] = createKingMovesFromOriginPosition(square, true);
        }
    }
    static final int[][] BlackKingMove_Lut = new int[64][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            BlackKingMove_Lut[bitOffset++] = createKingMovesFromOriginPosition(square, false);
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
    
    static final int[][] BlackPawnPromotionMove_Lut = new int[64][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            if (Position.getRank(square) == IntRank.R2) {
                BlackPawnPromotionMove_Lut[bitOffset] = createBlackPawnPromotionMovesFromOriginPosition(square);
            }
            bitOffset++;
        }
    }
    static int [] createBlackPawnPromotionMovesFromOriginPosition(int originPosition) {
        int originPiece = Piece.BLACK_PAWN;
        int [] moves = new int[4];
        // Create reference moves (with target none)
        int targetPosition = Direction.getDirectMoveSq(Direction.down, originPosition);
        moves[0] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.QUEEN );
        moves[1] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.ROOK );
        moves[2] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.BISHOP );
        moves[3] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.KNIGHT );
        return moves;
    }
    
    static final int[][] WhitePawnPromotionMove_Lut = new int[64][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            if (Position.getRank(square) == IntRank.R7) {
                WhitePawnPromotionMove_Lut[bitOffset] = createWhitePawnPromotionMovesFromOriginPosition(square);
            }
            bitOffset++;
        }
    }
    static int [] createWhitePawnPromotionMovesFromOriginPosition(int originPosition) {
        int originPiece = Piece.WHITE_PAWN;
        int [] moves = new int[4];
        // Create reference moves (with target none)
        int targetPosition = Direction.getDirectMoveSq(Direction.up, originPosition);
        moves[0] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.QUEEN );
        moves[1] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.ROOK );
        moves[2] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.BISHOP );
        moves[3] = Move.valueOf(Move.TYPE_PROMOTION_MASK, originPosition, originPiece, targetPosition, Piece.NONE, Piece.KNIGHT );
        return moves;
    }
    
    static int RookMove_Lut_Size = 0;
    static final int[][][] WhiteRookMove_Lut = new int[64][][]; // Position by direction by moves in that direction
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            WhiteRookMove_Lut[bitOffset++] = createRookMovesFromOriginPosition(square, true);
        }
    }
    static final int[][][] BlackRookMove_Lut = new int[64][][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            BlackRookMove_Lut[bitOffset++] = createRookMovesFromOriginPosition(square, false);
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
    static final int[][][] WhiteBishopMove_Lut = new int[64][][]; // Position by direction by moves in that direction
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            WhiteBishopMove_Lut[bitOffset++] = createBishopMovesFromOriginPosition(square, true);
        }
    }
    static final int[][][] BlackBishopMove_Lut = new int[64][][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            BlackBishopMove_Lut[bitOffset++] = createBishopMovesFromOriginPosition(square, false);
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
    static final int[][][] WhiteQueenMove_Lut = new int[64][][]; // Position by direction by moves in that direction
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            WhiteQueenMove_Lut[bitOffset++] = createQueenMovesFromOriginPosition(square, true);
        }
    }
    static final int[][][] BlackQueenMove_Lut = new int[64][][];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            BlackQueenMove_Lut[bitOffset++] = createQueenMovesFromOriginPosition(square, false);
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
    
    static void king_generateMoves_White(IAddMoves ml, Board theBoard, int atSquare) {
        single_addMoves_White(ml, theBoard, WhiteKingMove_Lut[atSquare]);   
    }
    
    static void king_generateMoves_Black(IAddMoves ml, Board theBoard, int atSquare) {
        single_addMoves_Black(ml, theBoard, BlackKingMove_Lut[atSquare]);   
    }
    
    static void knight_generateMoves_White(IAddMoves ml, Board theBoard, int atSquare) {
        single_addMoves_White(ml, theBoard, WhiteKnightMove_Lut[atSquare]);
    }
    
    static void knight_generateMoves_Black(IAddMoves ml, Board theBoard, int atSquare) {
        single_addMoves_Black(ml, theBoard, BlackKnightMove_Lut[atSquare]);
    }
        
    static void king_generateMovesExtSearch_White(IAddMoves ml, Board theBoard, int atSquare) {
        single_addCaptures_White(ml, theBoard, WhiteKingMove_Lut[atSquare]);
    }
    
    static void king_generateMovesExtSearch_Black(IAddMoves ml, Board theBoard, int bitOffset) {
        single_addCaptures_Black(ml, theBoard, BlackKingMove_Lut[bitOffset]);
    }
        
    static void knight_generateMovesExtSearch_White(IAddMoves ml, Board theBoard, int bitOffset) {
        single_addCaptures_White(ml, theBoard, WhiteKnightMove_Lut[bitOffset]);
    }
    
    static void knight_generateMovesExtSearch_Black(IAddMoves ml, Board theBoard, int atSquare) {
        single_addCaptures_Black(ml, theBoard, BlackKnightMove_Lut[atSquare]);
    }
    
    static void rook_generateMoves_White(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addMoves_White(ml, theBoard, WhiteRookMove_Lut[atSquare]);
    }
    
    static void rook_generateMoves_Black(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addMoves_Black(ml, theBoard, BlackRookMove_Lut[atSquare]);
    }
    
    static void queen_generateMoves_White(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addMoves_White(ml, theBoard, WhiteQueenMove_Lut[atSquare]); 
    }
    
    static void queen_generateMoves_Black(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addMoves_Black(ml, theBoard, BlackQueenMove_Lut[atSquare]); 
    }
    
    static void bishop_generateMoves_White(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addMoves_White(ml, theBoard, WhiteBishopMove_Lut[atSquare]);    
    }
    
    static void bishop_generateMoves_Black(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addMoves_Black(ml, theBoard, BlackBishopMove_Lut[atSquare]);    
    }
        
    static void rook_generateMovesExtSearch_Black(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addCapturesForBlack(ml, theBoard, BlackRookMove_Lut[atSquare]);
    }
    
    static void queen_generateMovesExtSearch_Black(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addCapturesForBlack(ml, theBoard, BlackQueenMove_Lut[atSquare]);    
    }
    
    static void bishop_generateMovesExtSearch_Black(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addCapturesForBlack(ml, theBoard, BlackBishopMove_Lut[atSquare]);   
    }
    
    static void rook_generateMovesExtSearch_White(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addCapturesForWhite(ml, theBoard, WhiteRookMove_Lut[atSquare]);
    }
    
    static void queen_generateMovesExtSearch_White(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addCapturesForWhite(ml, theBoard, WhiteQueenMove_Lut[atSquare]);    
    }
    
    static void bishop_generateMovesExtSearch_White(IAddMoves ml, Board theBoard, int atSquare) {
        multidirect_addCapturesForWhite(ml, theBoard, WhiteBishopMove_Lut[atSquare]);   
    }

    private static void multidirect_addMoves_White(IAddMoves ml, Board theBoard, int[][] moves) {
        for (int[] movesInDirection : moves) {
            for (int new_move : movesInDirection) {
                int targetPiece = theBoard.getPieceAtSquareEnemyBlack(1L << Move.getTargetPosition(new_move));
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
    
    private static void multidirect_addMoves_Black(IAddMoves ml, Board theBoard, int[][] moves) {
        for (int[] movesInDirection : moves) {
            for (int new_move : movesInDirection) {
                int targetPiece = theBoard.getPieceAtSquareEnemyWhite(1L << Move.getTargetPosition(new_move));
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
    
    private static void multidirect_addCapturesForBlack(IAddMoves ml, Board theBoard, int[][] moves) {
        for (int[] movesInDirection : moves) {
            for (int new_move : movesInDirection) {
                int targetPiece = theBoard.getPieceAtSquareEnemyWhite(1L << Move.getTargetPosition(new_move));
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
    }
    
    private static void multidirect_addCapturesForWhite(IAddMoves ml, Board theBoard, int[][] moves) {
        for (int[] movesInDirection : moves) {
            for (int new_move : movesInDirection) {
                int targetPiece = theBoard.getPieceAtSquareEnemyBlack(1L << Move.getTargetPosition(new_move));
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
    } 
    
    static int rook_get_direction(int atSquare, int target) {
    	int at_rank = BitBoard.getRank(atSquare);
    	int to_rank = BitBoard.getRank(target);
    	int direction = 0;
    	if (at_rank == to_rank) {
        	int at_file = BitBoard.getFile(atSquare);
        	int to_file = BitBoard.getFile(target);
    		if (at_file < to_file) {
    			direction = 3; // Direction.left
    		} else {
    			direction = 2; // Direction.right
    		}
    	} else if (at_rank < to_rank) {
    		direction = 1; // Direction.up
    	} else {
    		direction = 0; // Direction.down
    	}
    	return direction;
    }
    
    static void rook_checkMove_White(IAddMoves ml, Board theBoard, int atSquare, int target) {
    	int direction = rook_get_direction(atSquare, target);
        multidirect_checkForMove_White(ml, theBoard, WhiteRookMove_Lut[atSquare][direction]); 
    }
    
    static void rook_checkMove_Black(IAddMoves ml, Board theBoard, int atSquare, int target) {
    	int direction = rook_get_direction(atSquare, target);
        multidirect_checkForMove_Black(ml, theBoard, BlackRookMove_Lut[atSquare][direction]);
    }

    static int queen_get_direction(int atSquare, int target) {
    	int at_file = BitBoard.getFile(atSquare);
    	int to_file = BitBoard.getFile(target);
    	int at_rank = BitBoard.getRank(atSquare);
    	int to_rank = BitBoard.getRank(target);
    	int direction = 0;
    	if (at_rank == to_rank) {
    		// rook
    		if (at_file < to_file) {
    			direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.right);
    		} else {
    			direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.left);
    		}
    	} else if (at_file == to_file) {
    		// rook
    		if (at_rank < to_rank) {
	    		direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.up);
	    	} else {
	    		direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.down);
	    	}
    	} else {
    		//bishop
        	if (at_rank < to_rank) {
        		if (at_file < to_file) {
        			direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.upRight);
        		} else {
        			direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.upLeft);
        		}
        	} else {
        		if (at_file < to_file) {
    	    		direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.downRight);
    	    	} else {
    	    		direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.downLeft);
    	    	}
        	}
    	}
    	return direction;
    }
    
    static void queen_checkMove_White(IAddMoves ml, Board theBoard, int atSquare, int target) {
    	int direction = queen_get_direction(atSquare, target);
        multidirect_checkForMove_White(ml, theBoard, WhiteQueenMove_Lut[atSquare][direction]);
    }
    
    static void queen_checkMove_Black(IAddMoves ml, Board theBoard, int atSquare, int target) {
    	int direction = queen_get_direction(atSquare, target);
        multidirect_checkForMove_Black(ml, theBoard, BlackQueenMove_Lut[atSquare][direction]);
    }
    
    static int bishop_get_direction(int atSquare, int target) {
    	int at_file = BitBoard.getFile(atSquare);
    	int to_file = BitBoard.getFile(target);
    	int at_rank = BitBoard.getRank(atSquare);
    	int to_rank = BitBoard.getRank(target);
    	int direction = 0;
    	if (at_rank < to_rank) {
    		if (at_file < to_file) {
    			direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.upRight);
    		} else {
    			direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.upLeft);
    		}
    	} else {
    		if (at_file < to_file) {
	    		direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.downRight);
	    	} else {
	    		direction = SquareAttackEvaluator.directionIndex_Lut.get(Direction.downLeft);
	    	}
    	}
    	return direction;
    }

    static void bishop_checkMove_White(IAddMoves ml, Board theBoard, int atSquare, int target) {
    	int direction = bishop_get_direction(atSquare, target);
        multidirect_checkForMove_White(ml, theBoard, WhiteBishopMove_Lut[atSquare][direction]);    
    }
    
    static void bishop_checkMove_Black(IAddMoves ml, Board theBoard, int atSquare, int target) {
    	int direction = bishop_get_direction(atSquare, target);
        multidirect_checkForMove_Black(ml, theBoard, BlackBishopMove_Lut[atSquare][direction]);    
    }

    private static void multidirect_checkForMove_White(IAddMoves ml, Board theBoard, int[] moves) {
        for (int new_move : moves) {
            int targetPiece = theBoard.getPieceAtSquareEnemyBlack(1L << Move.getTargetPosition(new_move));
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
    
    private static void multidirect_checkForMove_Black(IAddMoves ml, Board theBoard, int[] moves) {
	    for (int new_move : moves) {
	        int targetPiece = theBoard.getPieceAtSquareEnemyWhite(1L << Move.getTargetPosition(new_move));
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
    
    private static void single_addMoves_White(IAddMoves ml, Board theBoard, int[] moves) {
        for (int new_move : moves) {
            int targetPiece = theBoard.getPieceAtSquareEnemyBlack(1L << Move.getTargetPosition(new_move));
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
    
    private static void single_addMoves_Black(IAddMoves ml, Board theBoard, int[] moves) {
        for (int new_move : moves) {
            int targetPiece = theBoard.getPieceAtSquareEnemyWhite(1L << Move.getTargetPosition(new_move));
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
    
    private static void single_addCaptures_White(IAddMoves ml, Board theBoard, int[] moves) {
        for (int new_move : moves) {
            int targetPiece = theBoard.getPieceAtSquareEnemyBlack(1L << Move.getTargetPosition(new_move));
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
        }
    }
    
    private static void single_addCaptures_Black(IAddMoves ml, Board theBoard, int[] moves) {
        for (int new_move : moves) {
            int targetPiece = theBoard.getPieceAtSquareEnemyWhite(1L << Move.getTargetPosition(new_move));
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
        }
    }
    
    private static int pawn_genOneSqTargetWhite(int bitOffset) {
        return bitOffset+8;
    }
    private static int pawn_genOneSqTargetBlack(int bitOffset) {
        return bitOffset-8;
    }   
    
    private static int pawn_genTwoSqTargetWhite(int bitOffset) {
        int moveTo = Position.NOPOSITION;
        if (BitBoard.getRank(bitOffset) == IntRank.R2) {
            // bound checking is implicit from start position check
            moveTo = bitOffset+16;
        }
        return moveTo;
    }
    
    private static int pawn_genTwoSqTargetBlack(int bitOffset) {
        int moveTo = Position.NOPOSITION;
        if (BitBoard.getRank(bitOffset) == IntRank.R7) {
            // bound checking is implicit from start position check
            moveTo = bitOffset-16;
        }
        return moveTo;
    }
    
    static long pawn_genLeftCaptureTargetWhite(int bitOffset) {
        return BitBoard.generatePawnCaptureTargetBoardUpLeft(bitOffset);
    }
    
    static long pawn_genRightCaptureTargetWhite(int bitOffset) {
        return BitBoard.generatePawnCaptureTargetBoardUpRight(bitOffset);
    }
    
    private static int pawn_isCapturableWhite(Board theBoard, long captureMask) {
        int capturePiece = Piece.NONE;
        int queryPiece = theBoard.getPieceAtSquareEnemyBlack(captureMask);
        if (queryPiece != Piece.NONE && queryPiece != Piece.DONT_CARE) {
            capturePiece = queryPiece;
        }
        return capturePiece;
    }
    
    private static boolean pawn_checkPromotionPossibleWhite(int targetBitOffset) {
        return BitBoard.getRank(targetBitOffset) == IntRank.R8;
    }
    
    private static void pawn_checkPromotionAddMoveWhite(int originBitOffset, IAddMoves ml, int targetBitOffset) {
        if (pawn_checkPromotionPossibleWhite(targetBitOffset)) {
            ml.addPrio(WhitePawnPromotionMove_Lut[originBitOffset][0]);
        } else {
            ml.addNormal(Move.valueOfBit(originBitOffset, Piece.WHITE_PAWN, targetBitOffset, Piece.NONE));
        }
    }
    
    private static void pawn_checkPromotionAddCaptureMoveWhite(int ownPiece, int originBitOffset, IAddMoves ml, int targetBitOffset, int targetPiece) {
        if (pawn_checkPromotionPossibleWhite(targetBitOffset)) {
            ml.addPrio(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, originBitOffset, ownPiece, targetBitOffset, targetPiece, Piece.QUEEN ));
        } else {
            ml.addPrio(Move.valueOfBit(originBitOffset, ownPiece, targetBitOffset, targetPiece));
        }
    }
    
    static long pawn_genLeftCaptureTargetBlack(int bitOffset) {
        return BitBoard.generatePawnCaptureTargetBoardDownRight(bitOffset);
    }
    
    static long pawn_genRightCaptureTargetBlack(int bitOffset) {
        return BitBoard.generatePawnCaptureTargetBoardDownLeft(bitOffset);
    }
    
    private static int pawn_isCapturableBlack(Board theBoard, long captureMask) {
        int capturePiece = Piece.NONE;
        int queryPiece = theBoard.getPieceAtSquareEnemyWhite(captureMask);
        if (queryPiece != Piece.NONE && queryPiece != Piece.DONT_CARE) {
            capturePiece = queryPiece;
        }
        return capturePiece;
    }
    
    private static boolean pawn_checkPromotionPossibleBlack(int targetBitOffset ) {
        return BitBoard.getRank(targetBitOffset) == IntRank.R1;
    }
    
    private static void pawn_checkPromotionAddMoveBlack(int originBitOffset, IAddMoves ml, int targetBitOffset) {
        if (pawn_checkPromotionPossibleBlack(targetBitOffset)) {
            ml.addPrio(BlackPawnPromotionMove_Lut[originBitOffset][0]);
        } else {
            ml.addNormal(Move.valueOfBit(originBitOffset, Piece.BLACK_PAWN, targetBitOffset, Piece.NONE));
        }
    }
    
    private static void pawn_checkPromotionAddCaptureMoveBlack(int ownPiece, int originBitOffset, IAddMoves ml,
            int targetBitOffset, int targetPiece) {
        if (pawn_checkPromotionPossibleBlack(targetBitOffset)) {
            ml.addPrio(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, originBitOffset, ownPiece, targetBitOffset, targetPiece, Piece.QUEEN ));
        } else {
            ml.addPrio(Move.valueOfBit(originBitOffset, ownPiece, targetBitOffset, targetPiece));
        }
    }
    
    static void pawn_generateMoves_White(IAddMoves ml, Board theBoard, int bitOffset) {
        int ownPiece = Piece.WHITE_PAWN;
        int capturePiece = Piece.NONE;
        // Check for standard one and two square moves
        int targetBitOffset = pawn_genOneSqTargetWhite(bitOffset);
        if (theBoard.squareIsEmpty(targetBitOffset)) {
            pawn_checkPromotionAddMoveWhite(bitOffset, ml, targetBitOffset);
            targetBitOffset = pawn_genTwoSqTargetWhite(bitOffset);
            if (targetBitOffset != Position.NOPOSITION && theBoard.squareIsEmpty(targetBitOffset)) {
                // Can't be a promotion or capture
                ml.addNormal(Move.valueOfBit(bitOffset, ownPiece, targetBitOffset , Piece.NONE));
            }   
        }
        // Check for capture moves, includes en passant
        int captureOffset = whitePawnLeftAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableWhite(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveWhite(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.BLACK_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
        captureOffset = whitePawnRightAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableWhite(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveWhite(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.BLACK_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
    }
    
    static void pawn_generateMoves_Black(IAddMoves ml, Board theBoard, int bitOffset) {
        int ownPiece = Piece.BLACK_PAWN;
        int capturePiece = Piece.NONE;
        // Check for standard one and two square moves
        int targetBitOffset = pawn_genOneSqTargetBlack(bitOffset);
        if (theBoard.squareIsEmpty(targetBitOffset)) {
            pawn_checkPromotionAddMoveBlack(bitOffset, ml, targetBitOffset);
            targetBitOffset = pawn_genTwoSqTargetBlack(bitOffset);
            if (targetBitOffset != Position.NOPOSITION && theBoard.squareIsEmpty(targetBitOffset)) {
                // Can't be a promotion or capture
                ml.addNormal(Move.valueOfBit(bitOffset, ownPiece, targetBitOffset , Piece.NONE));
            }   
        }
        // Check for capture moves, includes en passant
        int captureOffset = blackPawnLeftAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableBlack(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveBlack(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.WHITE_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
        captureOffset = blackPawnRightAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableBlack(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveBlack(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.WHITE_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
    }
    
    static void pawn_generateMovesForExtendedSearch_White(IAddMoves ml, Board theBoard, int bitOffset) {
        // Standard move
        int targetOffset = pawn_genOneSqTargetWhite(bitOffset);
        if (pawn_checkPromotionPossibleWhite(targetOffset) && theBoard.squareIsEmpty(targetOffset)) {
            ml.addPrio(WhitePawnPromotionMove_Lut[bitOffset][0]);
        }
        // Capture moves
        int ownPiece = Piece.WHITE_PAWN;
        int capturePiece = Piece.NONE;
        int captureOffset = whitePawnLeftAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableWhite(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveWhite(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.BLACK_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
        captureOffset = whitePawnRightAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableWhite(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveWhite(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.BLACK_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
    }
    
    static void pawn_generateMovesForExtendedSearch_Black(IAddMoves ml, Board theBoard, int bitOffset) {
        // Standard move
        int targetOffset = pawn_genOneSqTargetBlack(bitOffset);
        if (pawn_checkPromotionPossibleBlack(targetOffset) && theBoard.squareIsEmpty(targetOffset)) {
            ml.addPrio(BlackPawnPromotionMove_Lut[bitOffset][0]);
        }
        // Capture moves
        int ownPiece = Piece.BLACK_PAWN;
        int capturePiece = Piece.NONE;
        int captureOffset = blackPawnLeftAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableBlack(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveBlack(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.WHITE_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
        captureOffset = blackPawnRightAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            long captureMask = 1L << captureOffset;
            capturePiece = pawn_isCapturableBlack(theBoard, captureMask);
            if (capturePiece != Piece.NONE) {
                pawn_checkPromotionAddCaptureMoveBlack(ownPiece, bitOffset, ml, captureOffset, capturePiece);
            } else {
                int enPassant = theBoard.getEnPassantTargetSq();
                if (enPassant != BitBoard.INVALID && captureMask == (1L << enPassant)) {
                    capturePiece = Piece.WHITE_PAWN;
                    // promotion can't be possible if en passant capture
                    ml.addPrio(Move.valueOfEnPassantBit(0, bitOffset, ownPiece, captureOffset, capturePiece, Piece.NONE));
                }
            }
        }
    }
    
    static void pawn_generatePromotionMoves_White(IAddMoves ml, Board theBoard, int bitOffset) {
        // Standard move
        int targetOffset = pawn_genOneSqTargetWhite(bitOffset);
        if (pawn_checkPromotionPossibleWhite(targetOffset) && theBoard.squareIsEmpty(targetOffset)) {
            ml.addPrio(WhitePawnPromotionMove_Lut[bitOffset][0]);
        }
        // Capture moves
        int ownPiece = Piece.WHITE_PAWN;
        int capturePiece = Piece.NONE;
        int captureOffset = whitePawnLeftAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            capturePiece = pawn_isCapturableWhite(theBoard, 1L << captureOffset);
            if (capturePiece != Piece.NONE) {
                ml.addPrio(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, bitOffset, ownPiece, captureOffset, capturePiece, Piece.QUEEN));
            }
        }
        captureOffset = whitePawnRightAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            capturePiece = pawn_isCapturableWhite(theBoard, 1L << captureOffset);
            if (capturePiece != Piece.NONE) {
                ml.addPrio(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, bitOffset, ownPiece, captureOffset, capturePiece, Piece.QUEEN));
            }
        }
    }
    
    static void pawn_generatePromotionMoves_Black(IAddMoves ml, Board theBoard, int bitOffset) {
        // Standard move
        int targetOffset = pawn_genOneSqTargetBlack(bitOffset);
        if (pawn_checkPromotionPossibleBlack(targetOffset) && theBoard.squareIsEmpty(targetOffset)) {
            ml.addPrio(BlackPawnPromotionMove_Lut[bitOffset][0]);
        }
        // Capture moves
        int ownPiece = Piece.BLACK_PAWN;
        int capturePiece = Piece.NONE;
        int captureOffset = blackPawnLeftAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            capturePiece = pawn_isCapturableBlack(theBoard, 1L << captureOffset);
            if (capturePiece != Piece.NONE) {
                ml.addPrio(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, bitOffset, ownPiece, captureOffset, capturePiece, Piece.QUEEN));
            }
        }
        captureOffset = blackPawnRightAttacksAsOffset_Lut[bitOffset];
        if (captureOffset != Position.NOPOSITION) {
            capturePiece = pawn_isCapturableBlack(theBoard, 1L << captureOffset);
            if (capturePiece != Piece.NONE) {
                ml.addPrio(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, bitOffset, ownPiece, captureOffset, capturePiece, Piece.QUEEN));
            }
        }
    }
    
    /* 1-dimensional array:
     * 1st index is a position integer, this is the origin square
     * indexes a bit mask of the squares that the origin square can attack by a White Pawn capture */
    public static final int[] whitePawnLeftAttacksAsOffset_Lut = new int[64];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            whitePawnLeftAttacksAsOffset_Lut[bitOffset++] = createWhitePawnLeftCaptureMovesFromSq(square);
        }
    }
    static int createWhitePawnLeftCaptureMovesFromSq(int atPos) {
        int targetOffset = Position.NOPOSITION;
        if (Position.getRank(atPos) != 0) {
            int targetPosition = Direction.getDirectMoveSq(Direction.upLeft, atPos);
            if (targetPosition != Position.NOPOSITION) {
                targetOffset = BitBoard.positionToBit_Lut[targetPosition];
            }
        }
        return targetOffset;
    }
    
    /* 1-dimensional array:
     * 1st index is a position integer, this is the origin square
     * indexes a bit mask of the squares that the origin square can attack by a White Pawn capture */
    public static final int[] whitePawnRightAttacksAsOffset_Lut = new int[64];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            whitePawnRightAttacksAsOffset_Lut[bitOffset++] = createWhitePawnRightCaptureMovesFromSq(square);
        }
    }
    static int createWhitePawnRightCaptureMovesFromSq(int atPos) {
        int targetOffset = Position.NOPOSITION;
        if (Position.getRank(atPos) != 0) {
            int targetPosition = Direction.getDirectMoveSq(Direction.upRight, atPos);
            if (targetPosition != Position.NOPOSITION) {
                targetOffset = BitBoard.positionToBit_Lut[targetPosition];
            }
        }
        return targetOffset;
    }
    
    /* 1-dimensional array:
     * 1st index is a position integer, this is the origin square
     * indexes a bit mask of the squares that the origin square can attack by a White Pawn capture */
    public static final int[] blackPawnLeftAttacksAsOffset_Lut = new int[64];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            blackPawnLeftAttacksAsOffset_Lut[bitOffset++] = createBlackPawnLeftCaptureMovesFromSq(square);
        }
    }
    static int createBlackPawnLeftCaptureMovesFromSq(int atPos) {
        int targetOffset = Position.NOPOSITION;
        if (Position.getRank(atPos) != 7) {
            int targetPosition = Direction.getDirectMoveSq(Direction.downRight, atPos);
            if (targetPosition != Position.NOPOSITION) {
                targetOffset = BitBoard.positionToBit_Lut[targetPosition];
            }
        }
        return targetOffset;
    }
    
    /* 1-dimensional array:
     * 1st index is a position integer, this is the origin square
     * indexes a bit mask of the squares that the origin square can attack by a White Pawn capture */
    public static final int[] blackPawnRightAttacksAsOffset_Lut = new int[64];
    static {
        int bitOffset = 0;
        for (int square : Position.values) {
            blackPawnRightAttacksAsOffset_Lut[bitOffset++] = createblackPawnRightCaptureMovesFromSq(square);
        }
    }
    static int createblackPawnRightCaptureMovesFromSq(int atPos) {
        int targetOffset = Position.NOPOSITION;
        if (Position.getRank(atPos) != 7) {
            int targetPosition = Direction.getDirectMoveSq(Direction.downLeft, atPos);
            if (targetPosition != Position.NOPOSITION) {
                targetOffset = BitBoard.positionToBit_Lut[targetPosition];
            }
        }
        return targetOffset;
    }
}
