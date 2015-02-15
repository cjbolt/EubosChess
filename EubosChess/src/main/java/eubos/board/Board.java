package eubos.board;

import eubos.pieces.*;
import com.fluxchess.jcpi.models.*;

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
		// White
		theBoard[IntFile.Fa][IntRank.R1] = new Rook();
		theBoard[IntFile.Fb][IntRank.R1] = new Knight();
		theBoard[IntFile.Fc][IntRank.R1] = new Bishop();
		theBoard[IntFile.Fd][IntRank.R1] = new Queen();
		theBoard[IntFile.Fe][IntRank.R1] = new King();
		theBoard[IntFile.Ff][IntRank.R1] = new Bishop();
		theBoard[IntFile.Fg][IntRank.R1] = new Knight();
		theBoard[IntFile.Fh][IntRank.R1] = new Rook();
		// Black
		theBoard[IntFile.Fa][IntRank.R1] = new Rook();
		theBoard[IntFile.Fb][IntRank.R1] = new Knight();
		theBoard[IntFile.Fc][IntRank.R1] = new Bishop();
		theBoard[IntFile.Fd][IntRank.R1] = new Queen();
		theBoard[IntFile.Fe][IntRank.R1] = new King();
		theBoard[IntFile.Ff][IntRank.R1] = new Bishop();
		theBoard[IntFile.Fg][IntRank.R1] = new Knight();
		theBoard[IntFile.Fh][IntRank.R1] = new Rook();
	}
}
