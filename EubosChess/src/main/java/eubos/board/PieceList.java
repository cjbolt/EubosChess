package eubos.board;

import java.util.Arrays;
import java.util.List;

import eubos.main.EubosEngineMain;
import eubos.position.Position;

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
		if (EubosEngineMain.ASSERTS_ENABLED) {
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
		if (EubosEngineMain.ASSERTS_ENABLED) {
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
		
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert found;
		}
	}
	
	public void updatePiece(int piece, int atPos, int targetPos) {
		if (EubosEngineMain.ASSERTS_ENABLED) {
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
	
	private void forEachPieceTypeOfSideHelper(IForEachPieceCallback caller, int currPiece) {
		for (int piece = currPiece; piece < currPiece+NUM_PIECE_TYPES; piece++) {
			forEachPieceOfTypeHelper(piece_list[piece], caller, piece);
		}
	}
	
	private void forEachPieceOfTypeHelper(int [] piece_array, IForEachPieceCallback caller, int piece) {
		for (int atPos : piece_array) {
			if (atPos != Position.NOPOSITION) {
				caller.callback(piece, atPos);
			} else break;
		}
	}
	
	public void addMovesEndgame(boolean ownSideIsWhite, List<Integer> movesList, int potentialAttackersOfSquare) {
		int side = ownSideIsWhite ? 0 : Piece.BLACK;
		if (potentialAttackersOfSquare == Position.NOPOSITION) {
			{
				int atSquare = piece_list[side+Piece.KING][0];
				if (atSquare != Position.NOPOSITION) {			
					Piece.king_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				}
			}
			for(int atSquare : piece_list[side+Piece.QUEEN]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.queen_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.ROOK]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.rook_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.BISHOP]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.bishop_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.KNIGHT]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.knight_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.PAWN]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.pawn_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
		} else {
			long allAttacksMask = SquareAttackEvaluator.allAttacksOnPosition_Lut[potentialAttackersOfSquare];
			long pieceMask = ownSideIsWhite ? theBoard.getWhiteKing() : theBoard.getBlackKing();
			if ((allAttacksMask & pieceMask) != 0) {
				int atSquare = piece_list[side+Piece.KING][0];
				if (atSquare != Position.NOPOSITION) {
					if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
						Piece.king_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
					}
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteQueens() : theBoard.getBlackQueens();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.QUEEN]) {
					if (atSquare != Position.NOPOSITION) {
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
							Piece.queen_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteRooks() : theBoard.getBlackRooks();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.ROOK]) {
					if (atSquare != Position.NOPOSITION) {
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {	
							Piece.rook_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteBishops() : theBoard.getBlackBishops();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.BISHOP]) {
					if (atSquare != Position.NOPOSITION) {
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {			
							Piece.bishop_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteKnights() : theBoard.getBlackKnights();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.KNIGHT]) {
					if (atSquare != Position.NOPOSITION) {	
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
							Piece.knight_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			// Search pawn moves in extended search because they could lead to a promotion
			pieceMask = ownSideIsWhite ? theBoard.getWhitePawns() : theBoard.getBlackPawns();
			for(int atSquare : piece_list[side+Piece.PAWN]) {
				if (atSquare != Position.NOPOSITION) {	
					Piece.pawn_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
		}
	}
	
	public void addMovesMiddlegame(boolean ownSideIsWhite, List<Integer> movesList, int potentialAttackersOfSquare) {
		int side = ownSideIsWhite ? 0 : Piece.BLACK;
		if (potentialAttackersOfSquare == Position.NOPOSITION) {
			for(int atSquare : piece_list[side+Piece.QUEEN]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.queen_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.ROOK]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.rook_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.BISHOP]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.bishop_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.KNIGHT]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.knight_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			for(int atSquare : piece_list[side+Piece.PAWN]) {
				if (atSquare != Position.NOPOSITION) {			
					Piece.pawn_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			{
				int atSquare = piece_list[side+Piece.KING][0];
				if (atSquare != Position.NOPOSITION) {			
					Piece.king_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				}
			}
		} else {
			long allAttacksMask = SquareAttackEvaluator.allAttacksOnPosition_Lut[potentialAttackersOfSquare];
			long pieceMask = ownSideIsWhite ? theBoard.getWhiteQueens() : theBoard.getBlackQueens();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.QUEEN]) {
					if (atSquare != Position.NOPOSITION) {
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
							Piece.queen_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteRooks() : theBoard.getBlackRooks();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.ROOK]) {
					if (atSquare != Position.NOPOSITION) {	
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
							Piece.rook_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteBishops() : theBoard.getBlackBishops();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.BISHOP]) {
					if (atSquare != Position.NOPOSITION) {		
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
							Piece.bishop_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteKnights() : theBoard.getBlackKnights();
			if ((allAttacksMask & pieceMask) != 0) {
				for(int atSquare : piece_list[side+Piece.KNIGHT]) {
					if (atSquare != Position.NOPOSITION) {
						if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
							Piece.knight_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
						}
					} else break;
				}
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhitePawns() : theBoard.getBlackPawns();
			// Search pawn moves in extended search because they could lead to a promotion
			for(int atSquare : piece_list[side+Piece.PAWN]) {
				if (atSquare != Position.NOPOSITION) {
					Piece.pawn_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
				} else break;
			}
			pieceMask = ownSideIsWhite ? theBoard.getWhiteKing() : theBoard.getBlackKing();
			if ((allAttacksMask & pieceMask) != 0) {
				int atSquare = piece_list[side+Piece.KING][0];
				if (atSquare != Position.NOPOSITION) {
					if ((BitBoard.positionToMask_Lut[atSquare] & allAttacksMask) != 0) {
						Piece.king_generateMoves(movesList, theBoard, atSquare, ownSideIsWhite);
					}
				}
			}
		}
	}
	
	public void evaluateMaterialBalanceAndPieceMobility(boolean isWhite) {
		int side = isWhite ? 0 : Piece.BLACK;
		{
			int atSquare = piece_list[side+Piece.KING][0];
			if (atSquare != Position.NOPOSITION) {			
				theBoard.me.addPiece(isWhite, Piece.KING);
				theBoard.me.addPosition(isWhite, theBoard.isEndgame ? Board.KING_ENDGAME_WEIGHTINGS[atSquare] : Board.KING_MIDGAME_WEIGHTINGS[atSquare]);
			}
		}
		for(int atSquare : piece_list[side+Piece.QUEEN]) {
			if (atSquare != Position.NOPOSITION) {			
				theBoard.me.addPiece(isWhite, Piece.QUEEN);
				if (!theBoard.isEndgame)
					theBoard.me.addPosition(isWhite, theBoard.getTwiceNumEmptyAllDirectSquares(atSquare));
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.ROOK]) {
			if (atSquare != Position.NOPOSITION) {			
				theBoard.me.addPiece(isWhite, Piece.ROOK);
				if (!theBoard.isEndgame)
					theBoard.me.addPosition(isWhite, theBoard.getTwiceNumEmptyRankFileSquares(atSquare));
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.BISHOP]) {
			if (atSquare != Position.NOPOSITION) {			
				theBoard.me.addPiece(isWhite, Piece.BISHOP);
				if (!theBoard.isEndgame)
					theBoard.me.addPosition(isWhite, theBoard.getTwiceNumEmptyDiagonalSquares(atSquare));
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.KNIGHT]) {
			if (atSquare != Position.NOPOSITION) {			
				theBoard.me.addPiece(isWhite, Piece.KNIGHT);
				theBoard.me.addPosition(isWhite, Board.KNIGHT_WEIGHTINGS[atSquare]);
			} else break;
		}
		for(int atSquare : piece_list[side+Piece.PAWN]) {
			if (atSquare != Position.NOPOSITION) {			
				theBoard.me.addPiece(isWhite, Piece.PAWN);
				theBoard.me.addPosition(isWhite, isWhite ? Board.PAWN_WHITE_WEIGHTINGS[atSquare] : Board.PAWN_BLACK_WEIGHTINGS[atSquare]);
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
		return piece_list[piece][0];
	}
}
