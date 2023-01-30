package eubos.board;

import java.util.Arrays;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntRank;

import eubos.main.EubosEngineMain;
import eubos.position.IAddMoves;
import eubos.position.Move;
import eubos.position.Position;

public abstract class Piece {
    public enum Colour { 
        white, black;
        
        public static Colour getOpposite( Colour arg ) { return (arg == white) ? black : white; }
        public static boolean isWhite( Colour arg ) { return arg == white; }
        public static boolean isBlack( Colour arg ) { return arg == black; }
    };
    
    // Note: Piece values below are not completely arbitrary, they must match Zobrist indexes
    // e.g. int pieceType = (currPiece & Piece.PIECE_NO_COLOUR_MASK) - 1; // convert piece type to Zobrist index
    public static final int NONE = 0x0;
	public static final int QUEEN = 0x1;
	public static final int BISHOP = 0x2;
	public static final int KNIGHT = 0x3;
	public static final int ROOK = 0x4;
	public static final int KING = 0x5;
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
    public static final int PIECE_LENGTH = (BLACK|DONT_CARE);
    
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
    
    public static final int [] PIECE_PHASE;
    static {
        PIECE_PHASE = new int[PIECE_LENGTH];
        PIECE_PHASE[WHITE_QUEEN] = PIECE_PHASE[BLACK_QUEEN] = 640;
        PIECE_PHASE[WHITE_ROOK] = PIECE_PHASE[BLACK_ROOK] = 320;
        PIECE_PHASE[WHITE_BISHOP] = PIECE_PHASE[WHITE_KNIGHT] = PIECE_PHASE[BLACK_BISHOP] = PIECE_PHASE[BLACK_KNIGHT] = 192;
    }
    
    static final byte[] P_W_MG_PST;
    static {
        P_W_MG_PST = new byte[64];
        P_W_MG_PST[BitBoard.a2] = 1; P_W_MG_PST[BitBoard.b2] = 3;  P_W_MG_PST[BitBoard.c2] = 9;  P_W_MG_PST[BitBoard.d2] = 15; P_W_MG_PST[BitBoard.e2] = 14; P_W_MG_PST[BitBoard.f2] = 19; P_W_MG_PST[BitBoard.g2] = 7;  P_W_MG_PST[BitBoard.h2] = -2;
        P_W_MG_PST[BitBoard.a3] = -7;P_W_MG_PST[BitBoard.b3] = -12;P_W_MG_PST[BitBoard.c3] = 9;  P_W_MG_PST[BitBoard.d3] = 14; P_W_MG_PST[BitBoard.e3] = 27; P_W_MG_PST[BitBoard.f3] = 21; P_W_MG_PST[BitBoard.g3] = 4;  P_W_MG_PST[BitBoard.h3] = -18;
        P_W_MG_PST[BitBoard.a4] = -1;P_W_MG_PST[BitBoard.b4] = -18;P_W_MG_PST[BitBoard.c4] = 6;  P_W_MG_PST[BitBoard.d4] = 17; P_W_MG_PST[BitBoard.e4] = 35; P_W_MG_PST[BitBoard.f4] = 15; P_W_MG_PST[BitBoard.g4] = 0;  P_W_MG_PST[BitBoard.h4] = -3;
        P_W_MG_PST[BitBoard.a5] = 9; P_W_MG_PST[BitBoard.b5] = -2; P_W_MG_PST[BitBoard.c5] = -9; P_W_MG_PST[BitBoard.d5] = 1;  P_W_MG_PST[BitBoard.e5] = 1;  P_W_MG_PST[BitBoard.f5] = 0;  P_W_MG_PST[BitBoard.g5] = -10;P_W_MG_PST[BitBoard.h5] = 4;
        P_W_MG_PST[BitBoard.a6] = 2; P_W_MG_PST[BitBoard.b6] = -9; P_W_MG_PST[BitBoard.c6] = -4; P_W_MG_PST[BitBoard.d6] = 20; P_W_MG_PST[BitBoard.e6] = 20; P_W_MG_PST[BitBoard.f6] = -4; P_W_MG_PST[BitBoard.g6] = -11;P_W_MG_PST[BitBoard.h6] = -9;
        P_W_MG_PST[BitBoard.a7] = -5;P_W_MG_PST[BitBoard.b7] = 4;  P_W_MG_PST[BitBoard.c7] = -1; P_W_MG_PST[BitBoard.d7] = -10;P_W_MG_PST[BitBoard.e7] = -9; P_W_MG_PST[BitBoard.f7] = -11;P_W_MG_PST[BitBoard.g7] = 8;  P_W_MG_PST[BitBoard.h7] = -6;
    }
    
