package eubos.board;

import java.util.Arrays;

import com.fluxchess.jcpi.models.IntRank;

import eubos.main.EubosEngineMain;
import eubos.position.IAddMoves;
import eubos.position.Position;
import eubos.score.PiecewiseEvaluation;

public class PieceList {
	
	private static final int NUM_COLOURS = 2;
	private static final int NUM_PIECE_TYPES = 7;
	private static final int MAX_NUM_PIECES = 8; // based on max number of pawns
	
	// Piece lists
	// 1st index Colour 0 is white, 1 is black
	// 2nd index is piece type
	// 3rd index is index into that piece array, gives the position of that piece
	//     NOTE: DELIBERATELY TOO LONG, simplifies array traversal when removing and bringing down pieces
	private int [][] piece_list = new int[NUM_COLOURS*(NUM_PIECE_TYPES+1)][MAX_NUM_PIECES+1];
	
	private Board theBoard;
	
	public PieceList(Board theBoard) {
		for (int [] piece : piece_list) {
			Arrays.fill(piece, Position.NOPOSITION);
		}
		this.theBoard = theBoard;
	}
	
	public void addPiece(int piece, int atPos) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		for (int piece_number = 0; piece_number < MAX_NUM_PIECES; piece_number++) {
			if (piece_list[piece][piece_number] == Position.NOPOSITION) {
				piece_list[piece][piece_number] = atPos;
				break;
			}
		}
	}
	
	public void removePiece(int piece, int atPos) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		// find the index into the relevant piece list to clear
		int [] the_list = piece_list[piece];
		boolean found = false;
		for (int i=0; i < MAX_NUM_PIECES; i++) {
			if (the_list[i] == atPos) {
				found = true;
			}
			if (found) {
				// Bring down next entry 
				the_list[i] = the_list[i+1];
				// Break out ASAP
				if (the_list[i+1] == Position.NOPOSITION) {
					break;
				}
			}
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert found;
		}
	}
	
	public void updatePiece(int piece, int atPos, int targetPos) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		// find the piece to update
		for (int piece_number = 0; piece_number < MAX_NUM_PIECES; piece_number++) {
			if (piece_list[piece][piece_number] == atPos) {
				piece_list[piece][piece_number] = targetPos;
				break;
			}
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// error check to ensure consistency of piece list with bitboard!
			boolean found = false;
			for (int piece_number = 0; piece_number < MAX_NUM_PIECES; piece_number++) {
				if (piece_list[piece][piece_number] == targetPos) {
					found = true;
					break;
				}
			}
			assert found : String.format("PieceList out of sync with Board for %d at %s",
					piece, Position.toGenericPosition(targetPos));
		}
	}
	
	public void updatePiece(int oldPiece, int piece, int atPos, int targetPos) {
		// find the index into the relevant piece list to clear
		int [] the_list = piece_list[oldPiece];
		boolean found = false;
		for (int i=0; i < MAX_NUM_PIECES; i++) {
			if (the_list[i] == atPos) {
				found = true;
			} else {
				// Continue search or bringing down other positions...
			}
			if (found) {
				// Bring down next entry 
				the_list[i] = the_list[i+1];
				// Break out ASAP
				if (the_list[i] == Position.NOPOSITION) {
					break;
				}
			}
		}
		// find the piece to update
		for (int piece_number = 0; piece_number < MAX_NUM_PIECES; piece_number++) {
			if (piece_list[piece][piece_number] == Position.NOPOSITION) {
				piece_list[piece][piece_number] = targetPos;
				break;
			}
		}
	}
	
	public void forEachPieceDoCallback(IForEachPieceCallback caller) {
		// white pieces
		forEachPieceTypeOfSideHelper(caller, Piece.WHITE_KING);
		// black pieces
		forEachPieceTypeOfSideHelper(caller, Piece.BLACK_KING);
	}
	
	public void forEachPieceOfTypeDoCallback(IForEachPieceCallback caller, int[] pieceTypesToIterate) {
		for(int piece : pieceTypesToIterate) {
			forEachPieceOfTypeHelper(piece_list[piece], caller, piece);
		}
	}
	
	private void forEachPieceTypeOfSideHelper(IForEachPieceCallback caller, int currPiece) {
		for (int piece = currPiece; piece < currPiece+NUM_PIECE_TYPES; piece++) {
			forEachPieceOfTypeHelper(piece_list[piece], caller, piece);
		}
	}
	
	private void forEachPieceOfTypeHelper(int [] piece_array, IForEachPieceCallback caller, int piece) {
		for (int atPos : piece_array) {
			if (atPos != Position.NOPOSITION) {
				caller.callback(piece, BitBoard.bitToPosition_Lut[atPos]);
			} else break;
		}
	}
	
	public void addMovesEndgame_White(IAddMoves ml) {
		{
			int atSquare = piece_list[Piece.WHITE_KING][0];
			if (atSquare != Position.NOPOSITION) {			
				Piece.king_generateMoves_White(ml, theBoard, atSquare);
			}
		}
		for(int atSquare : piece_list[Piece.WHITE_PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.pawn_generateMoves_White(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.queen_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.rook_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.bishop_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.knight_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
	}
	
	public void addMovesEndgame_Black(IAddMoves ml) {
		{
			int atSquare = piece_list[Piece.BLACK_KING][0];
			if (atSquare != Position.NOPOSITION) {			
				Piece.king_generateMoves_Black(ml, theBoard, atSquare);
			}
		}
		for(int atSquare : piece_list[Piece.BLACK_PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.pawn_generateMoves_Black(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.queen_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.rook_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.bishop_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.knight_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
	}
	
	public void addMovesMiddlegame_White(IAddMoves ml) {
		for(int atSquare : piece_list[Piece.WHITE_BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.bishop_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.knight_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.queen_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.rook_generateMoves_White(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.pawn_generateMoves_White(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
			} else break;
		}
		{
			int atSquare = piece_list[Piece.WHITE_KING][0];
			if (atSquare != Position.NOPOSITION) {			
				Piece.king_generateMoves_White(ml, theBoard, atSquare);
			}
		}
	}
	
	public void addMovesMiddlegame_Black(IAddMoves ml) {
		for(int atSquare : piece_list[Piece.BLACK_BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.bishop_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.knight_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.queen_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.rook_generateMoves_Black(ml, theBoard, atSquare);
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.pawn_generateMoves_Black(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
			} else break;
		}
		{
			int atSquare = piece_list[Piece.BLACK_KING][0];
			if (atSquare != Position.NOPOSITION) {			
				Piece.king_generateMoves_Black(ml, theBoard, atSquare);
			}
		}
	}
	
	public void evaluateMaterialBalanceAndStaticPieceMobility(boolean isWhite, PiecewiseEvaluation me) {
		int side = isWhite ? 0 : Piece.BLACK;
		{
			int bitOffset = piece_list[side+Piece.KING][0];
			if (bitOffset != Position.NOPOSITION) {	
				me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.KING];
				me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.KING];
				me.position += Piece.PIECE_SQUARE_TABLES[side+Piece.KING][bitOffset];
				me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[side+Piece.KING][bitOffset];
			}
		}
		for(int bitOffset : piece_list[side+Piece.QUEEN]) {
			if (bitOffset != Position.NOPOSITION) {
				me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.QUEEN];
				me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.QUEEN];
				me.numberOfPieces[side+Piece.QUEEN]++;
			} else break;
		}
		for(int bitOffset : piece_list[side+Piece.ROOK]) {
			if (bitOffset != Position.NOPOSITION) {			
				me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.ROOK];
				me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.ROOK];
				me.position += Piece.PIECE_SQUARE_TABLES[side+Piece.ROOK][bitOffset];
				me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[side+Piece.ROOK][bitOffset];
				me.numberOfPieces[side+Piece.ROOK]++;
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.BISHOP];
				me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.BISHOP];
				me.numberOfPieces[side+Piece.BISHOP]++;
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.KNIGHT];
				me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.KNIGHT];
				me.position += Piece.PIECE_SQUARE_TABLES[side+Piece.KNIGHT][atSquare];
				me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[side+Piece.KNIGHT][atSquare];
				me.numberOfPieces[side+Piece.KNIGHT]++;
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.PAWN]) {
			if (atSquare != Position.NOPOSITION) {
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert theBoard.getPieceAtSquare(1L << atSquare) != Piece.NONE :
						String.format("Found a Pawn at %s that isn't on Board", Position.toGenericPosition(atSquare));
				}
				me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.PAWN];
				me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.PAWN];
				me.position += Piece.PIECE_SQUARE_TABLES[side+Piece.PAWN][atSquare];
				me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[side+Piece.PAWN][atSquare];
				me.numberOfPieces[side+Piece.PAWN]++;
			} else break;
		}
	}
	
	public void forEachPawnOfSideDoCallback(IForEachPieceCallback caller, boolean sideIsBlack) {
		int pawn = sideIsBlack ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
		int [] pawns = piece_list[pawn];
		forEachPieceOfTypeHelper(pawns, caller, pawn);		
	}
	
	public int getKingPos(boolean sideIsWhite) {
		int piece = sideIsWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
		int pos = piece_list[piece][0];
		return pos != Position.NOPOSITION ? BitBoard.bitToPosition_Lut[pos] : Position.NOPOSITION;
	}
	
	public int getQueenPos(boolean sideIsWhite) {
		int piece = sideIsWhite ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
		int pos = piece_list[piece][0];
		return pos != Position.NOPOSITION ? BitBoard.bitToPosition_Lut[pos] : Position.NOPOSITION;
	}
	
	public boolean validCaptureMoveExistsBlack(IAddMoves ml) {
		for(int atSquare : piece_list[Piece.BLACK_KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.knight_generateMovesExtSearch_Black(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.pawn_generateMovesForExtendedSearch_Black(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		{
			int atSquare = piece_list[Piece.BLACK_KING][0];
			if (atSquare != Position.NOPOSITION) {			
				Piece.king_generateMovesExtSearch_Black(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			}
		}
		for(int atSquare : piece_list[Piece.BLACK_QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.queen_generateMovesExtSearch_Black(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.bishop_generateMovesExtSearch_Black(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		for(int atSquare : piece_list[Piece.BLACK_ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.rook_generateMovesExtSearch_Black(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		return false;
	}
	
	public boolean validCaptureMoveExistsWhite(IAddMoves ml) {
		for(int atSquare : piece_list[Piece.WHITE_KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.knight_generateMovesExtSearch_White(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.pawn_generateMovesForExtendedSearch_White(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		{
			int atSquare = piece_list[Piece.WHITE_KING][0];
			if (atSquare != Position.NOPOSITION) {			
				Piece.king_generateMovesExtSearch_White(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			}
		}
		for(int atSquare : piece_list[Piece.WHITE_QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.queen_generateMovesExtSearch_White(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.bishop_generateMovesExtSearch_White(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		for(int atSquare : piece_list[Piece.WHITE_ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				Piece.rook_generateMovesExtSearch_White(ml, theBoard, atSquare);
				if (ml.isLegalMoveFound()) return true;
			} else break;
		}
		return false;
	}

	public void addMoves_PawnPromotions_White(IAddMoves ml) {
		for (int atSquare : piece_list[Piece.WHITE_PAWN]) {
			if (atSquare != Position.NOPOSITION) {
				if (BitBoard.getRank(atSquare) == IntRank.R7) {
					Piece.pawn_generatePromotionMoves_White(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
				}
			} else break;
		}
	}
	
	public void addMoves_PawnPromotions_Black(IAddMoves ml) {
		for (int atSquare : piece_list[Piece.BLACK_PAWN]) {
			if (atSquare != Position.NOPOSITION) {
				if (BitBoard.getRank(atSquare) == IntRank.R2) {
					Piece.pawn_generatePromotionMoves_Black(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
				}
			} else break;
		}
	}
	
	public void addMoves_CapturesExcludingPawnPromotions_White(IAddMoves ml, boolean isEndgame) {
		// Optimisations for generating move lists in extended search
		long opponentPieces = theBoard.getBlackPieces();
		if (isEndgame) {
			{
				int atSquare = piece_list[Piece.WHITE_KING][0];
				if (atSquare != Position.NOPOSITION) {
					long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & kingAttacksMask) != 0) {
						Piece.king_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				}
			}
			// Only search pawn moves that cannot be a promotion
			for(int atSquare : piece_list[Piece.WHITE_PAWN]) {
				if (atSquare != Position.NOPOSITION) {
					if (IntRank.R7 != BitBoard.getRank(atSquare)) {
						Piece.pawn_generateMovesForExtendedSearch_White(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_QUEEN]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {
						Piece.queen_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_ROOK]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {	
						Piece.rook_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_BISHOP]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {			
						Piece.bishop_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_KNIGHT]) {
				if (atSquare != Position.NOPOSITION) {	
					long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & knightAttacksMask) != 0) {
						Piece.knight_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
		} else {
			for(int atSquare : piece_list[Piece.WHITE_BISHOP]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {			
						Piece.bishop_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_KNIGHT]) {
				if (atSquare != Position.NOPOSITION) {	
					long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & knightAttacksMask) != 0) {
						Piece.knight_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_QUEEN]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {
						Piece.queen_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.WHITE_ROOK]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {	
						Piece.rook_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				} else break;
			}
			// Only search pawn moves that cannot be a promotion
			for(int atSquare : piece_list[Piece.WHITE_PAWN]) {
				if (atSquare != Position.NOPOSITION) {
					if (IntRank.R7 != BitBoard.getRank(atSquare)) {
						Piece.pawn_generateMovesForExtendedSearch_White(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
					}
				} else break;
			}
			{
				int atSquare = piece_list[Piece.WHITE_KING][0];
				if (atSquare != Position.NOPOSITION) {
					long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & kingAttacksMask) != 0) {
						Piece.king_generateMovesExtSearch_White(ml, theBoard, atSquare);
					}
				}
			}
		}
	}
	
	public void addMoves_CapturesExcludingPawnPromotions_Black(IAddMoves ml, boolean isEndgame) {
		// Optimisations for generating move lists in extended search
		long opponentPieces = theBoard.getWhitePieces();
		if (isEndgame) {
			{
				int atSquare = piece_list[Piece.BLACK_KING][0];
				if (atSquare != Position.NOPOSITION) {
					long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & kingAttacksMask) != 0) {
						Piece.king_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				}
			}
			// Only search pawn moves that cannot be a promotion
			for(int atSquare : piece_list[Piece.BLACK_PAWN]) {
				if (atSquare != Position.NOPOSITION) {
					if (IntRank.R2 != BitBoard.getRank(atSquare)) {
						Piece.pawn_generateMovesForExtendedSearch_Black(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_QUEEN]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {
						Piece.queen_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_ROOK]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {	
						Piece.rook_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_BISHOP]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {			
						Piece.bishop_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_KNIGHT]) {
				if (atSquare != Position.NOPOSITION) {	
					long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & knightAttacksMask) != 0) {
						Piece.knight_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
		} else {
			for(int atSquare : piece_list[Piece.BLACK_BISHOP]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {			
						Piece.bishop_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_KNIGHT]) {
				if (atSquare != Position.NOPOSITION) {	
					long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & knightAttacksMask) != 0) {
						Piece.knight_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_QUEEN]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {
						Piece.queen_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			for(int atSquare : piece_list[Piece.BLACK_ROOK]) {
				if (atSquare != Position.NOPOSITION) {
					long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & attacksMask) != 0) {	
						Piece.rook_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				} else break;
			}
			// Only search pawn moves that cannot be a promotion
			for(int atSquare : piece_list[Piece.BLACK_PAWN]) {
				if (atSquare != Position.NOPOSITION) {
					if (IntRank.R2 != BitBoard.getRank(atSquare)) {
						Piece.pawn_generateMovesForExtendedSearch_Black(ml, theBoard, BitBoard.bitToPosition_Lut[atSquare]);
					}
				} else break;
			}
			{
				int atSquare = piece_list[Piece.BLACK_KING][0];
				if (atSquare != Position.NOPOSITION) {
					long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[BitBoard.bitToPosition_Lut[atSquare]];
					if ((opponentPieces & kingAttacksMask) != 0) {
						Piece.king_generateMovesExtSearch_Black(ml, theBoard, atSquare);
					}
				}
			}
		}
	}
	
	public boolean isPresent(int pieceToMove, int originSquare) {
		for(int atSquare : piece_list[pieceToMove]) {
			if (atSquare != Position.NOPOSITION) {
				if (originSquare == atSquare) {
					return true;
				}
			} else break;
		}
		return false;
	}
}
