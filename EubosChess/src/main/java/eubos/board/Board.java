package eubos.board;

import eubos.pieces.*;

public class Board {
	private Piece[][] theBoard = new Piece[8][8];
	
	public void setupNewGame() {
		for (int i=0; i<8; i++) {
			for (int j=0; j<8; j++){
				theBoard[i][j] = null;
			}
		}
		setupBackRanks();
		for (int i=0; i<8; i++) {
			theBoard[i][1] = new Pawn();
			theBoard[i][6] = new Pawn();
		}
	}
	
	private void setupBackRanks() {
		theBoard[0][0] = new Rook();
		theBoard[1][0] = new Knight();
		theBoard[2][0] = new Bishop();
		theBoard[3][0] = new Queen();
		theBoard[4][0] = new King();
		theBoard[5][0] = new Bishop();
		theBoard[6][0] = new Knight();
		theBoard[7][0] = new Rook();
		theBoard[0][7] = new Rook();
		theBoard[1][7] = new Knight();
		theBoard[2][7] = new Bishop();
		theBoard[3][7] = new Queen();
		theBoard[4][7] = new King();
		theBoard[5][7] = new Bishop();
		theBoard[6][7] = new Knight();
		theBoard[7][7] = new Rook();
	}
}