    static final byte[] P_W_EG_PST;
    static {
        P_W_EG_PST = new byte[64];
        P_W_EG_PST[BitBoard.a2] = -5; P_W_EG_PST[BitBoard.b2] = -5; P_W_EG_PST[BitBoard.c2] = 5;  P_W_EG_PST[BitBoard.d2] = 4;  P_W_EG_PST[BitBoard.e2] = 10; P_W_EG_PST[BitBoard.f2] = 4; P_W_EG_PST[BitBoard.g2] = -3; P_W_EG_PST[BitBoard.h2] = -15;
        P_W_EG_PST[BitBoard.a3] = -5; P_W_EG_PST[BitBoard.b3] = -6; P_W_EG_PST[BitBoard.c3] = -8; P_W_EG_PST[BitBoard.d3] = 4;  P_W_EG_PST[BitBoard.e3] = 0;  P_W_EG_PST[BitBoard.f3] = 1; P_W_EG_PST[BitBoard.g3] = -5; P_W_EG_PST[BitBoard.h3] = -3;
        P_W_EG_PST[BitBoard.a4] = 5;  P_W_EG_PST[BitBoard.b4] = 0;  P_W_EG_PST[BitBoard.c4] = -5; P_W_EG_PST[BitBoard.d4] = 0;  P_W_EG_PST[BitBoard.e4] = -10;P_W_EG_PST[BitBoard.f4] = -9;P_W_EG_PST[BitBoard.g4] = -8; P_W_EG_PST[BitBoard.h4] = -5;
        P_W_EG_PST[BitBoard.a5] = 8;  P_W_EG_PST[BitBoard.b5] = 3;  P_W_EG_PST[BitBoard.c5] = 0;  P_W_EG_PST[BitBoard.d5] = -5; P_W_EG_PST[BitBoard.e5] = -3; P_W_EG_PST[BitBoard.f5] = -2;P_W_EG_PST[BitBoard.g5] = 12; P_W_EG_PST[BitBoard.h5] = 7;
        P_W_EG_PST[BitBoard.a6] = 23; P_W_EG_PST[BitBoard.b6] = 14; P_W_EG_PST[BitBoard.c6] = 17; P_W_EG_PST[BitBoard.d6] = 27; P_W_EG_PST[BitBoard.e6] = 28; P_W_EG_PST[BitBoard.f6] = 7; P_W_EG_PST[BitBoard.g6] = 8;  P_W_EG_PST[BitBoard.h6] = 10;
        P_W_EG_PST[BitBoard.a7] = 5;  P_W_EG_PST[BitBoard.b7] = -7; P_W_EG_PST[BitBoard.c7] = 11; P_W_EG_PST[BitBoard.d7] = 22; P_W_EG_PST[BitBoard.e7] = 21; P_W_EG_PST[BitBoard.f7] = 15;P_W_EG_PST[BitBoard.g7] = 5;  P_W_EG_PST[BitBoard.h7] = 5;
    } 
    
