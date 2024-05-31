package eubos.score;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;

public class PiecewiseEvaluation {
	
	public static final int PIECE_PHASE = 192;
	public static final int ROOK_PHASE = 320;
	public static final int QUEEN_PHASE = 640;
	public static final int TOTAL_PHASE = PIECE_PHASE * 8 + ROOK_PHASE * 4 + QUEEN_PHASE * 2;
	
	public short mg_material = 0;
	public short eg_material = 0;
	public short dynamicPosition = 0;
	public int mg_position = 0;
	public int eg_position = 0;
	
	public int phase = 0;
	public int [] numberOfPieces;
	
	public PiecewiseEvaluation() {
		numberOfPieces = new int [16];
	}
	
	public boolean isEndgame() {
		return phase > 3000;
	}
	
	public short getMiddleGameDelta() { return mg_material; }
	
	public short getEndGameDelta() { return eg_material; }
	
	public short getPosition() { return (short)(mg_position + dynamicPosition); }
	
	public short getEndgamePosition() { return (short)(eg_position + dynamicPosition); }

	public int getPhase() {
		return phase;
	}
	
	public void setPhase() {
		// Phase calculation; pawns are excluded, king is excluded
		phase = TOTAL_PHASE;
		phase -= numberOfPieces[Piece.WHITE_KNIGHT] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.WHITE_BISHOP] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.WHITE_ROOK] * ROOK_PHASE;
		phase -= numberOfPieces[Piece.WHITE_QUEEN] * QUEEN_PHASE;
		phase -= numberOfPieces[Piece.BLACK_KNIGHT] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.BLACK_BISHOP] * PIECE_PHASE;
		phase -= numberOfPieces[Piece.BLACK_ROOK] * ROOK_PHASE;
		phase -= numberOfPieces[Piece.BLACK_QUEEN] * QUEEN_PHASE;
		// Phase is now a 10 bit fixed point fraction of the total phase
	}

	public void addPst(int piece, int bitOffset) {
//		int x = combinedPosition;
//		int y = Piece.COMBINED_PIECE_SQUARE_TABLES[piece][bitOffset];
//		int s = x + y;
//		int c = (s ^ x ^ y) & 0x0001_0000;
//		combinedPosition = s - c;
		
//		mg_position += Piece.PIECE_SQUARE_TABLES[piece][0][bitOffset];
//		eg_position += Piece.PIECE_SQUARE_TABLES[piece][1][bitOffset];
		
		mg_position += Piece.COMBINED_PIECE_SQUARE_TABLES[piece][bitOffset] & 0xFFFF;
		eg_position += Piece.COMBINED_PIECE_SQUARE_TABLES[piece][bitOffset] >> 16;
	}
	
	public void subtractPst(int piece, int bitOffset) {
//		int x = combinedPosition;
//		int y = Piece.COMBINED_PIECE_SQUARE_TABLES[piece][bitOffset];
//		int d = x - y;
//		int b = (d ^ x ^ y) & 0x0001_0000;
//		combinedPosition = d + b;
		
//		mg_position -= Piece.PIECE_SQUARE_TABLES[piece][0][bitOffset];
//		eg_position -= Piece.PIECE_SQUARE_TABLES[piece][1][bitOffset];
		
		mg_position -= Piece.COMBINED_PIECE_SQUARE_TABLES[piece][bitOffset] & 0xFFFF;
		eg_position -= Piece.COMBINED_PIECE_SQUARE_TABLES[piece][bitOffset] >> 16;
	}
	
	public void updateRegular(int pieceTypeWithoutColour, int originPiece, int originBitOffset, int targetBitOffset) {
		if (EubosEngineMain.ENABLE_ASSERTS) assert (pieceTypeWithoutColour & Piece.BLACK) == 0;
		if (pieceTypeWithoutColour >= Piece.KNIGHT) {
			addPst(originPiece, targetBitOffset);
			subtractPst(originPiece, originBitOffset);
		}
	}
	
	public void updateWhenUndoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		int pawnToReplace = (promoPiece & Piece.BLACK)+Piece.PAWN;
		numberOfPieces[pawnToReplace]++;
		numberOfPieces[promoPiece]--;
		
		mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][pawnToReplace];
		mg_material -= Piece.PIECE_TO_MATERIAL_LUT[0][promoPiece];
		eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][pawnToReplace];
		eg_material -= Piece.PIECE_TO_MATERIAL_LUT[1][promoPiece];
		phase += Piece.PIECE_PHASE[promoPiece];
		
		addPst(pawnToReplace, newBitOffset);
		int pieceType = promoPiece & Piece.PIECE_NO_COLOUR_MASK;
		if (pieceType >= Piece.KNIGHT) {
			subtractPst(promoPiece, oldBitOffset);
		}
	}
	
	public void updateWhenDoingPromotion(int promoPiece, int oldBitOffset, int newBitOffset) {
		int pawnToRemove = (promoPiece & Piece.BLACK)+Piece.PAWN;
		numberOfPieces[pawnToRemove]--;
		numberOfPieces[promoPiece]++;
		
		mg_material -= Piece.PIECE_TO_MATERIAL_LUT[0][pawnToRemove];
		mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][promoPiece];
		eg_material -= Piece.PIECE_TO_MATERIAL_LUT[1][pawnToRemove];
		eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][promoPiece];
		phase -= Piece.PIECE_PHASE[promoPiece];
		
		subtractPst(pawnToRemove, oldBitOffset);
		int pieceType = promoPiece & Piece.PIECE_NO_COLOUR_MASK;
		if (pieceType >= Piece.KNIGHT) {
			addPst(promoPiece, newBitOffset);
		}
	}
	
	public void updateForCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]--;
		mg_material -= Piece.PIECE_TO_MATERIAL_LUT[0][currPiece];
		eg_material -= Piece.PIECE_TO_MATERIAL_LUT[1][currPiece];
		phase += Piece.PIECE_PHASE[currPiece];
		int pieceType = currPiece & Piece.PIECE_NO_COLOUR_MASK;
		if (pieceType >= Piece.KNIGHT) {
			subtractPst(currPiece, bitOffset);
		}
	}
	
	public void updateForReplacedCapture(int currPiece, int bitOffset) {
		numberOfPieces[currPiece]++;
		mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][currPiece];
		eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][currPiece];
		phase -= Piece.PIECE_PHASE[currPiece];
		int pieceType = currPiece & Piece.PIECE_NO_COLOUR_MASK;
		if (pieceType >= Piece.KNIGHT) {
			addPst(currPiece, bitOffset);
		}
	}
}
