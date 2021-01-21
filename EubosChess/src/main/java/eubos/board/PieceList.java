package eubos.board;

import java.util.Arrays;

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
	private int [][][] piece_list = new int[NUM_COLOURS][NUM_PIECE_TYPES][MAX_NUM_PIECES];
	private byte [][] piece_count = new byte [NUM_COLOURS][NUM_PIECE_TYPES];
	
	public PieceList() {
		for (int [][] side : piece_list) {
			for ( int[] piece_type : side) {
				Arrays.fill(piece_type, Position.NOPOSITION);
			}
		}
	}
	
	public void addPiece(int piece, int atPos) {
		int colour_index = piece >> Piece.COLOUR_BIT_SHIFT;
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
		int colour_index = piece >> Piece.COLOUR_BIT_SHIFT;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert piece != Piece.NONE;
			assert atPos != Position.NOPOSITION;
		}
		
		// find the index into the relevant piece list to clear
		int [] the_list = piece_list[colour_index][piece_index];
		boolean found = false;
		for (int i=0; i < MAX_NUM_PIECES; i++) {
			if (the_list[i] == Position.NOPOSITION) {
				// nothing to remove - fail?
				break;
			} else if (the_list[i] == atPos) {
				piece_count[colour_index][piece_index] -= 1;
				found = true;
			} else {
				// do nothing, just continue search or bringing down other positions...
			}
			if (found) {
				// bring down next entry 
				the_list[i] = (i < (MAX_NUM_PIECES-1)) ? the_list[i+1] : Position.NOPOSITION;
			} 
		}
	}
	
	public void updatePiece(int piece, int atPos, int targetPos) {
		int colour_index = piece >> Piece.COLOUR_BIT_SHIFT;
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
		int colour_index = piece >> Piece.COLOUR_BIT_SHIFT;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		return piece_count[colour_index][piece_index];
	}
	
	public int[] getPieceArray(int piece) {
		int colour_index = piece >> Piece.COLOUR_BIT_SHIFT;
		int piece_index = piece & Piece.PIECE_NO_COLOUR_MASK;
		return Arrays.copyOf(piece_list[colour_index][piece_index],piece_count[colour_index][piece_index]);
	}
	
	public void forEachPieceDoCallback(IForEachPieceCallback caller) {
		// white pieces
		int currPiece = Piece.KING;
		helper(piece_list[0], caller, currPiece);
		// black pieces
		currPiece = Piece.BLACK_KING;
		helper(piece_list[1], caller, currPiece);
	}
	
	private void helper(int [][] side, IForEachPieceCallback caller, int currPiece) {
		for (int i = Piece.KING; i < NUM_PIECE_TYPES; i++) {
			anotherHelper(side[i], caller, currPiece);
			currPiece++;
		}
	}
	
	private void anotherHelper(int [] piece_array, IForEachPieceCallback caller, int piece) {
		for(int atPos : piece_array) {
			if (atPos != Position.NOPOSITION) {
				caller.callback(piece, atPos);
			} else {
				break;
			}
		}
	}
	
	public void forEachPawnOfSideDoCallback(IForEachPieceCallback caller, boolean sideIsBlack) {
		int colour_index = sideIsBlack ? 1 : 0;
		int pawn = sideIsBlack ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
		int [] pawns = piece_list[colour_index][Piece.PAWN];
		anotherHelper(pawns, caller, pawn);		
	}
	
	public int getKingPos(boolean sideIsWhite) {
		int colour_index = sideIsWhite ? 0 : 1;
		return piece_list[colour_index][Piece.KING][0];
	}

	public void forEachPieceOfSideDoCallback(IForEachPieceCallback caller, boolean sideIsWhite) {
		int colour_index = sideIsWhite ? 0 : 1;
		int [][] pieces = piece_list[colour_index];
		int piece = sideIsWhite ? Piece.WHITE_KING : Piece.BLACK_KING;
		helper(pieces, caller, piece);		
	}
}