    static final byte[] R_W_MG_PST;
    static {
        R_W_MG_PST = new byte[64];
        R_W_MG_PST[BitBoard.a1] = -15; R_W_MG_PST[BitBoard.b1] = -10; R_W_MG_PST[BitBoard.c1] = -7; R_W_MG_PST[BitBoard.d1] = 0; R_W_MG_PST[BitBoard.e1] = R_W_MG_PST[BitBoard.d1]; R_W_MG_PST[BitBoard.f1] = R_W_MG_PST[BitBoard.c1]; R_W_MG_PST[BitBoard.g1] = R_W_MG_PST[BitBoard.b1]; R_W_MG_PST[BitBoard.h1] = R_W_MG_PST[BitBoard.a1];
        R_W_MG_PST[BitBoard.a2] = -10; R_W_MG_PST[BitBoard.b2] = -7; R_W_MG_PST[BitBoard.c2] = -2; R_W_MG_PST[BitBoard.d2] = 3; R_W_MG_PST[BitBoard.e2] = R_W_MG_PST[BitBoard.d2]; R_W_MG_PST[BitBoard.f2] = R_W_MG_PST[BitBoard.c2]; R_W_MG_PST[BitBoard.g2] = R_W_MG_PST[BitBoard.b2]; R_W_MG_PST[BitBoard.h2] = R_W_MG_PST[BitBoard.a2];
        R_W_MG_PST[BitBoard.a3] = -12; R_W_MG_PST[BitBoard.b3] = -5; R_W_MG_PST[BitBoard.c3] = 0; R_W_MG_PST[BitBoard.d3] = 1; R_W_MG_PST[BitBoard.e3] = R_W_MG_PST[BitBoard.d3]; R_W_MG_PST[BitBoard.f3] = R_W_MG_PST[BitBoard.c3]; R_W_MG_PST[BitBoard.g3] = R_W_MG_PST[BitBoard.b3]; R_W_MG_PST[BitBoard.h3] = R_W_MG_PST[BitBoard.a3];
        R_W_MG_PST[BitBoard.a4] = -7; R_W_MG_PST[BitBoard.b4] = -2; R_W_MG_PST[BitBoard.c4] = -1; R_W_MG_PST[BitBoard.d4] = -2; R_W_MG_PST[BitBoard.e4] = R_W_MG_PST[BitBoard.d4]; R_W_MG_PST[BitBoard.f4] = R_W_MG_PST[BitBoard.c4]; R_W_MG_PST[BitBoard.g4] = R_W_MG_PST[BitBoard.b4]; R_W_MG_PST[BitBoard.h4] = R_W_MG_PST[BitBoard.a4];
        R_W_MG_PST[BitBoard.a5] = -12; R_W_MG_PST[BitBoard.b5] = -8; R_W_MG_PST[BitBoard.c5] = -1; R_W_MG_PST[BitBoard.d5] = 1; R_W_MG_PST[BitBoard.e5] = R_W_MG_PST[BitBoard.d5]; R_W_MG_PST[BitBoard.f5] = R_W_MG_PST[BitBoard.c5]; R_W_MG_PST[BitBoard.g5] = R_W_MG_PST[BitBoard.b5]; R_W_MG_PST[BitBoard.h5] = R_W_MG_PST[BitBoard.a5];
        R_W_MG_PST[BitBoard.a6] = -11; R_W_MG_PST[BitBoard.b6] = -1; R_W_MG_PST[BitBoard.c6] = 2; R_W_MG_PST[BitBoard.d6] = 12; R_W_MG_PST[BitBoard.e6] = R_W_MG_PST[BitBoard.d6]; R_W_MG_PST[BitBoard.f6] = R_W_MG_PST[BitBoard.c6]; R_W_MG_PST[BitBoard.g6] = R_W_MG_PST[BitBoard.b6]; R_W_MG_PST[BitBoard.h6] = R_W_MG_PST[BitBoard.a6];
        R_W_MG_PST[BitBoard.a7] = 5; R_W_MG_PST[BitBoard.b7] = 7; R_W_MG_PST[BitBoard.c7] = 13; R_W_MG_PST[BitBoard.d7] = 20; R_W_MG_PST[BitBoard.e7] = R_W_MG_PST[BitBoard.d7]; R_W_MG_PST[BitBoard.f7] = R_W_MG_PST[BitBoard.c7]; R_W_MG_PST[BitBoard.g7] = R_W_MG_PST[BitBoard.b7]; R_W_MG_PST[BitBoard.h7] = R_W_MG_PST[BitBoard.a7];
        R_W_MG_PST[BitBoard.a8] = -7; R_W_MG_PST[BitBoard.b8] = -10; R_W_MG_PST[BitBoard.c8] = 0; R_W_MG_PST[BitBoard.d8] = 5; R_W_MG_PST[BitBoard.e8] = R_W_MG_PST[BitBoard.d8]; R_W_MG_PST[BitBoard.f8] = R_W_MG_PST[BitBoard.c8]; R_W_MG_PST[BitBoard.g8] = R_W_MG_PST[BitBoard.b8]; R_W_MG_PST[BitBoard.h8] = R_W_MG_PST[BitBoard.a8];
    }
    
