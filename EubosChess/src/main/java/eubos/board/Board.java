package eubos.board;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.CastlingManager;
import eubos.position.Move;
import eubos.position.Position;
import eubos.score.PiecewiseEvaluation;

import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntRank;

public class Board {
	
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
	
	public static final short MATERIAL_VALUE_KING = 4000;
	public static final short MATERIAL_VALUE_QUEEN = 900;
	public static final short MATERIAL_VALUE_ROOK = 490;
	public static final short MATERIAL_VALUE_BISHOP = 320;
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
        PAWN_WHITE_WEIGHTINGS[Position.a4] = 0; PAWN_WHITE_WEIGHTINGS[Position.b4] = 0; PAWN_WHITE_WEIGHTINGS[Position.c4] = 5; PAWN_WHITE_WEIGHTINGS[Position.d4] = 10; PAWN_WHITE_WEIGHTINGS[Position.e4] = 10;PAWN_WHITE_WEIGHTINGS[Position.f4] = 5; PAWN_WHITE_WEIGHTINGS[Position.g4] = 0; PAWN_WHITE_WEIGHTINGS[Position.h4] = 0;
        PAWN_WHITE_WEIGHTINGS[Position.a5] = 0; PAWN_WHITE_WEIGHTINGS[Position.b5] = 3; PAWN_WHITE_WEIGHTINGS[Position.c5] = 5; PAWN_WHITE_WEIGHTINGS[Position.d5] = 15; PAWN_WHITE_WEIGHTINGS[Position.e5] = 15; PAWN_WHITE_WEIGHTINGS[Position.f5] = 5; PAWN_WHITE_WEIGHTINGS[Position.g5] = 3; PAWN_WHITE_WEIGHTINGS[Position.h5] = 0;
		PAWN_WHITE_WEIGHTINGS[Position.a6] = 5; PAWN_WHITE_WEIGHTINGS[Position.b6] = 25; PAWN_WHITE_WEIGHTINGS[Position.c6] = 25; PAWN_WHITE_WEIGHTINGS[Position.d6] = 25; PAWN_WHITE_WEIGHTINGS[Position.e6] = 25; PAWN_WHITE_WEIGHTINGS[Position.f6] = 25; PAWN_WHITE_WEIGHTINGS[Position.g6] = 25; PAWN_WHITE_WEIGHTINGS[Position.h6] = 10;
		PAWN_WHITE_WEIGHTINGS[Position.a7] = 25; PAWN_WHITE_WEIGHTINGS[Position.b7] = 50; PAWN_WHITE_WEIGHTINGS[Position.c7] = 50; PAWN_WHITE_WEIGHTINGS[Position.d7] = 50; PAWN_WHITE_WEIGHTINGS[Position.e7] = 50; PAWN_WHITE_WEIGHTINGS[Position.f7] = 50; PAWN_WHITE_WEIGHTINGS[Position.g7] = 50; PAWN_WHITE_WEIGHTINGS[Position.h7] = 25;
		PAWN_WHITE_WEIGHTINGS[Position.a8] = 0; PAWN_WHITE_WEIGHTINGS[Position.b8] = 0; PAWN_WHITE_WEIGHTINGS[Position.c8] = 0; PAWN_WHITE_WEIGHTINGS[Position.d8] = 0; PAWN_WHITE_WEIGHTINGS[Position.e8] = 0; PAWN_WHITE_WEIGHTINGS[Position.f8] = 0; PAWN_WHITE_WEIGHTINGS[Position.g8] = 0; PAWN_WHITE_WEIGHTINGS[Position.h8] = 0;
    }
    
	static final byte[] PAWN_BLACK_WEIGHTINGS;
    static {
    	PAWN_BLACK_WEIGHTINGS = new byte[128];
        PAWN_BLACK_WEIGHTINGS[Position.a1] = 0; PAWN_BLACK_WEIGHTINGS[Position.b1] = 0; PAWN_BLACK_WEIGHTINGS[Position.c1] = 0; PAWN_BLACK_WEIGHTINGS[Position.d1] = 0; PAWN_BLACK_WEIGHTINGS[Position.e1] = 0; PAWN_BLACK_WEIGHTINGS[Position.f1] = 0; PAWN_BLACK_WEIGHTINGS[Position.g1] = 0; PAWN_BLACK_WEIGHTINGS[Position.h1] = 0;
        PAWN_BLACK_WEIGHTINGS[Position.a2] = 25; PAWN_BLACK_WEIGHTINGS[Position.b2] = 50; PAWN_BLACK_WEIGHTINGS[Position.c2] = 50; PAWN_BLACK_WEIGHTINGS[Position.d2] = 50;PAWN_BLACK_WEIGHTINGS[Position.e2] = 50;PAWN_BLACK_WEIGHTINGS[Position.f2] = 50; PAWN_BLACK_WEIGHTINGS[Position.g2] = 50; PAWN_BLACK_WEIGHTINGS[Position.h2] = 25;
        PAWN_BLACK_WEIGHTINGS[Position.a3] = 5; PAWN_BLACK_WEIGHTINGS[Position.b3] = 25; PAWN_BLACK_WEIGHTINGS[Position.c3] = 25; PAWN_BLACK_WEIGHTINGS[Position.d3] = 25;PAWN_BLACK_WEIGHTINGS[Position.e3] = 25;PAWN_BLACK_WEIGHTINGS[Position.f3] = 25; PAWN_BLACK_WEIGHTINGS[Position.g3] = 25; PAWN_BLACK_WEIGHTINGS[Position.h3] = 10;
        PAWN_BLACK_WEIGHTINGS[Position.a4] = 0; PAWN_BLACK_WEIGHTINGS[Position.b4] = 3; PAWN_BLACK_WEIGHTINGS[Position.c4] = 5; PAWN_BLACK_WEIGHTINGS[Position.d4] = 15;PAWN_BLACK_WEIGHTINGS[Position.e4] = 15;PAWN_BLACK_WEIGHTINGS[Position.f4] = 5;PAWN_BLACK_WEIGHTINGS[Position.g4] = 3; PAWN_BLACK_WEIGHTINGS[Position.h4] = 0;
        PAWN_BLACK_WEIGHTINGS[Position.a5] = 0; PAWN_BLACK_WEIGHTINGS[Position.b5] = 0; PAWN_BLACK_WEIGHTINGS[Position.c5] = 5; PAWN_BLACK_WEIGHTINGS[Position.d5] = 10;PAWN_BLACK_WEIGHTINGS[Position.e5] = 10;PAWN_BLACK_WEIGHTINGS[Position.f5] = 5;PAWN_BLACK_WEIGHTINGS[Position.g5] = 0; PAWN_BLACK_WEIGHTINGS[Position.h5] = 0;
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
        KING_MIDGAME_WEIGHTINGS[Position.a1] = 5;KING_MIDGAME_WEIGHTINGS[Position.b1] = 10;KING_MIDGAME_WEIGHTINGS[Position.c1] = 5;KING_MIDGAME_WEIGHTINGS[Position.d1] = 0;KING_MIDGAME_WEIGHTINGS[Position.e1] = 0;KING_MIDGAME_WEIGHTINGS[Position.f1] = 5;KING_MIDGAME_WEIGHTINGS[Position.g1] = 10;KING_MIDGAME_WEIGHTINGS[Position.h1] = 5;
		KING_MIDGAME_WEIGHTINGS[Position.a2] = 0;KING_MIDGAME_WEIGHTINGS[Position.b2] = 0;KING_MIDGAME_WEIGHTINGS[Position.c2] = 0;KING_MIDGAME_WEIGHTINGS[Position.d2] = 0;KING_MIDGAME_WEIGHTINGS[Position.e2] = 0;KING_MIDGAME_WEIGHTINGS[Position.f2] = 0;KING_MIDGAME_WEIGHTINGS[Position.g2] = 0;KING_MIDGAME_WEIGHTINGS[Position.h2] = 0;
		KING_MIDGAME_WEIGHTINGS[Position.a3] = -20;KING_MIDGAME_WEIGHTINGS[Position.b3] = -20;KING_MIDGAME_WEIGHTINGS[Position.c3] = -30;KING_MIDGAME_WEIGHTINGS[Position.d3] = -30;KING_MIDGAME_WEIGHTINGS[Position.e3] = -30;KING_MIDGAME_WEIGHTINGS[Position.f3] = -30;KING_MIDGAME_WEIGHTINGS[Position.g3] = -20;KING_MIDGAME_WEIGHTINGS[Position.h3] = -20;
		KING_MIDGAME_WEIGHTINGS[Position.a4] = -30;KING_MIDGAME_WEIGHTINGS[Position.b4] = -40;KING_MIDGAME_WEIGHTINGS[Position.c4] = -50;KING_MIDGAME_WEIGHTINGS[Position.d4] = -50;KING_MIDGAME_WEIGHTINGS[Position.e4] = -50;KING_MIDGAME_WEIGHTINGS[Position.f4] = -40;KING_MIDGAME_WEIGHTINGS[Position.g4] = -40;KING_MIDGAME_WEIGHTINGS[Position.h4] = -30;
		KING_MIDGAME_WEIGHTINGS[Position.a5] = -30;KING_MIDGAME_WEIGHTINGS[Position.b5] = -40;KING_MIDGAME_WEIGHTINGS[Position.c5] = -50;KING_MIDGAME_WEIGHTINGS[Position.d5] = -50;KING_MIDGAME_WEIGHTINGS[Position.e5] = -50;KING_MIDGAME_WEIGHTINGS[Position.f5] = -40;KING_MIDGAME_WEIGHTINGS[Position.g5] = -40;KING_MIDGAME_WEIGHTINGS[Position.h5] = -30;
		KING_MIDGAME_WEIGHTINGS[Position.a6] = -20;KING_MIDGAME_WEIGHTINGS[Position.b6] = -20;KING_MIDGAME_WEIGHTINGS[Position.c6] = -30;KING_MIDGAME_WEIGHTINGS[Position.d6] = -30;KING_MIDGAME_WEIGHTINGS[Position.e6] = -30;KING_MIDGAME_WEIGHTINGS[Position.f6] = -30;KING_MIDGAME_WEIGHTINGS[Position.g6] = -20;KING_MIDGAME_WEIGHTINGS[Position.h6] = -20;
		KING_MIDGAME_WEIGHTINGS[Position.a7] = 0;KING_MIDGAME_WEIGHTINGS[Position.b7] = 0;KING_MIDGAME_WEIGHTINGS[Position.c7] = 0;KING_MIDGAME_WEIGHTINGS[Position.d7] = 0;KING_MIDGAME_WEIGHTINGS[Position.e7] = 0;KING_MIDGAME_WEIGHTINGS[Position.f7] = 0;KING_MIDGAME_WEIGHTINGS[Position.g7] = 0;KING_MIDGAME_WEIGHTINGS[Position.h7] = 0;
		KING_MIDGAME_WEIGHTINGS[Position.a8] = 5;KING_MIDGAME_WEIGHTINGS[Position.b8] = 10;KING_MIDGAME_WEIGHTINGS[Position.c8] = 5;KING_MIDGAME_WEIGHTINGS[Position.d8] = 0;KING_MIDGAME_WEIGHTINGS[Position.e8] = 0;KING_MIDGAME_WEIGHTINGS[Position.f8] = 5;KING_MIDGAME_WEIGHTINGS[Position.g8] = 10;KING_MIDGAME_WEIGHTINGS[Position.h8] = 5;
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
	
	public int doMove(int move) throws InvalidPieceException {
		int capturePosition = Position.NOPOSITION;
		int pieceToMove = Move.getOriginPiece(move);
		int originSquare = Move.getOriginPosition(move);
		int targetSquare = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
		int promotedPiece = Move.getPromotion(move);
		long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
		long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
		long positionsMask = initialSquareMask | targetSquareMask;
		
		// Initialise En Passant target square
		setEnPassantTargetSq(Position.NOPOSITION);
		
		if (Move.isEnPassantCapture(move)) {
			// Handle en passant captures, don't need to do other checks in this case
			capturePosition = generateCapturePositionForEnPassant(pieceToMove, targetSquare);
			pickUpPieceAtSquare(capturePosition, targetPiece);
		} else {
			// Handle castling, setting en passant etc
			if (!moveEnablesEnPassantCapture(pieceToMove, originSquare, targetSquare)) {
				// Handle castling secondary rook moves...
				if (Piece.isKing(pieceToMove)) {
					performSecondaryCastlingMove(move);
				}
				if (targetPiece != Piece.NONE) {
					capturePosition = targetSquare;
					pickUpPieceAtSquare(targetSquare, targetPiece);
				}
			}			
		}
		// Switch piece-specific bitboards and piece lists
		if (promotedPiece != Piece.NONE) {
			// For a promotion, need to resolve piece-specific across multiple bitboards
			pieces[INDEX_PAWN] &= ~initialSquareMask;
			pieces[promotedPiece & Piece.PIECE_NO_COLOUR_MASK] |= targetSquareMask;
			pieceLists.removePiece(pieceToMove, originSquare);
			pieceLists.addPiece((promotedPiece | (pieceToMove & ~Piece.PIECE_NO_COLOUR_MASK)), targetSquare);
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] ^= positionsMask;
			pieceLists.updatePiece(pieceToMove, originSquare, targetSquare);
		}
		// Switch colour bitboard
		if (Piece.isWhite(pieceToMove)) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
		
		return capturePosition;
	}
	
	public int undoMove(int moveToUndo) throws InvalidPieceException {
		int capturedPieceSquare = Position.NOPOSITION;
		int originPiece = Move.getOriginPiece(moveToUndo);
		int originSquare = Move.getOriginPosition(moveToUndo);
		int targetSquare = Move.getTargetPosition(moveToUndo);
		int targetPiece = Move.getTargetPiece(moveToUndo);
		int promotedPiece = Move.getPromotion(moveToUndo);
		long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
		long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
		long positionsMask = initialSquareMask | targetSquareMask;
		boolean isCapture = targetPiece != Piece.NONE;
		
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
			pieceLists.removePiece((promotedPiece | (originPiece & ~Piece.PIECE_NO_COLOUR_MASK)), originSquare);
			pieceLists.addPiece(originPiece, targetSquare);
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & originPiece] ^= positionsMask;
			pieceLists.updatePiece(originPiece, originSquare, targetSquare);
		}
		// Switch colour bitboard
		if (Piece.isWhite(originPiece)) {
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
		if (originPiece == Piece.WHITE_PAWN) {
			if ( Position.getRank(originSquare) == IntRank.R2) {
				if (Position.getRank(targetSquare) == IntRank.R4) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetSquare-16);
				}
			}
		} else if (originPiece == Piece.BLACK_PAWN) {
			if (Position.getRank(originSquare) == IntRank.R7) {
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
	
	private void performSecondaryCastlingMove(int move) throws InvalidPieceException {
		if (Move.areEqual(move, CastlingManager.wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.h1, Position.f1);
		} else if (Move.areEqual(move, CastlingManager.wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.a1, Position.d1);
		} else if (Move.areEqual(move, CastlingManager.bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.h8, Position.f8);
		} else if (Move.areEqual(move, CastlingManager.bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.a8, Position.d8);
		}
	}
	
	private void unperformSecondaryCastlingMove(int move) throws InvalidPieceException {
		if (Move.areEqual(move, CastlingManager.undo_wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.f1, Position.h1);
		} else if (Move.areEqual(move, CastlingManager.undo_wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.d1, Position.a1);
		} else if (Move.areEqual(move, CastlingManager.undo_bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.f8, Position.h8);
		} else if (Move.areEqual(move, CastlingManager.undo_bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.d8, Position.a8);
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
				if (EubosEngineMain.ASSERTS_ENABLED)
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
				if (EubosEngineMain.ASSERTS_ENABLED)
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
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert atPos != Position.NOPOSITION;
			assert pieceToPlace != Piece.NONE;
		}
		pieceLists.addPiece(pieceToPlace, atPos);
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
		int kingSquare = pieceLists.getKingPos(Colour.isWhite(side));
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
				if (EubosEngineMain.ASSERTS_ENABLED)
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
			pieceLists.removePiece(type, atPos);
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
			pieceLists.removePiece(piece, atPos);
		} else {
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
		int kingPosition = pieceLists.getKingPos(Piece.isWhite(piece));
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
	
	public List<Integer> getRegularPieceMoves(boolean ownSideIsWhite) {
		List<Integer> movesList = new LinkedList<Integer>();
		pieceLists.addMoves(ownSideIsWhite, movesList);
		return movesList;
	}
	
	public PiecewiseEvaluation evaluateMaterial() {
		me = new PiecewiseEvaluation();
		pieceLists.evaluateMaterialBalanceAndPieceMobility(true);
		pieceLists.evaluateMaterialBalanceAndPieceMobility(false);
		return me;
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
	
	public int evaluateKingSafety(boolean onMoveWasWhite) {
		int evaluation = 0;
		if (!isEndgame) {
			// King
			long kingMask = onMoveWasWhite ? getWhiteKing() : getBlackKing();
			boolean isKingOnDarkSq = (kingMask & DARK_SQUARES_MASK) != 0;
			// Attackers
			long attackingQueensMask = onMoveWasWhite ? getBlackQueens() : getWhiteQueens();
			long attackingRooksMask = onMoveWasWhite ? getBlackRooks() : getWhiteRooks();
			long attackingBishopsMask = onMoveWasWhite ? getBlackBishops() : getWhiteBishops();
			// create masks of attackers
			long pertinentBishopMask = attackingBishopsMask & ((isKingOnDarkSq) ? DARK_SQUARES_MASK : LIGHT_SQUARES_MASK);
			long diagonalAttackersMask = attackingQueensMask | pertinentBishopMask;
			long rankFileAttackersMask = attackingQueensMask | attackingRooksMask;
			
			// First score according to King exposure on open diagonals
			int numPotentialAttackers = Long.bitCount(diagonalAttackersMask);
			int kingPos = pieceLists.getKingPos(onMoveWasWhite);
			evaluation = getNumEmptyDiagonalSquares(kingPos) * -numPotentialAttackers;
			
			numPotentialAttackers = Long.bitCount(rankFileAttackersMask);
			evaluation += getNumEmptyRankFileSquares(kingPos) * -numPotentialAttackers;
			
			if (!onMoveWasWhite) {
				evaluation = -evaluation;
			}
		}
		return evaluation;
	}
	
	public byte getNumEmptyDiagonalSquares(int atPos) {
		return getNumEmptySquaresInDirection(atPos, SquareAttackEvaluator.diagonals);
	}
	
	public byte getNumEmptyRankFileSquares(int atPos) {
		return getNumEmptySquaresInDirection(atPos, SquareAttackEvaluator.rankFile);
	}
	
	public byte getNumEmptyAllDirectSquares(int atPos) {
		return getNumEmptySquaresInDirection(atPos, SquareAttackEvaluator.allDirect);
	}
	
	private byte getNumEmptySquaresInDirection(int atPos, Direction [] dirs) {
		byte numSquares = 0;
		int [][] array = SquareAttackEvaluator.directPieceMove_Lut[atPos]; 
		for (Direction dir: dirs) { 
			long inPathMask = 0;
			switch(dir) {
			case downLeft:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionDownLeft_Lut[atPos];
				break;
			case upLeft:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionUpLeft_Lut[atPos];
				break;
			case upRight:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionUpRight_Lut[atPos];
				break;
			case downRight:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionDownRight_Lut[atPos];
				break;
			case left:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionLeft_Lut[atPos];
				break;
			case up:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionUp_Lut[atPos];
				break;
			case right:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionRight_Lut[atPos];
				break;
			case down:
				inPathMask = SquareAttackEvaluator.directAttacksOnPositionDown_Lut[atPos];
				break;	
			default:
				break;		
			}
			if (inPathMask == 0) {
				// skip if nothing to do for this direction
				continue;
			}
			// one dimension for each direction, other dimension is array of squares in that direction
			if ((allPieces & inPathMask) != 0) {
				for (int attackerSq: array[SquareAttackEvaluator.directionIndex_Lut.get(dir)]) {
					if (squareIsEmpty(attackerSq)) {
						numSquares++;
					} else break;
				}
			} else {
				// all the squares are empty in this direction
				numSquares += array[SquareAttackEvaluator.directionIndex_Lut.get(dir)].length;
			}
		}
		return numSquares;
	}
	
	public void forEachPiece(IForEachPieceCallback caller) {
		pieceLists.forEachPieceDoCallback(caller);
	}
	
	public void forEachPawnOfSide(IForEachPieceCallback caller, boolean isBlack) {
		pieceLists.forEachPawnOfSideDoCallback(caller, isBlack);
	}
}
