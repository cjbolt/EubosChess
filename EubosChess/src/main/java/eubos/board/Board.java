package eubos.board;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.CastlingManager;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.Position;
import eubos.score.PiecewiseEvaluation;
import eubos.score.PositionEvaluator;

import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntRank;

public class Board {
	
	public static final boolean ENABLE_PIECE_LISTS = true;
	
	private static final long LIGHT_SQUARES_MASK = 0x55AA55AA55AA55AAL;
	private static final long DARK_SQUARES_MASK = 0xAA55AA55AA55AA55L; 
	
	private static final long[] FileMask_Lut = new long[8];
	static {
		for (int file : IntFile.values) {
			long mask = 0;
			int f=file;
			for (int r = 0; r<8; r++) {
				mask  |= 1L << r*8+f;
			}
			FileMask_Lut[file]= mask;
		}
	}
	
	private static final long[] RankMask_Lut = new long[8];
	static {
		for (int r : IntRank.values) {
			long mask = 0;
			for (int f = 0; f<8; f++) {
				mask  |= 1L << r*8+f;
			}
			RankMask_Lut[r] = mask;
		}
	}
	
	private static final long[][] PassedPawn_Lut = new long[2][]; 
	static {
		long[] white_map = new long[128];
		PassedPawn_Lut[Colour.white.ordinal()] = white_map;
		for (int atPos : Position.values) {
			white_map[atPos] = buildPassedPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), true);
		}
		long[] black_map = new long[128];
		PassedPawn_Lut[Colour.black.ordinal()] = black_map;
		for (int atPos : Position.values) {
			black_map[atPos] = buildPassedPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), false);
		}
	}
	private static long buildPassedPawnFileMask(int f, int r, boolean isWhite) {
		long mask = 0;
		boolean hasPrevFile = IntFile.toGenericFile(f).hasPrev();
		boolean hasNextFile = IntFile.toGenericFile(f).hasNext();
		if (isWhite) {
			for (r=r+1; r < 7; r++) {
				mask = addRankForPassedPawnMask(mask, r, f, hasPrevFile,
						hasNextFile);
			}
		} else {
			for (r=r-1; r > 0; r--) {
				mask = addRankForPassedPawnMask(mask, r, f, hasPrevFile,
						hasNextFile);	
			}
		}
		return mask;
	}
	private static long addRankForPassedPawnMask(long mask, int r, int f,
			boolean hasPrevFile, boolean hasNextFile) {
		if (hasPrevFile) {
			mask |= 1L << r*8+(f-1);
		}
		mask |= 1L << r*8+f;
		if (hasNextFile) {
			mask |= 1L << r*8+(f+1);
		}
		return mask;
	}
	
	private static final long[][] BackwardsPawn_Lut = new long[2][]; 
	static {
		long[] white_map = new long[128];
		BackwardsPawn_Lut[Colour.white.ordinal()] = white_map;
		for (int atPos : Position.values) {
			white_map[atPos] = buildBackwardPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), true);
		}
		long[] black_map = new long[128];
		BackwardsPawn_Lut[Colour.black.ordinal()] = black_map;
		for (int atPos : Position.values) {
			black_map[atPos] = buildBackwardPawnFileMask(Position.getFile(atPos), Position.getRank(atPos), false);
		}
	}
	private static long buildBackwardPawnFileMask(int f, int r, boolean isWhite) {
		long mask = 0;
		boolean hasPrevFile = IntFile.toGenericFile(f).hasPrev();
		boolean hasNextFile = IntFile.toGenericFile(f).hasNext();
		if (isWhite) {
			for (r=r-1; r > 0; r--) {
				mask = addRankForBackwardsPawnMask(mask, r, f, hasPrevFile, hasNextFile);
			}
		} else {
			for (r=r+1; r < 7; r++) {
				mask = addRankForBackwardsPawnMask(mask, r, f, hasPrevFile,	hasNextFile);	
			}
		}
		return mask;
	}
	private static long addRankForBackwardsPawnMask(long mask, int r, int f,
			boolean hasPrevFile, boolean hasNextFile) {
		if (hasPrevFile) {
			mask |= 1L << r*8+(f-1);
		}
		if (hasNextFile) {
			mask |= 1L << r*8+(f+1);
		}
		return mask;
	}
	
	private static final long[] IsolatedPawn_Lut = new long[128]; 
	static {
		for (int atPos : Position.values) {
			IsolatedPawn_Lut[atPos] = buildIsolatedPawnFileMask(Position.getFile(atPos));
		}
	}
	private static long buildIsolatedPawnFileMask(int f) {
		long mask = 0;
		boolean hasPrevFile = IntFile.toGenericFile(f).hasPrev();
		boolean hasNextFile = IntFile.toGenericFile(f).hasNext();
		if (hasPrevFile) {
			mask |= FileMask_Lut[f-1];
		}
		if (hasNextFile) {
			mask |= FileMask_Lut[f+1];
		}
		return mask;
	}
	
	private static long[] knightKingSafetyMask_Lut = new long[128];
	static {
		for (int atPos : Position.values) {
			knightKingSafetyMask_Lut[atPos] = buildKnightAttacksForKingZone(atPos);
		}
	}
	private static long buildKnightAttacksForKingZone(int atPos) {
		long mask = 0;
		long kingZone = SquareAttackEvaluator.KingMove_Lut[atPos];
		for (int knightPos : Position.values) {
			long knightMask = SquareAttackEvaluator.KnightMove_Lut[knightPos];
			if ((knightMask & kingZone) != 0) {
				mask |= BitBoard.positionToMask_Lut[knightPos];
			}
		}
		return mask;
	}
	
	public static final short MATERIAL_VALUE_KING = 4000;
	public static final short MATERIAL_VALUE_QUEEN = 900;
	public static final short MATERIAL_VALUE_ROOK = 490;
	public static final short MATERIAL_VALUE_BISHOP = 305;
	public static final short MATERIAL_VALUE_KNIGHT = 290;
	public static final short MATERIAL_VALUE_PAWN = 100;
	
    public static final short [] PIECE_TO_MATERIAL_LUT = {0, Board.MATERIAL_VALUE_KING, Board.MATERIAL_VALUE_QUEEN, Board.MATERIAL_VALUE_ROOK, 
    		Board.MATERIAL_VALUE_BISHOP, Board.MATERIAL_VALUE_KNIGHT, Board.MATERIAL_VALUE_PAWN };
	
	static final byte[] PAWN_WHITE_WEIGHTINGS;
    static {
    	PAWN_WHITE_WEIGHTINGS = new byte[128];
        PAWN_WHITE_WEIGHTINGS[Position.a1] = 0; PAWN_WHITE_WEIGHTINGS[Position.b1] = 0; PAWN_WHITE_WEIGHTINGS[Position.c1] = 0; PAWN_WHITE_WEIGHTINGS[Position.d1] = 0; PAWN_WHITE_WEIGHTINGS[Position.e1] = 0; PAWN_WHITE_WEIGHTINGS[Position.f1] = 0; PAWN_WHITE_WEIGHTINGS[Position.g1] = 0; PAWN_WHITE_WEIGHTINGS[Position.h1] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a2] = 0; PAWN_WHITE_WEIGHTINGS[Position.b2] = 0; PAWN_WHITE_WEIGHTINGS[Position.c2] = 0; PAWN_WHITE_WEIGHTINGS[Position.d2] = 0; PAWN_WHITE_WEIGHTINGS[Position.e2] = 0; PAWN_WHITE_WEIGHTINGS[Position.f2] = 0; PAWN_WHITE_WEIGHTINGS[Position.g2] = 0; PAWN_WHITE_WEIGHTINGS[Position.h2] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a3] = 0; PAWN_WHITE_WEIGHTINGS[Position.b3] = 0; PAWN_WHITE_WEIGHTINGS[Position.c3] = 0; PAWN_WHITE_WEIGHTINGS[Position.d3] = 5; PAWN_WHITE_WEIGHTINGS[Position.e3] = 5; PAWN_WHITE_WEIGHTINGS[Position.f3] = 0; PAWN_WHITE_WEIGHTINGS[Position.g3] = 0; PAWN_WHITE_WEIGHTINGS[Position.h3] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a4] = 0; PAWN_WHITE_WEIGHTINGS[Position.b4] = 0; PAWN_WHITE_WEIGHTINGS[Position.c4] = 3; PAWN_WHITE_WEIGHTINGS[Position.d4] = 8; PAWN_WHITE_WEIGHTINGS[Position.e4] = 8;PAWN_WHITE_WEIGHTINGS[Position.f4] = 3; PAWN_WHITE_WEIGHTINGS[Position.g4] = 0; PAWN_WHITE_WEIGHTINGS[Position.h4] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a5] = 2; PAWN_WHITE_WEIGHTINGS[Position.b5] = 3; PAWN_WHITE_WEIGHTINGS[Position.c5] = 8; PAWN_WHITE_WEIGHTINGS[Position.d5] = 12; PAWN_WHITE_WEIGHTINGS[Position.e5] = 12; PAWN_WHITE_WEIGHTINGS[Position.f5] = 8; PAWN_WHITE_WEIGHTINGS[Position.g5] = 3; PAWN_WHITE_WEIGHTINGS[Position.h5] = 2;
		PAWN_WHITE_WEIGHTINGS[Position.a6] = 4; PAWN_WHITE_WEIGHTINGS[Position.b6] = 8; PAWN_WHITE_WEIGHTINGS[Position.c6] = 12; PAWN_WHITE_WEIGHTINGS[Position.d6] = 16; PAWN_WHITE_WEIGHTINGS[Position.e6] = 16; PAWN_WHITE_WEIGHTINGS[Position.f6] = 12; PAWN_WHITE_WEIGHTINGS[Position.g6] = 8; PAWN_WHITE_WEIGHTINGS[Position.h6] = 4;
		PAWN_WHITE_WEIGHTINGS[Position.a7] = 5; PAWN_WHITE_WEIGHTINGS[Position.b7] = 10; PAWN_WHITE_WEIGHTINGS[Position.c7] = 15; PAWN_WHITE_WEIGHTINGS[Position.d7] = 20; PAWN_WHITE_WEIGHTINGS[Position.e7] = 20; PAWN_WHITE_WEIGHTINGS[Position.f7] = 15; PAWN_WHITE_WEIGHTINGS[Position.g7] = 10; PAWN_WHITE_WEIGHTINGS[Position.h7] = 5;
		PAWN_WHITE_WEIGHTINGS[Position.a8] = 0; PAWN_WHITE_WEIGHTINGS[Position.b8] = 0; PAWN_WHITE_WEIGHTINGS[Position.c8] = 0; PAWN_WHITE_WEIGHTINGS[Position.d8] = 0; PAWN_WHITE_WEIGHTINGS[Position.e8] = 0; PAWN_WHITE_WEIGHTINGS[Position.f8] = 0; PAWN_WHITE_WEIGHTINGS[Position.g8] = 0; PAWN_WHITE_WEIGHTINGS[Position.h8] = 0;
    }
    
	static final byte[] PAWN_BLACK_WEIGHTINGS;
    static {
    	PAWN_BLACK_WEIGHTINGS = new byte[128];
        PAWN_BLACK_WEIGHTINGS[Position.a1] = 0; PAWN_BLACK_WEIGHTINGS[Position.b1] = 0; PAWN_BLACK_WEIGHTINGS[Position.c1] = 0; PAWN_BLACK_WEIGHTINGS[Position.d1] = 0; PAWN_BLACK_WEIGHTINGS[Position.e1] = 0; PAWN_BLACK_WEIGHTINGS[Position.f1] = 0; PAWN_BLACK_WEIGHTINGS[Position.g1] = 0; PAWN_BLACK_WEIGHTINGS[Position.h1] = 0;
        PAWN_BLACK_WEIGHTINGS[Position.a2] = 5; PAWN_BLACK_WEIGHTINGS[Position.b2] = 10; PAWN_BLACK_WEIGHTINGS[Position.c2] = 15; PAWN_BLACK_WEIGHTINGS[Position.d2] = 20;PAWN_BLACK_WEIGHTINGS[Position.e2] = 20;PAWN_BLACK_WEIGHTINGS[Position.f2] = 15; PAWN_BLACK_WEIGHTINGS[Position.g2] = 10; PAWN_BLACK_WEIGHTINGS[Position.h2] = 5;
        PAWN_BLACK_WEIGHTINGS[Position.a3] = 4; PAWN_BLACK_WEIGHTINGS[Position.b3] = 8; PAWN_BLACK_WEIGHTINGS[Position.c3] = 12; PAWN_BLACK_WEIGHTINGS[Position.d3] = 16;PAWN_BLACK_WEIGHTINGS[Position.e3] = 16;PAWN_BLACK_WEIGHTINGS[Position.f3] = 12; PAWN_BLACK_WEIGHTINGS[Position.g3] = 8; PAWN_BLACK_WEIGHTINGS[Position.h3] = 4;
        PAWN_BLACK_WEIGHTINGS[Position.a4] = 2; PAWN_BLACK_WEIGHTINGS[Position.b4] = 3; PAWN_BLACK_WEIGHTINGS[Position.c4] = 8; PAWN_BLACK_WEIGHTINGS[Position.d4] = 12;PAWN_BLACK_WEIGHTINGS[Position.e4] = 12;PAWN_BLACK_WEIGHTINGS[Position.f4] = 8;PAWN_BLACK_WEIGHTINGS[Position.g4] = 3; PAWN_BLACK_WEIGHTINGS[Position.h4] = 2;
        PAWN_BLACK_WEIGHTINGS[Position.a5] = 0; PAWN_BLACK_WEIGHTINGS[Position.b5] = 0; PAWN_BLACK_WEIGHTINGS[Position.c5] = 3; PAWN_BLACK_WEIGHTINGS[Position.d5] = 8;PAWN_BLACK_WEIGHTINGS[Position.e5] = 8;PAWN_BLACK_WEIGHTINGS[Position.f5] = 3;PAWN_BLACK_WEIGHTINGS[Position.g5] = 0; PAWN_BLACK_WEIGHTINGS[Position.h5] = 0;
		PAWN_BLACK_WEIGHTINGS[Position.a6] = 0; PAWN_BLACK_WEIGHTINGS[Position.b6] = 0; PAWN_BLACK_WEIGHTINGS[Position.c6] = 0; PAWN_BLACK_WEIGHTINGS[Position.d6] = 5;PAWN_BLACK_WEIGHTINGS[Position.e6] = 5;PAWN_BLACK_WEIGHTINGS[Position.f6] = 0; PAWN_BLACK_WEIGHTINGS[Position.g6] = 0; PAWN_BLACK_WEIGHTINGS[Position.h6] = 0;
		PAWN_BLACK_WEIGHTINGS[Position.a7] = 0; PAWN_BLACK_WEIGHTINGS[Position.b7] = 0; PAWN_BLACK_WEIGHTINGS[Position.c7] = 0; PAWN_BLACK_WEIGHTINGS[Position.d7] = 0; PAWN_BLACK_WEIGHTINGS[Position.e7] = 0; PAWN_BLACK_WEIGHTINGS[Position.f7] = 0; PAWN_BLACK_WEIGHTINGS[Position.g7] = 0; PAWN_BLACK_WEIGHTINGS[Position.h7] = 0;
		PAWN_BLACK_WEIGHTINGS[Position.a8] = 0; PAWN_BLACK_WEIGHTINGS[Position.b8] = 0; PAWN_BLACK_WEIGHTINGS[Position.c8] = 0; PAWN_BLACK_WEIGHTINGS[Position.d8] = 0; PAWN_BLACK_WEIGHTINGS[Position.e8] = 0; PAWN_BLACK_WEIGHTINGS[Position.f8] = 0; PAWN_BLACK_WEIGHTINGS[Position.g8] = 0; PAWN_BLACK_WEIGHTINGS[Position.h8] = 0;
    }    
	
	static final byte[] KNIGHT_WEIGHTINGS;
    static {
    	KNIGHT_WEIGHTINGS = new byte[128];
        KNIGHT_WEIGHTINGS[Position.a1] = -20;KNIGHT_WEIGHTINGS[Position.b1] = -10;KNIGHT_WEIGHTINGS[Position.c1] = -10;KNIGHT_WEIGHTINGS[Position.d1] = -10;KNIGHT_WEIGHTINGS[Position.e1] = -10;KNIGHT_WEIGHTINGS[Position.f1] = -10;KNIGHT_WEIGHTINGS[Position.g1] = -10;KNIGHT_WEIGHTINGS[Position.h1] = -20;
		KNIGHT_WEIGHTINGS[Position.a2] = -10;KNIGHT_WEIGHTINGS[Position.b2] = 0;KNIGHT_WEIGHTINGS[Position.c2] = 0;KNIGHT_WEIGHTINGS[Position.d2] = 0;KNIGHT_WEIGHTINGS[Position.e2] = 0;KNIGHT_WEIGHTINGS[Position.f2] = 0;KNIGHT_WEIGHTINGS[Position.g2] = 0;KNIGHT_WEIGHTINGS[Position.h2] = -10;
		KNIGHT_WEIGHTINGS[Position.a3] = -10;KNIGHT_WEIGHTINGS[Position.b3] = 0;KNIGHT_WEIGHTINGS[Position.c3] = 10;KNIGHT_WEIGHTINGS[Position.d3] = 10;KNIGHT_WEIGHTINGS[Position.e3] = 10;KNIGHT_WEIGHTINGS[Position.f3] = 10;KNIGHT_WEIGHTINGS[Position.g3] = 0;KNIGHT_WEIGHTINGS[Position.h3] = -10;
		KNIGHT_WEIGHTINGS[Position.a4] = -10;KNIGHT_WEIGHTINGS[Position.b4] = 0;KNIGHT_WEIGHTINGS[Position.c4] = 10;KNIGHT_WEIGHTINGS[Position.d4] = 20;KNIGHT_WEIGHTINGS[Position.e4] = 20;KNIGHT_WEIGHTINGS[Position.f4] = 10;KNIGHT_WEIGHTINGS[Position.g4] = 0;KNIGHT_WEIGHTINGS[Position.h4] = -10;
		KNIGHT_WEIGHTINGS[Position.a5] = -10;KNIGHT_WEIGHTINGS[Position.b5] = 0;KNIGHT_WEIGHTINGS[Position.c5] = 10;KNIGHT_WEIGHTINGS[Position.d5] = 20;KNIGHT_WEIGHTINGS[Position.e5] = 20;KNIGHT_WEIGHTINGS[Position.f5] = 10;KNIGHT_WEIGHTINGS[Position.g5] = 0;KNIGHT_WEIGHTINGS[Position.h5] = -10;
		KNIGHT_WEIGHTINGS[Position.a6] = -10;KNIGHT_WEIGHTINGS[Position.b6] = 0;KNIGHT_WEIGHTINGS[Position.c6] = 10;KNIGHT_WEIGHTINGS[Position.d6] = 10;KNIGHT_WEIGHTINGS[Position.e6] = 10;KNIGHT_WEIGHTINGS[Position.f6] = 10;KNIGHT_WEIGHTINGS[Position.g6] = 0;KNIGHT_WEIGHTINGS[Position.h6] = -10;
		KNIGHT_WEIGHTINGS[Position.a7] = -10;KNIGHT_WEIGHTINGS[Position.b7] = 0;KNIGHT_WEIGHTINGS[Position.c7] = 0;KNIGHT_WEIGHTINGS[Position.d7] = 0;KNIGHT_WEIGHTINGS[Position.e7] = 0;KNIGHT_WEIGHTINGS[Position.f7] = 0;KNIGHT_WEIGHTINGS[Position.g7] = 0;KNIGHT_WEIGHTINGS[Position.h7] = -10;
		KNIGHT_WEIGHTINGS[Position.a8] = -20;KNIGHT_WEIGHTINGS[Position.b8] = -10;KNIGHT_WEIGHTINGS[Position.c8] = -10;KNIGHT_WEIGHTINGS[Position.d8] = -10;KNIGHT_WEIGHTINGS[Position.e8] = -10;KNIGHT_WEIGHTINGS[Position.f8] = -10;KNIGHT_WEIGHTINGS[Position.g8] = -10;KNIGHT_WEIGHTINGS[Position.h8] = -20;
    }
    
    static final byte[] KING_ENDGAME_WEIGHTINGS;
    static {
    	KING_ENDGAME_WEIGHTINGS = new byte[128];
        KING_ENDGAME_WEIGHTINGS[Position.a1] = -30;KING_ENDGAME_WEIGHTINGS[Position.b1] = -30;KING_ENDGAME_WEIGHTINGS[Position.c1] = -30;KING_ENDGAME_WEIGHTINGS[Position.d1] = -30;KING_ENDGAME_WEIGHTINGS[Position.e1] = -30;KING_ENDGAME_WEIGHTINGS[Position.f1] = -30;KING_ENDGAME_WEIGHTINGS[Position.g1] = -30;KING_ENDGAME_WEIGHTINGS[Position.h1] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a2] = -30;KING_ENDGAME_WEIGHTINGS[Position.b2] = -20;KING_ENDGAME_WEIGHTINGS[Position.c2] = -20;KING_ENDGAME_WEIGHTINGS[Position.d2] = -20;KING_ENDGAME_WEIGHTINGS[Position.e2] = -20;KING_ENDGAME_WEIGHTINGS[Position.f2] = -20;KING_ENDGAME_WEIGHTINGS[Position.g2] = -20;KING_ENDGAME_WEIGHTINGS[Position.h2] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a3] = -30;KING_ENDGAME_WEIGHTINGS[Position.b3] = -10;KING_ENDGAME_WEIGHTINGS[Position.c3] = 0;KING_ENDGAME_WEIGHTINGS[Position.d3] = 10;KING_ENDGAME_WEIGHTINGS[Position.e3] = 10;KING_ENDGAME_WEIGHTINGS[Position.f3] = 0;KING_ENDGAME_WEIGHTINGS[Position.g3] = -10;KING_ENDGAME_WEIGHTINGS[Position.h3] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a4] = -20;KING_ENDGAME_WEIGHTINGS[Position.b4] = -10;KING_ENDGAME_WEIGHTINGS[Position.c4] = 10;KING_ENDGAME_WEIGHTINGS[Position.d4] = 20;KING_ENDGAME_WEIGHTINGS[Position.e4] = 20;KING_ENDGAME_WEIGHTINGS[Position.f4] = 10;KING_ENDGAME_WEIGHTINGS[Position.g4] = -10;KING_ENDGAME_WEIGHTINGS[Position.h4] = -20;
		KING_ENDGAME_WEIGHTINGS[Position.a5] = -20;KING_ENDGAME_WEIGHTINGS[Position.b5] = -10;KING_ENDGAME_WEIGHTINGS[Position.c5] = 10;KING_ENDGAME_WEIGHTINGS[Position.d5] = 20;KING_ENDGAME_WEIGHTINGS[Position.e5] = 20;KING_ENDGAME_WEIGHTINGS[Position.f5] = 10;KING_ENDGAME_WEIGHTINGS[Position.g5] = -10;KING_ENDGAME_WEIGHTINGS[Position.h5] = -20;
		KING_ENDGAME_WEIGHTINGS[Position.a6] = -30;KING_ENDGAME_WEIGHTINGS[Position.b6] = -10;KING_ENDGAME_WEIGHTINGS[Position.c6] = 0;KING_ENDGAME_WEIGHTINGS[Position.d6] = 10;KING_ENDGAME_WEIGHTINGS[Position.e6] = 10;KING_ENDGAME_WEIGHTINGS[Position.f6] = 0;KING_ENDGAME_WEIGHTINGS[Position.g6] = -10;KING_ENDGAME_WEIGHTINGS[Position.h6] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a7] = -30;KING_ENDGAME_WEIGHTINGS[Position.b7] = -20;KING_ENDGAME_WEIGHTINGS[Position.c7] = -20;KING_ENDGAME_WEIGHTINGS[Position.d7] = -20;KING_ENDGAME_WEIGHTINGS[Position.e7] = -20;KING_ENDGAME_WEIGHTINGS[Position.f7] = -20;KING_ENDGAME_WEIGHTINGS[Position.g7] = -20;KING_ENDGAME_WEIGHTINGS[Position.h7] = -30;
		KING_ENDGAME_WEIGHTINGS[Position.a8] = -30;KING_ENDGAME_WEIGHTINGS[Position.b8] = -30;KING_ENDGAME_WEIGHTINGS[Position.c8] = -30;KING_ENDGAME_WEIGHTINGS[Position.d8] = -30;KING_ENDGAME_WEIGHTINGS[Position.e8] = -30;KING_ENDGAME_WEIGHTINGS[Position.f8] = -30;KING_ENDGAME_WEIGHTINGS[Position.g8] = -30;KING_ENDGAME_WEIGHTINGS[Position.h8] = -30;
    }
    
    static final byte[] KING_MIDGAME_WEIGHTINGS;
    static {
    	KING_MIDGAME_WEIGHTINGS = new byte[128];
        KING_MIDGAME_WEIGHTINGS[Position.a1] = 5;KING_MIDGAME_WEIGHTINGS[Position.b1] = 25;KING_MIDGAME_WEIGHTINGS[Position.c1] = 50;KING_MIDGAME_WEIGHTINGS[Position.d1] = 0;KING_MIDGAME_WEIGHTINGS[Position.e1] = 0;KING_MIDGAME_WEIGHTINGS[Position.f1] = 5;KING_MIDGAME_WEIGHTINGS[Position.g1] = 50;KING_MIDGAME_WEIGHTINGS[Position.h1] = 5;
		KING_MIDGAME_WEIGHTINGS[Position.a2] = 0;KING_MIDGAME_WEIGHTINGS[Position.b2] = -10;KING_MIDGAME_WEIGHTINGS[Position.c2] = -10;KING_MIDGAME_WEIGHTINGS[Position.d2] = -10;KING_MIDGAME_WEIGHTINGS[Position.e2] = -10;KING_MIDGAME_WEIGHTINGS[Position.f2] = -10;KING_MIDGAME_WEIGHTINGS[Position.g2] = -10;KING_MIDGAME_WEIGHTINGS[Position.h2] = -10;
		KING_MIDGAME_WEIGHTINGS[Position.a3] = -20;KING_MIDGAME_WEIGHTINGS[Position.b3] = -20;KING_MIDGAME_WEIGHTINGS[Position.c3] = -30;KING_MIDGAME_WEIGHTINGS[Position.d3] = -30;KING_MIDGAME_WEIGHTINGS[Position.e3] = -30;KING_MIDGAME_WEIGHTINGS[Position.f3] = -30;KING_MIDGAME_WEIGHTINGS[Position.g3] = -20;KING_MIDGAME_WEIGHTINGS[Position.h3] = -20;
		KING_MIDGAME_WEIGHTINGS[Position.a4] = -30;KING_MIDGAME_WEIGHTINGS[Position.b4] = -40;KING_MIDGAME_WEIGHTINGS[Position.c4] = -50;KING_MIDGAME_WEIGHTINGS[Position.d4] = -50;KING_MIDGAME_WEIGHTINGS[Position.e4] = -50;KING_MIDGAME_WEIGHTINGS[Position.f4] = -40;KING_MIDGAME_WEIGHTINGS[Position.g4] = -40;KING_MIDGAME_WEIGHTINGS[Position.h4] = -30;
		KING_MIDGAME_WEIGHTINGS[Position.a5] = -30;KING_MIDGAME_WEIGHTINGS[Position.b5] = -40;KING_MIDGAME_WEIGHTINGS[Position.c5] = -50;KING_MIDGAME_WEIGHTINGS[Position.d5] = -50;KING_MIDGAME_WEIGHTINGS[Position.e5] = -50;KING_MIDGAME_WEIGHTINGS[Position.f5] = -40;KING_MIDGAME_WEIGHTINGS[Position.g5] = -40;KING_MIDGAME_WEIGHTINGS[Position.h5] = -30;
		KING_MIDGAME_WEIGHTINGS[Position.a6] = -20;KING_MIDGAME_WEIGHTINGS[Position.b6] = -20;KING_MIDGAME_WEIGHTINGS[Position.c6] = -30;KING_MIDGAME_WEIGHTINGS[Position.d6] = -30;KING_MIDGAME_WEIGHTINGS[Position.e6] = -30;KING_MIDGAME_WEIGHTINGS[Position.f6] = -30;KING_MIDGAME_WEIGHTINGS[Position.g6] = -20;KING_MIDGAME_WEIGHTINGS[Position.h6] = -20;
		KING_MIDGAME_WEIGHTINGS[Position.a7] = -10;KING_MIDGAME_WEIGHTINGS[Position.b7] = -10;KING_MIDGAME_WEIGHTINGS[Position.c7] = -10;KING_MIDGAME_WEIGHTINGS[Position.d7] = -10;KING_MIDGAME_WEIGHTINGS[Position.e7] = -10;KING_MIDGAME_WEIGHTINGS[Position.f7] = -10;KING_MIDGAME_WEIGHTINGS[Position.g7] = -10;KING_MIDGAME_WEIGHTINGS[Position.h7] = -10;
		KING_MIDGAME_WEIGHTINGS[Position.a8] = 5;KING_MIDGAME_WEIGHTINGS[Position.b8] = 25;KING_MIDGAME_WEIGHTINGS[Position.c8] = 50;KING_MIDGAME_WEIGHTINGS[Position.d8] = 0;KING_MIDGAME_WEIGHTINGS[Position.e8] = 0;KING_MIDGAME_WEIGHTINGS[Position.f8] = 5;KING_MIDGAME_WEIGHTINGS[Position.g8] = 50;KING_MIDGAME_WEIGHTINGS[Position.h8] = 5;
    }
	
	private long allPieces = 0x0;
	private long whitePieces = 0x0;
	private long blackPieces = 0x0;
	
	public long getWhitePieces() {
		return whitePieces;
	}
	public long getBlackPieces() {
		return blackPieces;
	}

	private static final int INDEX_PAWN = Piece.PAWN;
	private static final int INDEX_KNIGHT = Piece.KNIGHT;
	private static final int INDEX_BISHOP = Piece.BISHOP;
	private static final int INDEX_ROOK = Piece.ROOK;
	private static final int INDEX_QUEEN = Piece.QUEEN;
	private static final int INDEX_KING = Piece.KING;
	//private static final int INDEX_NONE = Piece.NONE;
	
	private long[] pieces = new long[7]; // N.b. INDEX_NONE is an empty long at index 0.
	
	static final int ENDGAME_MATERIAL_THRESHOLD = 
			Board.MATERIAL_VALUE_KING + 
			Board.MATERIAL_VALUE_ROOK + 
			Board.MATERIAL_VALUE_KNIGHT + 
			(4 * Board.MATERIAL_VALUE_PAWN);
	
	static final int ENDGAME_MATERIAL_THRESHOLD_WITHOUT_QUEENS =
			Board.MATERIAL_VALUE_KING + 
			Board.MATERIAL_VALUE_ROOK + 
			Board.MATERIAL_VALUE_KNIGHT +
			Board.MATERIAL_VALUE_BISHOP +
			(4 * Board.MATERIAL_VALUE_PAWN);
	
	public boolean isEndgame;
	
	private PieceList pieceLists = new PieceList(this);
	
	public PiecewiseEvaluation me;
	
	public Board( Map<Integer, Integer> pieceMap,  Piece.Colour initialOnMove ) {
		allPieces = 0x0;
		whitePieces = 0x0;
		blackPieces = 0x0;
		for (int i=0; i<=INDEX_PAWN; i++) {
			pieces[i] = 0x0;
		}
		for ( Entry<Integer, Integer> nextPiece : pieceMap.entrySet() ) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue() );
		}
		isEndgame = false;
		me = evaluateMaterial();
		boolean queensOffBoard = (getWhiteQueens() == 0) && (getBlackQueens() == 0);
		int opponentMaterial = Piece.Colour.isWhite(initialOnMove) ? me.getBlack() : me.getWhite();
		boolean queensOffMaterialThresholdReached = opponentMaterial <= ENDGAME_MATERIAL_THRESHOLD_WITHOUT_QUEENS;
		boolean materialQuantityThreshholdReached = me.getWhite() <= ENDGAME_MATERIAL_THRESHOLD && me.getBlack() <= ENDGAME_MATERIAL_THRESHOLD;
		if ((queensOffBoard && queensOffMaterialThresholdReached) || materialQuantityThreshholdReached) {
			isEndgame = true;
		}
	}
	
	public static String reportStaticDataSizes() {
		StringBuilder s = new StringBuilder();
		int bytecountofstatics = PAWN_WHITE_WEIGHTINGS.length + PAWN_BLACK_WEIGHTINGS.length + KNIGHT_WEIGHTINGS.length + KING_ENDGAME_WEIGHTINGS.length + KING_MIDGAME_WEIGHTINGS.length;
		s.append(String.format("PieceSquareTables %d bytes\n", bytecountofstatics));
		int len = 0;
		for(int i = 0; i < PassedPawn_Lut.length; i++)
		{
		    len += PassedPawn_Lut[i].length;
		}
		s.append(String.format("PassedPawn_Lut %d bytes\n", len*8));
		s.append(String.format("FileMask_Lut %d bytes\n", FileMask_Lut.length*8));
		s.append(String.format("RankMask_Lut %d bytes\n", RankMask_Lut.length*8));
		return s.toString();
	}
	
	public String getAsFenString() {
		int currPiece = Piece.NONE;
		int spaceCounter = 0;
		StringBuilder fen = new StringBuilder();
		for (int rank=7; rank>=0; rank--) {
			for (int file=0; file<8; file++) {
				currPiece = this.getPieceAtSquare(Position.valueOf(file,rank));
				if (currPiece != Piece.NONE) {
					if (spaceCounter != 0)
						fen.append(spaceCounter);
					fen.append(Piece.toFenChar(currPiece));
					spaceCounter=0;					
				} else {
					spaceCounter++;
				}
			}
			if (spaceCounter != 0)
				fen.append(spaceCounter);
			if (rank != 0)
				fen.append('/');
			spaceCounter=0;
		}
		return fen.toString();
	}
	
	public int doMove(int move) {
		int capturePosition = Position.NOPOSITION;
		int pieceToMove = Move.getOriginPiece(move);
		boolean isWhite = Piece.isWhite(pieceToMove);
		int originSquare = Move.getOriginPosition(move);
		int targetSquare = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
		int promotedPiece = Move.getPromotion(move);
		long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
		long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
		long positionsMask = initialSquareMask | targetSquareMask;
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert (pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] & initialSquareMask) != 0: 
				String.format("Non-existant piece at %s", Position.toGenericPosition(originSquare));
		}
		
		// Initialise En Passant target square
		setEnPassantTargetSq(Position.NOPOSITION);
		
		if (targetPiece != Piece.NONE) {
			// Handle captures
			if (Move.isEnPassantCapture(move)) {
				// Handle en passant captures, don't need to do other checks in this case
				capturePosition = generateCapturePositionForEnPassant(pieceToMove, targetSquare);
			} else {
				capturePosition = targetSquare;
			}
			pickUpPieceAtSquare(capturePosition, targetPiece);
		} else {
			if (!moveEnablesEnPassantCapture(pieceToMove, originSquare, targetSquare)) {
				// Handle castling secondary rook moves...
				if (Piece.isKing(pieceToMove)) {
					performSecondaryCastlingMove(move);
				}
			}
		}
		
		// Switch piece-specific bitboards and piece lists
		if (promotedPiece != Piece.NONE) {
			// For a promotion, need to resolve piece-specific across multiple bitboards
			pieces[INDEX_PAWN] &= ~initialSquareMask;
			pieces[promotedPiece] |= targetSquareMask;
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(pieceToMove, (isWhite ? promotedPiece : promotedPiece|Piece.BLACK), originSquare, targetSquare);
			}
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] ^= positionsMask;
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(pieceToMove, originSquare, targetSquare);
			}
		}
		// Switch colour bitboard
		if (isWhite) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
		
		return capturePosition;
	}
	
	public int undoMove(int moveToUndo) {
		int capturedPieceSquare = Position.NOPOSITION;
		int originPiece = Move.getOriginPiece(moveToUndo);
		boolean isWhite = Piece.isWhite(originPiece);
		int originSquare = Move.getOriginPosition(moveToUndo);
		int targetSquare = Move.getTargetPosition(moveToUndo);
		int targetPiece = Move.getTargetPiece(moveToUndo);
		int promotedPiece = Move.getPromotion(moveToUndo);
		long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
		long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
		long positionsMask = initialSquareMask | targetSquareMask;
		boolean isCapture = targetPiece != Piece.NONE;
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			long pieceMask = (promotedPiece != Piece.NONE) ? pieces[promotedPiece] : pieces[Piece.PIECE_NO_COLOUR_MASK & originPiece];
			assert (pieceMask & initialSquareMask) != 0: String.format("Non-existant piece at %s, %s",
					Position.toGenericPosition(originSquare), Move.toString(moveToUndo));
		}
		
		// Handle reversal of any castling secondary rook moves on the board
		if (Piece.isKing(originPiece)) {
			unperformSecondaryCastlingMove(moveToUndo);
		}
		// Switch piece bitboard
		if (promotedPiece != Piece.NONE) {
			// Remove promoted piece and replace it with a pawn
			pieces[promotedPiece] &= ~initialSquareMask;	
			pieces[INDEX_PAWN] |= targetSquareMask;
			// and update piece list
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece((isWhite ? promotedPiece : promotedPiece|Piece.BLACK), originPiece, originSquare, targetSquare);
			}
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & originPiece] ^= positionsMask;
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(originPiece, originSquare, targetSquare);
			}
		}
		// Switch colour bitboard
		if (isWhite) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
		
		// Undo any capture that had been previously performed.
		if (isCapture) {
			// Origin square because the move has been reversed and origin square is the original target square
			capturedPieceSquare = Move.isEnPassantCapture(moveToUndo) ? 
					generateCapturePositionForEnPassant(originPiece, originSquare) : originSquare;
			setPieceAtSquare(capturedPieceSquare, targetPiece);
		}
		
		return capturedPieceSquare;
	}
	
	public int generateCapturePositionForEnPassant(int pieceToMove, int targetSquare) {
		if (pieceToMove == Piece.WHITE_PAWN) {
			targetSquare -= 16;
		} else if (pieceToMove == Piece.BLACK_PAWN){
			targetSquare += 16;
		}
		return targetSquare;
	}
	
	private boolean moveEnablesEnPassantCapture(int originPiece, int originSquare, int targetSquare) {
		boolean isEnPassantCapturePossible = false;
		if (Piece.isPawn(originPiece)) {
			if (Position.getRank(originSquare) == IntRank.R2) {
				if (Position.getRank(targetSquare) == IntRank.R4) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetSquare-16);
				}
			} else if (Position.getRank(originSquare) == IntRank.R7) {
				if (Position.getRank(targetSquare) == IntRank.R5) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetSquare+16);
				}
			}
		}
		return isEnPassantCapturePossible;
	}
	
	private static final long wksc_mask = BitBoard.positionToMask_Lut[Position.h1] | BitBoard.positionToMask_Lut[Position.f1];
	private static final long wqsc_mask = BitBoard.positionToMask_Lut[Position.a1] | BitBoard.positionToMask_Lut[Position.d1];
	private static final long bksc_mask = BitBoard.positionToMask_Lut[Position.h8] | BitBoard.positionToMask_Lut[Position.f8];
	private static final long bqsc_mask = BitBoard.positionToMask_Lut[Position.a8] | BitBoard.positionToMask_Lut[Position.d8];
	
	private void performSecondaryCastlingMove(int move) {
		if (Move.areEqual(move, CastlingManager.wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.WHITE_ROOK, Position.h1, Position.f1);
			}
		} else if (Move.areEqual(move, CastlingManager.wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.WHITE_ROOK, Position.a1, Position.d1);
			}
		} else if (Move.areEqual(move, CastlingManager.bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.BLACK_ROOK, Position.h8, Position.f8);
			}
		} else if (Move.areEqual(move, CastlingManager.bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.BLACK_ROOK, Position.a8, Position.d8);
			}
		}
	}
	
	private void unperformSecondaryCastlingMove(int move) {
		if (Move.areEqual(move, CastlingManager.undo_wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.WHITE_ROOK, Position.f1, Position.h1);
			}
		} else if (Move.areEqual(move, CastlingManager.undo_wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.WHITE_ROOK, Position.d1, Position.a1);
			}
		} else if (Move.areEqual(move, CastlingManager.undo_bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.BLACK_ROOK, Position.f8, Position.h8);
			}
		} else if (Move.areEqual(move, CastlingManager.undo_bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			if (ENABLE_PIECE_LISTS) {
				pieceLists.updatePiece(Piece.BLACK_ROOK, Position.d8, Position.a8);
			}
		}
	}
	
	private int enPassantTargetSq = Position.NOPOSITION;
	public int getEnPassantTargetSq() {
		return enPassantTargetSq;
	}
	public void setEnPassantTargetSq(int enPassantTargetSq) {
		// TODO: add bounds checking - only certain en passant squares can be legal.
		this.enPassantTargetSq = enPassantTargetSq;
	}
	
	public boolean squareIsEmpty( int atPos ) {
		return (allPieces & BitBoard.positionToMask_Lut[atPos]) == 0;		
	}
	
	public boolean squareIsAttacked( int atPos, Piece.Colour attackingColour ) {
		return SquareAttackEvaluator.isAttacked(this, atPos, attackingColour);
	}
	
	public int getPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		long pieceToGet = BitBoard.positionToMask_Lut[atPos];;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert (whitePieces & pieceToGet) != 0;
			}
			// Sorted in order of frequency of piece on the chess board, for efficiency
			if ((pieces[INDEX_PAWN] & pieceToGet) != 0) {
				type |= Piece.PAWN;
			} else if ((pieces[INDEX_ROOK] & pieceToGet) != 0) {
				type |= Piece.ROOK;
			} else if ((pieces[INDEX_BISHOP] & pieceToGet) != 0) {
				type |= Piece.BISHOP;
			} else if ((pieces[INDEX_KNIGHT] & pieceToGet) != 0) {
				type |= Piece.KNIGHT;
			} else if ((pieces[INDEX_KING] & pieceToGet) != 0) {
				type |= Piece.KING;
			} else if ((pieces[INDEX_QUEEN] & pieceToGet) != 0) {
				type |= Piece.QUEEN;
			}
		}
		return type;
	}
	
	public int getPieceAtSquareOptimise( int atPos, boolean ownSideIsWhite ) {
		int type = Piece.NONE;
		long pieceToGet = BitBoard.positionToMask_Lut[atPos];;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				if (!ownSideIsWhite) return Piece.DONT_CARE;
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert (whitePieces & pieceToGet) != 0;
				if (ownSideIsWhite) return Piece.DONT_CARE;
			}
			// Sorted in order of frequency of piece on the chess board, for efficiency
			if ((pieces[INDEX_PAWN] & pieceToGet) != 0) {
				type |= Piece.PAWN;
			} else if ((pieces[INDEX_ROOK] & pieceToGet) != 0) {
				type |= Piece.ROOK;
			} else if ((pieces[INDEX_BISHOP] & pieceToGet) != 0) {
				type |= Piece.BISHOP;
			} else if ((pieces[INDEX_KNIGHT] & pieceToGet) != 0) {
				type |= Piece.KNIGHT;
			} else if ((pieces[INDEX_KING] & pieceToGet) != 0) {
				type |= Piece.KING;
			} else if ((pieces[INDEX_QUEEN] & pieceToGet) != 0) {
				type |= Piece.QUEEN;
			}
		}
		return type;
	}

	public void setPieceAtSquare( int atPos, int pieceToPlace ) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert atPos != Position.NOPOSITION;
			assert pieceToPlace != Piece.NONE;
		}
		if (ENABLE_PIECE_LISTS) {
			pieceLists.addPiece(pieceToPlace, atPos);
		}
		long mask = BitBoard.positionToMask_Lut[atPos];
		// Set on piece-specific bitboard
		pieces[pieceToPlace & Piece.PIECE_NO_COLOUR_MASK] |= mask;
		// Set on colour bitboard
		if (Piece.isBlack(pieceToPlace)) {
			blackPieces |= (mask);
		} else {
			whitePieces |= (mask);
		}
		// Set on all pieces bitboard
		allPieces |= (mask);
	}
	
	public boolean isKingInCheck(Piece.Colour side) {
		boolean inCheck = false;
		int kingSquare = Position.NOPOSITION;
		if (ENABLE_PIECE_LISTS) {
			kingSquare = pieceLists.getKingPos(Colour.isWhite(side));
		} else {
			long king = (Piece.Colour.isWhite(side)) ? getWhiteKing() : getBlackKing();

			if (king == 0)  return false;
			
			kingSquare = BitBoard.bitToPosition_Lut[Long.numberOfTrailingZeros(king)];
		}
		if (kingSquare != Position.NOPOSITION) {
			// The conditional is needed because some unit test positions don't have a king...
			inCheck = squareIsAttacked(kingSquare, Piece.Colour.getOpposite(side));
		}
		return inCheck;
	}
	
	public int pickUpPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		long pieceToPickUp = BitBoard.positionToMask_Lut[atPos];
		if ((allPieces & pieceToPickUp) != 0) {	
			// Remove from relevant colour bitboard
			if ((blackPieces & pieceToPickUp) != 0) {
				blackPieces &= ~pieceToPickUp;
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert (whitePieces & pieceToPickUp) != 0;
				whitePieces &= ~pieceToPickUp;
			}
			// Remove from specific-piece bitboard
			if ((pieces[INDEX_KING] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_KING] &= ~pieceToPickUp;
				type |= Piece.KING;
			} else if ((pieces[INDEX_QUEEN] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_QUEEN] &= ~pieceToPickUp;
				type |= Piece.QUEEN;
			} else if ((pieces[INDEX_ROOK] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_ROOK] &= ~pieceToPickUp;
				type |= Piece.ROOK;
			} else if ((pieces[INDEX_BISHOP] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_BISHOP] &= ~pieceToPickUp;
				type |= Piece.BISHOP;
			} else if ((pieces[INDEX_KNIGHT] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_KNIGHT] &= ~pieceToPickUp;
				type |= Piece.KNIGHT;
			} else if ((pieces[INDEX_PAWN] & pieceToPickUp) == pieceToPickUp) {
				pieces[INDEX_PAWN] &= ~pieceToPickUp;
				type |= Piece.PAWN;
			}
			// Remove from all pieces bitboard
			allPieces &= ~pieceToPickUp;
			// Remove from piece list
			if (ENABLE_PIECE_LISTS) {
				pieceLists.removePiece(type, atPos);
			}
		}
		return type;
	}
	
	public int pickUpPieceAtSquare( int atPos, int piece ) {
		long pieceToPickUp = BitBoard.positionToMask_Lut[atPos];
		if ((allPieces & pieceToPickUp) != 0) {	
			// Remove from relevant colour bitboard
			if (Piece.isBlack(piece)) {
				blackPieces &= ~pieceToPickUp;
			} else {
				whitePieces &= ~pieceToPickUp;
			}
			// remove from specific bitboard
			pieces[piece & Piece.PIECE_NO_COLOUR_MASK] &= ~pieceToPickUp;
			// Remove from all pieces bitboard
			allPieces &= ~pieceToPickUp;
			// Remove from piece list
			if (ENABLE_PIECE_LISTS) {
				pieceLists.removePiece(piece, atPos);
			}
		} else {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert false : String.format("Non-existant target piece at %s", Position.toGenericPosition(atPos));
			}
			piece = Piece.NONE;
		}
		return piece;
	}
	
	public int countDoubledPawnsForSide(Colour side) {
		int doubledCount = 0;
		long pawns = Colour.isWhite(side) ? getWhitePawns() : getBlackPawns();
		for (int file : IntFile.values) {
			long mask = FileMask_Lut[file];
			long fileMask = pawns & mask;
			int numPawnsInFile = Long.bitCount(fileMask);
			if (numPawnsInFile > 1) {
				doubledCount += numPawnsInFile-1;
			}
		}
		return doubledCount;
	}
	
	public boolean isPassedPawn(int atPos, Colour side) {
		boolean isPassed = true;
		long mask = PassedPawn_Lut[side.ordinal()][atPos];
		long otherSidePawns = Colour.isWhite(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & otherSidePawns) != 0) {
			isPassed  = false;
		}
		return isPassed;
	}
	
	public boolean isBackwardsPawn(int atPos, Colour side) {
		boolean isBackwards = true;
		long mask = BackwardsPawn_Lut[side.ordinal()][atPos];
		long ownSidePawns = Colour.isBlack(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & ownSidePawns) != 0) {
			isBackwards  = false;
		}
		return isBackwards;
	}
	
	public boolean isIsolatedPawn(int atPos, Colour side) {
		boolean isIsolated = true;
		long mask = IsolatedPawn_Lut[atPos];
		long ownSidePawns = Colour.isBlack(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & ownSidePawns) != 0) {
			isIsolated  = false;
		}
		return isIsolated;
	}
	
	class allPiecesOnBoardIterator implements PrimitiveIterator.OfInt {	
		private int[] pieces = null;
		private int count = 0;
		private int next = 0;

		allPiecesOnBoardIterator()  {
			pieces = new int[64];
			buildIterList(allPieces);
		}

		allPiecesOnBoardIterator( int typeToIterate )  {
			pieces = new int[64];
			long bitBoardToIterate;
			if (typeToIterate == Piece.WHITE_PAWN) {
				bitBoardToIterate = getWhitePawns();
			} else if (typeToIterate == Piece.BLACK_PAWN) {
				bitBoardToIterate = getBlackPawns();
			} else {
				bitBoardToIterate = 0x0;
			}
			buildIterList(bitBoardToIterate);
		}

		private void buildIterList(long bitBoardToIterate) {
			PrimitiveIterator.OfInt iter = BitBoard.iterator(bitBoardToIterate);
			while (iter.hasNext()) {
				int bit_index = iter.nextInt();
				pieces[count++] = BitBoard.bitToPosition_Lut[bit_index];
			}
		}	

		public boolean hasNext() {
			return next < pieces.length && next < count;
		}

		public Integer next() {
			assert false; // should always use nextInt()
			return pieces[next++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
		}

		@Override
		public int nextInt() {
			return pieces[next++];
		}
	}

	public PrimitiveIterator.OfInt iterator() {
		// default iterator returns all the pieces on the board, not all positions
		return new allPiecesOnBoardIterator( );
	}
		
	public long getBlackPawns() {
		return blackPieces & (pieces[INDEX_PAWN]);
	}
	
	public long getBlackKnights() {
		return blackPieces & (pieces[INDEX_KNIGHT]);
	}
	
	public long getBlackBishops() {
		return blackPieces & (pieces[INDEX_BISHOP]);
	}
	
	public long getBlackRooks() {
		return blackPieces & (pieces[INDEX_ROOK]);
	}
	
	public long getBlackQueens() {
		return blackPieces & (pieces[INDEX_QUEEN]);
	}
	
	public long getBlackKing() {
		return blackPieces & (pieces[INDEX_KING]);
	}
	
	public long getWhitePawns() {
		return whitePieces & (pieces[INDEX_PAWN]);
	}
	
	public long getWhiteBishops() {
		return whitePieces & (pieces[INDEX_BISHOP]);
	}
	
	public long getWhiteRooks() {
		return whitePieces & (pieces[INDEX_ROOK]);
	}
	
	public long getWhiteQueens() {
		return whitePieces & (pieces[INDEX_QUEEN]);
	}
	
	public long getWhiteKnights() {
		return whitePieces & (pieces[INDEX_KNIGHT]);
	}
	
	public long getWhiteKing() {
		return whitePieces & (pieces[INDEX_KING]);
	}
	
	public boolean isOnHalfOpenFile(GenericPosition atPos, int type) {
		boolean isHalfOpen = false;
		long fileMask = FileMask_Lut[IntFile.valueOf(atPos.file)];
		long otherSide = Piece.getOpposite(type) == Colour.white ? whitePieces : blackPieces;
		long pawnMask = otherSide & (pieces[INDEX_PAWN]);
		boolean opponentPawnOnFile = (pawnMask & fileMask) != 0;
		if (opponentPawnOnFile) {
			long ownSide = Piece.isWhite(type) ? whitePieces : blackPieces;
			pawnMask = ownSide & (pieces[INDEX_PAWN]);
			// and no pawns of own side
			isHalfOpen = !((pawnMask & fileMask) != 0);
		}
		return isHalfOpen;
	}
	
	public boolean moveCouldLeadToOwnKingDiscoveredCheck(int move, int piece) {
		boolean couldLeadToDiscoveredCheck = false;
		int kingPosition = Position.NOPOSITION;
		if (ENABLE_PIECE_LISTS) {
			kingPosition = pieceLists.getKingPos(Piece.isWhite(piece));
		} else {
			long king = (Piece.isWhite(piece)) ? getWhiteKing() : getBlackKing();

			if (king == 0)  return false;
			
			kingPosition = BitBoard.bitToPosition_Lut[Long.numberOfTrailingZeros(king)];
		}
		if (kingPosition != Position.NOPOSITION) {
			couldLeadToDiscoveredCheck = SquareAttackEvaluator.moveCouldLeadToDiscoveredCheck(move, kingPosition);
		}
		return couldLeadToDiscoveredCheck;
	}
	
	private boolean isPromotionPawnBlocked(long pawns, Direction dir) {
		boolean potentialPromotion = false;
		long scratchBitBoard = pawns;
		while ( scratchBitBoard != 0x0L ) {
			int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
			int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
			if (squareIsEmpty(Direction.getDirectMoveSq(dir, atSquare))) {
				potentialPromotion = true;
				break;
			}
			// clear the lssb
			scratchBitBoard &= scratchBitBoard-1;
		}
		return potentialPromotion;
	}
	
	public boolean isPromotionPossible(Colour onMove) {
		// TODO At the moment this doesn't consider if the pawn is pinned.
		boolean potentialPromotion = false;
		if (Piece.Colour.isWhite(onMove)) {
			long pawns = getWhitePawns() & (RankMask_Lut[IntRank.R7]);
			if (pawns != 0L) {
				potentialPromotion = isPromotionPawnBlocked(pawns, Direction.up);
			}
		} else {
			long pawns = getBlackPawns() & (RankMask_Lut[IntRank.R2]);
			if (pawns != 0L) {
				potentialPromotion = isPromotionPawnBlocked(pawns, Direction.down);
			}
		}
		return potentialPromotion;
	}
	
	public void getRegularPieceMoves(MoveList ml, boolean ownSideIsWhite, boolean captures, int potentialAttckersOfSquare) {
		if (ENABLE_PIECE_LISTS) {
			if (isEndgame) {
				pieceLists.addMovesEndgame(ml, ownSideIsWhite, captures, potentialAttckersOfSquare);
			} else {
				pieceLists.addMovesMiddlegame(ml, ownSideIsWhite, captures, potentialAttckersOfSquare);
			}
		} else {
			long bitBoardToIterate = ownSideIsWhite ? whitePieces : blackPieces;
			List<Integer> movesList = new LinkedList<Integer>();
			long potentialAttackersMask = (potentialAttckersOfSquare != Position.NOPOSITION) ? SquareAttackEvaluator.allAttacksOnPosition_Lut[potentialAttckersOfSquare] : -1;
			long scratchBitBoard = 0;
			// Unrolled loop for performance optimisation...
			if (isEndgame) {
				scratchBitBoard = bitBoardToIterate & pieces[INDEX_KING];
				scratchBitBoard &= potentialAttackersMask;
				while ( scratchBitBoard != 0x0L ) {
					int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
					int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
					Piece.king_generateMoves(ml, this, atSquare, ownSideIsWhite);
					scratchBitBoard &= scratchBitBoard-1L;
				}
			}
			scratchBitBoard = bitBoardToIterate & pieces[INDEX_QUEEN];
			scratchBitBoard &= potentialAttackersMask;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				Piece.queen_generateMoves(ml, this, atSquare, ownSideIsWhite);
				scratchBitBoard &= scratchBitBoard-1L;
			}
			scratchBitBoard = bitBoardToIterate & pieces[INDEX_ROOK];
			scratchBitBoard &= potentialAttackersMask;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				Piece.rook_generateMoves(ml, this, atSquare, ownSideIsWhite);
				scratchBitBoard &= scratchBitBoard-1L;
			}
			scratchBitBoard = bitBoardToIterate & pieces[INDEX_BISHOP];
			scratchBitBoard &= potentialAttackersMask;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				Piece.bishop_generateMoves(ml, this, atSquare, ownSideIsWhite);
				scratchBitBoard &= scratchBitBoard-1L;
			}
			scratchBitBoard = bitBoardToIterate & pieces[INDEX_KNIGHT];
			scratchBitBoard &= potentialAttackersMask;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				Piece.knight_generateMoves(ml, this, atSquare, ownSideIsWhite);
				scratchBitBoard &= scratchBitBoard-1L;
			}
			scratchBitBoard = bitBoardToIterate & pieces[INDEX_PAWN];
			//scratchBitBoard &= potentialAttackersMask;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				Piece.pawn_generateMoves(ml, this, atSquare, ownSideIsWhite);
				scratchBitBoard &= scratchBitBoard-1L;
			}
			if (!isEndgame) {
				scratchBitBoard = bitBoardToIterate & pieces[INDEX_KING];
				scratchBitBoard &= potentialAttackersMask;
				while ( scratchBitBoard != 0x0L ) {
					int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
					int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
					Piece.king_generateMoves(ml, this, atSquare, ownSideIsWhite);
					scratchBitBoard &= scratchBitBoard-1L;
				}
			}
		}
	}
	
	public PiecewiseEvaluation evaluateMaterial() {
		if (ENABLE_PIECE_LISTS) {
			me = new PiecewiseEvaluation();
			pieceLists.evaluateMaterialBalanceAndPieceMobility(true);
			pieceLists.evaluateMaterialBalanceAndPieceMobility(false);
			return me;
		} else {
			PrimitiveIterator.OfInt iter_p = this.iterator();
			PiecewiseEvaluation material = new PiecewiseEvaluation();
			while ( iter_p.hasNext() ) {
				int atPos = iter_p.nextInt();
				int currPiece = getPieceAtSquare(atPos);
				material = updateMaterialForPiece(currPiece, atPos, material);
			}
			me = material;
			return me;
		}
	}
	
    // For reasons of performance optimisation, part of the material evaluation considers the mobility of pieces.
    // This function generates a score considering three categories A) material B) static PSTs C) Piece mobility (dynamic) 
	@SuppressWarnings("unused")
	private PiecewiseEvaluation updateMaterialForPiece(int currPiece, int atPos, PiecewiseEvaluation eval) {
		switch(currPiece) {
		case Piece.WHITE_PAWN:
			eval.addPiece(true, Piece.PAWN);
			eval.addPosition(true, PAWN_WHITE_WEIGHTINGS[atPos]);
			break;
		case Piece.BLACK_PAWN:
			eval.addPiece(false, Piece.PAWN);
			eval.addPosition(false, PAWN_BLACK_WEIGHTINGS[atPos]);
			break;
		case Piece.WHITE_ROOK:
			eval.addPiece(true, Piece.ROOK);
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !isEndgame)
				eval.addPosition(true, getTwiceNumEmptyRankFileSquares(atPos));
			break;
		case Piece.BLACK_ROOK:
			eval.addPiece(false, Piece.ROOK);
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !isEndgame)
				eval.addPosition(false, getTwiceNumEmptyRankFileSquares(atPos));
			break;
		case Piece.WHITE_BISHOP:
			eval.addPiece(true, Piece.BISHOP);
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !isEndgame)
				eval.addPosition(true, getTwiceNumEmptyDiagonalSquares(atPos));
			break;
		case Piece.BLACK_BISHOP:
			eval.addPiece(false, Piece.BISHOP);
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !isEndgame)
				eval.addPosition(false, getTwiceNumEmptyDiagonalSquares(atPos));
			break;
		case Piece.WHITE_KNIGHT:
			eval.addPiece(true, Piece.KNIGHT);
			eval.addPosition(true, KNIGHT_WEIGHTINGS[atPos]);
			break;
		case Piece.BLACK_KNIGHT:
			eval.addPiece(false, Piece.KNIGHT);
			eval.addPosition(false, KNIGHT_WEIGHTINGS[atPos]);
			break;
		case Piece.WHITE_QUEEN:
			eval.addPiece(true, Piece.QUEEN);
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !isEndgame)
				eval.addPosition(true, getTwiceNumEmptyAllDirectSquares(atPos));
			break;
		case Piece.BLACK_QUEEN:
			eval.addPiece(false, Piece.QUEEN);
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !isEndgame)
				eval.addPosition(false, getTwiceNumEmptyAllDirectSquares(atPos));
			break;
		case Piece.WHITE_KING:
			eval.addPiece(true, Piece.KING);
			eval.addPosition(true, (isEndgame) ? KING_ENDGAME_WEIGHTINGS[atPos] : KING_MIDGAME_WEIGHTINGS[atPos]);
			break;			
		case Piece.BLACK_KING:
			eval.addPiece(false, Piece.KING);
			eval.addPosition(false, (isEndgame) ? KING_ENDGAME_WEIGHTINGS[atPos] : KING_MIDGAME_WEIGHTINGS[atPos]);
			break;
		default:
			break;
		}
		return eval;
	}
	
	public boolean isInsufficientMaterial() {
		// Major pieces
		if (pieces[Piece.QUEEN] != 0)
			return false;
		if (pieces[Piece.ROOK] != 0)
			return false;
		// Possible promotions
		if (pieces[Piece.PAWN] != 0)
			return false;
		
		// Minor pieces
		int numWhiteBishops = Long.bitCount(pieces[Piece.BISHOP] & whitePieces);
		int numWhiteKnights = Long.bitCount(pieces[Piece.KNIGHT] & whitePieces);
		int numBlackBishops = Long.bitCount(pieces[Piece.BISHOP] & blackPieces);
		int numBlackKnights = Long.bitCount(pieces[Piece.KNIGHT] & blackPieces);
		
		if (numWhiteBishops >= 2 || numBlackBishops >= 2) {
			// One side has at least two bishops
			return false;
		}
		if ((numWhiteBishops == 1 && numWhiteKnights >= 1) ||
		    (numBlackBishops == 1 && numBlackKnights >= 1))
			// One side has Knight and Bishop
			return false;
		
		// else insufficient
		return true;
	}
	
	public boolean isInsufficientMaterial(Piece.Colour side) {
		long ownBitBoard =  Colour.isWhite(side) ? whitePieces : blackPieces;
		// Major pieces
		if ((pieces[Piece.QUEEN] & ownBitBoard) != 0)
			return false;
		if ((pieces[Piece.ROOK] & ownBitBoard) != 0)
			return false;
		// Possible promotions
		if ((pieces[Piece.PAWN] & ownBitBoard) != 0)
			return false;
		
		// Minor pieces
		int numBishops = Long.bitCount((pieces[Piece.BISHOP] & ownBitBoard));
		int numKnights = Long.bitCount((pieces[Piece.KNIGHT] & ownBitBoard));
		
		if (numBishops >= 2) {
			// side has at least two bishops
			return false;
		}
		if (numBishops == 1 && numKnights >= 1)
			// side has Knight and Bishop
			return false;
		
		// else insufficient
		return true;
	}
	
	public int evaluateKingSafety(Piece.Colour side) {
		int evaluation = 0;
		boolean isWhite = Piece.Colour.isWhite(side);
		if (!isEndgame) {
			// King
			long kingMask = isWhite ? getWhiteKing() : getBlackKing();
			boolean isKingOnDarkSq = (kingMask & DARK_SQUARES_MASK) != 0;
			// Attackers
			long attackingQueensMask = isWhite ? getBlackQueens() : getWhiteQueens();
			long attackingRooksMask = isWhite ? getBlackRooks() : getWhiteRooks();
			long attackingBishopsMask = isWhite ? getBlackBishops() : getWhiteBishops();
			long attackingKnightsMask = isWhite ? getBlackKnights() : getWhiteKnights();
			// create masks of attackers
			long pertinentBishopMask = attackingBishopsMask & ((isKingOnDarkSq) ? DARK_SQUARES_MASK : LIGHT_SQUARES_MASK);
			long diagonalAttackersMask = attackingQueensMask | pertinentBishopMask;
			long rankFileAttackersMask = attackingQueensMask | attackingRooksMask;
			
			// First score according to King exposure on open diagonals
			int numPotentialAttackers = Long.bitCount(diagonalAttackersMask);
			int kingPos = Position.NOPOSITION;
			if (ENABLE_PIECE_LISTS) {
				kingPos = pieceLists.getKingPos(isWhite);
			} else {
				kingPos = BitBoard.bitToPosition_Lut[Long.numberOfTrailingZeros(kingMask)];
			}
			evaluation = (getKingSafetyEvaluationDiagonalSquares(isWhite, kingPos)) * -numPotentialAttackers;
			// Then score according to King exposure on open rank/files
			numPotentialAttackers = Long.bitCount(rankFileAttackersMask);
			evaluation += (getKingSafetyEvaluationRankFileSquares(isWhite, kingPos)) * -numPotentialAttackers;
			// Then account for Knight proximity to the adjacent square around the King
			long pertintentKnightsMask = attackingKnightsMask & knightKingSafetyMask_Lut[kingPos];
			evaluation += -8*Long.bitCount(pertintentKnightsMask);
			
		}
		return evaluation;
	}
	
	public byte getTwiceNumEmptyDiagonalSquares(int atPos) {
		return getTwiceNumEmptySquaresInDirection(atPos, SquareAttackEvaluator.diagonals);
	}
	
	public byte getTwiceNumEmptyRankFileSquares(int atPos) {
		return getTwiceNumEmptySquaresInDirection(atPos, SquareAttackEvaluator.rankFile);
	}
	
	public byte getTwiceNumEmptyAllDirectSquares(int atPos) {
		return getTwiceNumEmptySquaresInDirection(atPos, SquareAttackEvaluator.allDirect);
	}
	
	public byte getKingSafetyEvaluationDiagonalSquares(boolean whiteOnMove, int atPos) {
		long defenders = (whiteOnMove) ? getWhiteBishops() : getBlackBishops();
		long blockers = (whiteOnMove) ? getWhitePawns()/*|getWhiteKnights()*/ : getBlackPawns()/*|getBlackKnights()*/;
		return getKingSafetyEvaluation(defenders, blockers, atPos, SquareAttackEvaluator.diagonals);
	}
	
	public byte getKingSafetyEvaluationRankFileSquares(boolean whiteOnMove, int atPos) {
		long defenders = (whiteOnMove) ? getWhiteRooks() : getBlackRooks();
		long blockers = (whiteOnMove) ? getWhitePawns()/*|getWhiteKnights()*/ : getBlackPawns()/*|getBlackKnights()*/;
		return getKingSafetyEvaluation(defenders, blockers, atPos, SquareAttackEvaluator.rankFile);
	}
		
	static final long[][][] emptySquareMask_Lut = new long[128][SquareAttackEvaluator.allDirect.length][];
	static {
		for (int square : Position.values) {
			int [][] forSqArray = SquareAttackEvaluator.directPieceMove_Lut[square];
			int j=0;
			for (int[] dir : forSqArray) {
				long [] mask = new long[dir.length];
				int i=0;
				for (int sq : dir) {
					mask[i++] = BitBoard.positionToMask_Lut[sq];
				}
				emptySquareMask_Lut[square][j++] = mask;
			}
		}
	}
	
	private byte getKingSafetyEvaluation(long defenderMask, long blockerMask, int atPos, Direction [] dirs) {
		byte numSquares = 0; 
		//long ownPieces = defenderMask | blockerMask;
		// One dimension for each direction, other dimension is array of individual square masks in that direction
		long [][] emptySqMaskArray = emptySquareMask_Lut[atPos]; 
		for (Direction dir: dirs) { 
			int directionIndex = SquareAttackEvaluator.directionIndex_Lut.get(dir);
			long inPathMask = SquareAttackEvaluator.directAttacksOnPositionAll_Lut[directionIndex][atPos];
			if (inPathMask != 0) {
				if ((defenderMask & inPathMask) != 0) {
					// If there is a defender, assume this direction is safe
					break;
				}
				if ((blockerMask & inPathMask) != 0) {
					// Count the empty squares to the blocker
					for (long mask: emptySqMaskArray[directionIndex]) {
						if ((blockerMask & mask) == 0) {
							numSquares+=2;
						} else break;
					}
				} else {
					// All the squares are empty in this direction
					numSquares += (emptySqMaskArray[directionIndex].length*2);
				}
			} else {
				// This is a square on the edge of the board from which that direction is off the board
			}
		}
		return numSquares;
	}
	
	private byte getTwiceNumEmptySquaresInDirection(int atPos, Direction [] dirs) {
		byte numSquares = 0;
		// One dimension for each direction, other dimension is array of individual square masks in that direction
		long [][] emptySqMaskArray = emptySquareMask_Lut[atPos]; 
		for (Direction dir: dirs) { 
			int directionIndex = SquareAttackEvaluator.directionIndex_Lut.get(dir);
			long inPathMask = SquareAttackEvaluator.directAttacksOnPositionAll_Lut[directionIndex][atPos];
			if (inPathMask != 0) {
				if ((allPieces & inPathMask) != 0) {
					// Count the empty squares
					for (long mask: emptySqMaskArray[directionIndex]) {
						if ((allPieces & mask) == 0) {
							numSquares+=2;
						} else break;
					}
				} else {
					// All the squares are empty in this direction
					numSquares += (emptySqMaskArray[directionIndex].length*2);
				}
			} else {
				 // This is a square on the edge of the board from which that direction is off the board
			}
		}
		return numSquares;
	}
	
	public void forEachPiece(IForEachPieceCallback caller) {
		if (ENABLE_PIECE_LISTS) {
			pieceLists.forEachPieceDoCallback(caller);
		} else {
			long scratchBitBoard = allPieces;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				caller.callback(getPieceAtSquare(atSquare), atSquare);
				scratchBitBoard &= scratchBitBoard-1L;
			}
		}
	}
	
	public void forEachPawnOfSide(IForEachPieceCallback caller, boolean isBlack) {
		if (ENABLE_PIECE_LISTS) {
			pieceLists.forEachPawnOfSideDoCallback(caller, isBlack);
		} else {
			long pawnMask = isBlack ? getBlackPawns() : getWhitePawns();
			int piece = isBlack ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
			long scratchBitBoard = pawnMask;
			while ( scratchBitBoard != 0x0L ) {
				int bitIndex = Long.numberOfTrailingZeros(scratchBitBoard);
				int atSquare = BitBoard.bitToPosition_Lut[bitIndex];
				caller.callback(piece, atSquare);
				scratchBitBoard &= scratchBitBoard-1L;
			}
		}
	}
}