    static final byte[] N_W_MG_PST;
    static {
        N_W_MG_PST = new byte[64];
        N_W_MG_PST[BitBoard.a1] = -75; N_W_MG_PST[BitBoard.b1] = -40;N_W_MG_PST[BitBoard.c1] = -32;N_W_MG_PST[BitBoard.d1] = -31; N_W_MG_PST[BitBoard.e1] = N_W_MG_PST[BitBoard.d1];N_W_MG_PST[BitBoard.f1] = N_W_MG_PST[BitBoard.c1];N_W_MG_PST[BitBoard.g1] = N_W_MG_PST[BitBoard.b1];N_W_MG_PST[BitBoard.h1] = N_W_MG_PST[BitBoard.a1];
        N_W_MG_PST[BitBoard.a2] = -40; N_W_MG_PST[BitBoard.b2] = -21;N_W_MG_PST[BitBoard.c2] = -20;N_W_MG_PST[BitBoard.d2] = -10; N_W_MG_PST[BitBoard.e2] = N_W_MG_PST[BitBoard.d2];N_W_MG_PST[BitBoard.f2] = N_W_MG_PST[BitBoard.c2];N_W_MG_PST[BitBoard.g2] = N_W_MG_PST[BitBoard.b2];N_W_MG_PST[BitBoard.h2] = N_W_MG_PST[BitBoard.a2];
        N_W_MG_PST[BitBoard.a3] = -35; N_W_MG_PST[BitBoard.b3] = -13;N_W_MG_PST[BitBoard.c3] = 5;N_W_MG_PST[BitBoard.d3] = 8; N_W_MG_PST[BitBoard.e3] = N_W_MG_PST[BitBoard.d3];N_W_MG_PST[BitBoard.f3] = N_W_MG_PST[BitBoard.c3];N_W_MG_PST[BitBoard.g3] = N_W_MG_PST[BitBoard.b3];N_W_MG_PST[BitBoard.h3] = N_W_MG_PST[BitBoard.a3];
        N_W_MG_PST[BitBoard.a4] = -20; N_W_MG_PST[BitBoard.b4] = 3;N_W_MG_PST[BitBoard.c4] = 18;N_W_MG_PST[BitBoard.d4] = 30; N_W_MG_PST[BitBoard.e4] = N_W_MG_PST[BitBoard.d4];N_W_MG_PST[BitBoard.f4] = N_W_MG_PST[BitBoard.c4];N_W_MG_PST[BitBoard.g4] = N_W_MG_PST[BitBoard.b4];N_W_MG_PST[BitBoard.h4] = N_W_MG_PST[BitBoard.a4];
        N_W_MG_PST[BitBoard.a5] = -19; N_W_MG_PST[BitBoard.b5] = 8;N_W_MG_PST[BitBoard.c5] = 20;N_W_MG_PST[BitBoard.d5] = 35; N_W_MG_PST[BitBoard.e5] = N_W_MG_PST[BitBoard.d5];N_W_MG_PST[BitBoard.f5] = N_W_MG_PST[BitBoard.c5];N_W_MG_PST[BitBoard.g5] = N_W_MG_PST[BitBoard.b5];N_W_MG_PST[BitBoard.h5] = N_W_MG_PST[BitBoard.a5];
        N_W_MG_PST[BitBoard.a6] = -5; N_W_MG_PST[BitBoard.b6] = 12;N_W_MG_PST[BitBoard.c6] = 35;N_W_MG_PST[BitBoard.d6] = 36; N_W_MG_PST[BitBoard.e6] = N_W_MG_PST[BitBoard.d6];N_W_MG_PST[BitBoard.f6] = N_W_MG_PST[BitBoard.c6];N_W_MG_PST[BitBoard.g6] = N_W_MG_PST[BitBoard.b6];N_W_MG_PST[BitBoard.h6] = N_W_MG_PST[BitBoard.a6];
        N_W_MG_PST[BitBoard.a7] = -38; N_W_MG_PST[BitBoard.b7] = -13;N_W_MG_PST[BitBoard.c7] = 2;N_W_MG_PST[BitBoard.d7] = 25; N_W_MG_PST[BitBoard.e7] = N_W_MG_PST[BitBoard.d7];N_W_MG_PST[BitBoard.f7] = N_W_MG_PST[BitBoard.c7];N_W_MG_PST[BitBoard.g7] = N_W_MG_PST[BitBoard.b7];N_W_MG_PST[BitBoard.h7] = N_W_MG_PST[BitBoard.a7];
        N_W_MG_PST[BitBoard.a8] = -80; N_W_MG_PST[BitBoard.b8] = -35;N_W_MG_PST[BitBoard.c8] = -20;N_W_MG_PST[BitBoard.d8] = -10; N_W_MG_PST[BitBoard.e8] = N_W_MG_PST[BitBoard.d8];N_W_MG_PST[BitBoard.f8] = N_W_MG_PST[BitBoard.c8];N_W_MG_PST[BitBoard.g8] = N_W_MG_PST[BitBoard.b8];N_W_MG_PST[BitBoard.h8] = N_W_MG_PST[BitBoard.a8];
    }
    
