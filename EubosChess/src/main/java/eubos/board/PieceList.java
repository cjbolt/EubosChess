package eubos.board;

import java.util.Arrays;

import eubos.main.EubosEngineMain;
import eubos.position.Position;

public class PieceList {
	
	// Piece lists
	// 1st index Colour 0 is white, 1 is black
	// 2nd index is piece type
	// 3rd index is index into that piece array, gives the position of that piece
	private int [][][] piece_list = new int[2][7][8];
	private byte [][] piece_count = new byte [2][7];
	
	public PieceList() {
		for (int [][] side : piece_list) {
			for ( int[] piece_type : side) {
				Arrays.fill(piece_type, Position.NOPOSITION);
			}
		}
	}
	
	public void addPiece(int piece, int atPos) {
		int colour_index = Piece.isBlack(piece) ? 1 : 0;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		int piece_number = 0;
		
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		// find the index into the relevant piece list to set
		for (int position : piece_list[colour_index][piece_index]) {
			if (position == Position.NOPOSITION) {
				piece_list[colour_index][piece_index][piece_number] = atPos;
				piece_count[colour_index][piece_index] += 1;
				break;
			} else if (position == atPos) {
				// return no change needed
				break;
			} else {
				// search on
				piece_number++;
			}
		}
	}
	
	public void removePiece(int piece, int atPos) {
		int colour_index = Piece.isBlack(piece) ? 1 : 0;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		// find the index into the relevant piece list to clear
		int [] the_list = piece_list[colour_index][piece_index];
		boolean found = false;
		for (int i=0; i < 8; i++) {
			if (the_list[i] == Position.NOPOSITION) {
				// nothing to remove - fail?
				break;
			}
			if (!found && the_list[i] == atPos) {
				piece_count[colour_index][piece_index] -= 1;
				found = true;
			}
			if (found) {
				if (i != 7) {
					// bring down next entry 
					the_list[i] = the_list[i+1];
				} else {
					// can't bring down next entry
					the_list[i] = Position.NOPOSITION;
				}
			} else {
				// do nothing, go to next index....
			}
		}
	}
	
	public void updatePiece(int piece, int atPos, int targetPos) {
		int colour_index = Piece.isBlack(piece) ? 1 : 0;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		int piece_number = 0;
		
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		// find the index into the relevant piece list to update
		for (int position : piece_list[colour_index][piece_index]) {
			if (position == atPos) {
				piece_list[colour_index][piece_index][piece_number] = targetPos;
				break;
			} else {
				// search on
				piece_number++;
			}
		}
	}
	
	public int getNum(int piece) {
		int colour_index = Piece.isBlack(piece) ? 1 : 0;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		return piece_count[colour_index][piece_index];
	}
	
	public int[] getPieceArray(int piece) {
		int colour_index = Piece.isBlack(piece) ? 1 : 0;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		return Arrays.copyOf(piece_list[colour_index][piece_index],piece_count[colour_index][piece_index]);
	}
	
	public void forEachPieceDoCallback(IForEachPieceCallback caller) {
		int [][] whiteSide = piece_list[0];
		int currPiece = Piece.NONE;
		for (int [] piece : whiteSide) { 
			for(int atPos : piece) {
				if (atPos != Position.NOPOSITION) {
					caller.callback(currPiece, atPos);
				} else {
					break;
				}
			}
			currPiece++;
		}
		int [][] blackSide = piece_list[1];
		currPiece = Piece.NONE | Piece.BLACK;
		for (int [] piece : blackSide) { 
			for(int atPos : piece) {
				if (atPos != Position.NOPOSITION) {
					caller.callback(currPiece, atPos);
				} else {
					break;
				}
			}
			currPiece++;
		}
	}
	
	public void forEachPawnOfSideDoCallback(IForEachPieceCallback caller, boolean isBlack) {
		int colour_index = isBlack ? 1 : 0;
		int pawn = isBlack ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
		int [] pawns = piece_list[colour_index][Piece.PAWN];
		for(int atPos : pawns) {
			if (atPos != Position.NOPOSITION) {
				caller.callback(pawn, atPos);
			} else {
				break;
			}
		}		
	}
}