    static final byte[] N_EG_PST;
    static {
        N_EG_PST = new byte[64];
        N_EG_PST[BitBoard.a1] = -20;N_EG_PST[BitBoard.b1] = -10;N_EG_PST[BitBoard.c1] = -10;N_EG_PST[BitBoard.d1] = -10;N_EG_PST[BitBoard.e1] = -10;N_EG_PST[BitBoard.f1] = -10;N_EG_PST[BitBoard.g1] = -10;N_EG_PST[BitBoard.h1] = -20;
        N_EG_PST[BitBoard.a2] = -10;N_EG_PST[BitBoard.b2] = 0;N_EG_PST[BitBoard.c2] = 0;N_EG_PST[BitBoard.d2] = 0;N_EG_PST[BitBoard.e2] = 0;N_EG_PST[BitBoard.f2] = 0;N_EG_PST[BitBoard.g2] = 0;N_EG_PST[BitBoard.h2] = -10;
        N_EG_PST[BitBoard.a3] = -10;N_EG_PST[BitBoard.b3] = 0;N_EG_PST[BitBoard.c3] = 10;N_EG_PST[BitBoard.d3] = 10;N_EG_PST[BitBoard.e3] = 10;N_EG_PST[BitBoard.f3] = 10;N_EG_PST[BitBoard.g3] = 0;N_EG_PST[BitBoard.h3] = -10;
        N_EG_PST[BitBoard.a4] = -10;N_EG_PST[BitBoard.b4] = 0;N_EG_PST[BitBoard.c4] = 10;N_EG_PST[BitBoard.d4] = 20;N_EG_PST[BitBoard.e4] = 20;N_EG_PST[BitBoard.f4] = 10;N_EG_PST[BitBoard.g4] = 0;N_EG_PST[BitBoard.h4] = -10;
        N_EG_PST[BitBoard.a5] = -10;N_EG_PST[BitBoard.b5] = 0;N_EG_PST[BitBoard.c5] = 10;N_EG_PST[BitBoard.d5] = 20;N_EG_PST[BitBoard.e5] = 20;N_EG_PST[BitBoard.f5] = 10;N_EG_PST[BitBoard.g5] = 0;N_EG_PST[BitBoard.h5] = -10;
        N_EG_PST[BitBoard.a6] = -10;N_EG_PST[BitBoard.b6] = 0;N_EG_PST[BitBoard.c6] = 10;N_EG_PST[BitBoard.d6] = 10;N_EG_PST[BitBoard.e6] = 10;N_EG_PST[BitBoard.f6] = 10;N_EG_PST[BitBoard.g6] = 0;N_EG_PST[BitBoard.h6] = -10;
        N_EG_PST[BitBoard.a7] = -10;N_EG_PST[BitBoard.b7] = 0;N_EG_PST[BitBoard.c7] = 0;N_EG_PST[BitBoard.d7] = 0;N_EG_PST[BitBoard.e7] = 0;N_EG_PST[BitBoard.f7] = 0;N_EG_PST[BitBoard.g7] = 0;N_EG_PST[BitBoard.h7] = -10;
        N_EG_PST[BitBoard.a8] = -20;N_EG_PST[BitBoard.b8] = -10;N_EG_PST[BitBoard.c8] = -10;N_EG_PST[BitBoard.d8] = -10;N_EG_PST[BitBoard.e8] = -10;N_EG_PST[BitBoard.f8] = -10;N_EG_PST[BitBoard.g8] = -10;N_EG_PST[BitBoard.h8] = -20;
    }
    
    static final byte[] K_W_EG_PST;
    static {
        K_W_EG_PST = new byte[64];
        K_W_EG_PST[BitBoard.a1] = -30;K_W_EG_PST[BitBoard.b1] = -30;K_W_EG_PST[BitBoard.c1] = -30;K_W_EG_PST[BitBoard.d1] = -30;K_W_EG_PST[BitBoard.e1] = -30;K_W_EG_PST[BitBoard.f1] = -30;K_W_EG_PST[BitBoard.g1] = -30;K_W_EG_PST[BitBoard.h1] = -30;
        K_W_EG_PST[BitBoard.a2] = -30;K_W_EG_PST[BitBoard.b2] = -20;K_W_EG_PST[BitBoard.c2] = -20;K_W_EG_PST[BitBoard.d2] = -20;K_W_EG_PST[BitBoard.e2] = -20;K_W_EG_PST[BitBoard.f2] = -20;K_W_EG_PST[BitBoard.g2] = -20;K_W_EG_PST[BitBoard.h2] = -30;
        K_W_EG_PST[BitBoard.a3] = -30;K_W_EG_PST[BitBoard.b3] = -10;K_W_EG_PST[BitBoard.c3] = 0;K_W_EG_PST[BitBoard.d3] = 10;K_W_EG_PST[BitBoard.e3] = 10;K_W_EG_PST[BitBoard.f3] = 0;K_W_EG_PST[BitBoard.g3] = -10;K_W_EG_PST[BitBoard.h3] = -30;
        K_W_EG_PST[BitBoard.a4] = -20;K_W_EG_PST[BitBoard.b4] = -10;K_W_EG_PST[BitBoard.c4] = 10;K_W_EG_PST[BitBoard.d4] = 20;K_W_EG_PST[BitBoard.e4] = 20;K_W_EG_PST[BitBoard.f4] = 10;K_W_EG_PST[BitBoard.g4] = -10;K_W_EG_PST[BitBoard.h4] = -20;
        K_W_EG_PST[BitBoard.a5] = -20;K_W_EG_PST[BitBoard.b5] = -10;K_W_EG_PST[BitBoard.c5] = 10;K_W_EG_PST[BitBoard.d5] = 20;K_W_EG_PST[BitBoard.e5] = 20;K_W_EG_PST[BitBoard.f5] = 10;K_W_EG_PST[BitBoard.g5] = -10;K_W_EG_PST[BitBoard.h5] = -20;
        K_W_EG_PST[BitBoard.a6] = -30;K_W_EG_PST[BitBoard.b6] = -10;K_W_EG_PST[BitBoard.c6] = 0;K_W_EG_PST[BitBoard.d6] = 10;K_W_EG_PST[BitBoard.e6] = 10;K_W_EG_PST[BitBoard.f6] = 0;K_W_EG_PST[BitBoard.g6] = -10;K_W_EG_PST[BitBoard.h6] = -30;
        K_W_EG_PST[BitBoard.a7] = -30;K_W_EG_PST[BitBoard.b7] = -20;K_W_EG_PST[BitBoard.c7] = -20;K_W_EG_PST[BitBoard.d7] = -20;K_W_EG_PST[BitBoard.e7] = -20;K_W_EG_PST[BitBoard.f7] = -20;K_W_EG_PST[BitBoard.g7] = -20;K_W_EG_PST[BitBoard.h7] = -30;
        K_W_EG_PST[BitBoard.a8] = -30;K_W_EG_PST[BitBoard.b8] = -30;K_W_EG_PST[BitBoard.c8] = -30;K_W_EG_PST[BitBoard.d8] = -30;K_W_EG_PST[BitBoard.e8] = -30;K_W_EG_PST[BitBoard.f8] = -30;K_W_EG_PST[BitBoard.g8] = -30;K_W_EG_PST[BitBoard.h8] = -30;
    }
    
    static final byte[] K_W_MG_PST;
    static {
        K_W_MG_PST = new byte[64];
        K_W_MG_PST[BitBoard.a1] = 5;K_W_MG_PST[BitBoard.b1] = 25;K_W_MG_PST[BitBoard.c1] = 50;K_W_MG_PST[BitBoard.d1] = 0;K_W_MG_PST[BitBoard.e1] = 0;K_W_MG_PST[BitBoard.f1] = 5;K_W_MG_PST[BitBoard.g1] = 50;K_W_MG_PST[BitBoard.h1] = 5;
        K_W_MG_PST[BitBoard.a2] = 0;K_W_MG_PST[BitBoard.b2] = -10;K_W_MG_PST[BitBoard.c2] = -10;K_W_MG_PST[BitBoard.d2] = -10;K_W_MG_PST[BitBoard.e2] = -10;K_W_MG_PST[BitBoard.f2] = -10;K_W_MG_PST[BitBoard.g2] = -10;K_W_MG_PST[BitBoard.h2] = -10;
        K_W_MG_PST[BitBoard.a3] = -20;K_W_MG_PST[BitBoard.b3] = -20;K_W_MG_PST[BitBoard.c3] = -30;K_W_MG_PST[BitBoard.d3] = -30;K_W_MG_PST[BitBoard.e3] = -30;K_W_MG_PST[BitBoard.f3] = -30;K_W_MG_PST[BitBoard.g3] = -20;K_W_MG_PST[BitBoard.h3] = -20;
        K_W_MG_PST[BitBoard.a4] = -30;K_W_MG_PST[BitBoard.b4] = -40;K_W_MG_PST[BitBoard.c4] = -50;K_W_MG_PST[BitBoard.d4] = -50;K_W_MG_PST[BitBoard.e4] = -50;K_W_MG_PST[BitBoard.f4] = -40;K_W_MG_PST[BitBoard.g4] = -40;K_W_MG_PST[BitBoard.h4] = -30;
        K_W_MG_PST[BitBoard.a5] = -30;K_W_MG_PST[BitBoard.b5] = -40;K_W_MG_PST[BitBoard.c5] = -50;K_W_MG_PST[BitBoard.d5] = -50;K_W_MG_PST[BitBoard.e5] = -50;K_W_MG_PST[BitBoard.f5] = -40;K_W_MG_PST[BitBoard.g5] = -40;K_W_MG_PST[BitBoard.h5] = -30;
        K_W_MG_PST[BitBoard.a6] = -20;K_W_MG_PST[BitBoard.b6] = -20;K_W_MG_PST[BitBoard.c6] = -30;K_W_MG_PST[BitBoard.d6] = -30;K_W_MG_PST[BitBoard.e6] = -30;K_W_MG_PST[BitBoard.f6] = -30;K_W_MG_PST[BitBoard.g6] = -20;K_W_MG_PST[BitBoard.h6] = -20;
        K_W_MG_PST[BitBoard.a7] = -10;K_W_MG_PST[BitBoard.b7] = -10;K_W_MG_PST[BitBoard.c7] = -10;K_W_MG_PST[BitBoard.d7] = -10;K_W_MG_PST[BitBoard.e7] = -10;K_W_MG_PST[BitBoard.f7] = -10;K_W_MG_PST[BitBoard.g7] = -10;K_W_MG_PST[BitBoard.h7] = -10;
        K_W_MG_PST[BitBoard.a8] = 5;K_W_MG_PST[BitBoard.b8] = 25;K_W_MG_PST[BitBoard.c8] = 50;K_W_MG_PST[BitBoard.d8] = 0;K_W_MG_PST[BitBoard.e8] = 0;K_W_MG_PST[BitBoard.f8] = 5;K_W_MG_PST[BitBoard.g8] = 50;K_W_MG_PST[BitBoard.h8] = 5;
    }
    
    static final byte[] ZERO_WEIGHTING = new byte[64];   
    
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
    
    static byte[] makeBlackPstFromWhitePst(byte [] white) {
    	byte [] black = new byte[64];
    	for (int w_rank=0, b_rank=56; w_rank <= 56; w_rank+=8, b_rank-=8) {
	    	for (int file=0; file <8; file++) {
	    		black[b_rank+file] = (byte)-white[w_rank+file];
	    	}
    	}
    	return black;
    }
    
    static int [] createCombinedTable(byte [] mg_table, byte[] eg_table) {
    	int [] combined_table = new int [64];
    	for (int i=0; i < 64; i++) {
    		int mg = mg_table[i];
    		int eg = eg_table[i];
    		combined_table[i] = mg;
    		combined_table[i] &= 0x0000_FFFF;
    		combined_table[i] |= (eg << 16);
    	}
    	return combined_table;
    }
    
    public static final int [][] COMBINED_PIECE_SQUARE_TABLES;
    static {
    	COMBINED_PIECE_SQUARE_TABLES = new int[15][];
        int [] zero_combined = new int [64];
    	COMBINED_PIECE_SQUARE_TABLES[Piece.WHITE_PAWN] = createCombinedTable(P_W_MG_PST, P_W_EG_PST);
    	COMBINED_PIECE_SQUARE_TABLES[Piece.WHITE_KING] = createCombinedTable(K_W_MG_PST, K_W_EG_PST);
    	COMBINED_PIECE_SQUARE_TABLES[Piece.WHITE_QUEEN] = zero_combined;
    	COMBINED_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK] = createCombinedTable(R_W_MG_PST, ZERO_WEIGHTING);
    	COMBINED_PIECE_SQUARE_TABLES[Piece.WHITE_BISHOP] = zero_combined;
    	COMBINED_PIECE_SQUARE_TABLES[Piece.WHITE_KNIGHT] = createCombinedTable(N_W_MG_PST, N_EG_PST);
        
    	COMBINED_PIECE_SQUARE_TABLES[Piece.BLACK_PAWN] = createCombinedTable(makeBlackPstFromWhitePst(P_W_MG_PST), makeBlackPstFromWhitePst(P_W_EG_PST));
    	COMBINED_PIECE_SQUARE_TABLES[Piece.BLACK_KING] = createCombinedTable(makeBlackPstFromWhitePst(K_W_MG_PST), makeBlackPstFromWhitePst(K_W_EG_PST));
    	COMBINED_PIECE_SQUARE_TABLES[Piece.BLACK_QUEEN] = zero_combined;
    	COMBINED_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK] = createCombinedTable(makeBlackPstFromWhitePst(R_W_MG_PST), ZERO_WEIGHTING);
    	COMBINED_PIECE_SQUARE_TABLES[Piece.BLACK_BISHOP] = zero_combined;
    	COMBINED_PIECE_SQUARE_TABLES[Piece.BLACK_KNIGHT] = createCombinedTable(makeBlackPstFromWhitePst(N_W_MG_PST), makeBlackPstFromWhitePst(N_EG_PST));
    }
 
}
