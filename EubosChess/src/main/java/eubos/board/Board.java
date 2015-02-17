package eubos.board;

import eubos.pieces.*;

import com.fluxchess.jcpi.models.*;

public class Board {
	private Piece[][] theBoard = new Piece[8][8];
	
	public Board() {
		setupNewGame();
	}
	
	public GenericMove findBestMove() throws IllegalNotationException {
		// for now find a random legal move for the side indicated
		return( new GenericMove("e7e5") );
	}
	
	private void setupNewGame() {
		setupBackRanks();
		// Create Pawns
		for (int i: IntFile.values) {
			theBoard[i][IntRank.R2] = new Pawn( Piece.PieceColour.white );
			theBoard[i][IntRank.R7] = new Pawn( Piece.PieceColour.black );
		}
	}
	
	private void setupBackRanks() {
		// White
		theBoard[IntFile.Fa][IntRank.R1] = new Rook( Piece.PieceColour.white );
		theBoard[IntFile.Fb][IntRank.R1] = new Knight( Piece.PieceColour.white );
		theBoard[IntFile.Fc][IntRank.R1] = new Bishop( Piece.PieceColour.white );
		theBoard[IntFile.Fd][IntRank.R1] = new Queen( Piece.PieceColour.white );
		theBoard[IntFile.Fe][IntRank.R1] = new King( Piece.PieceColour.white );
		theBoard[IntFile.Ff][IntRank.R1] = new Bishop( Piece.PieceColour.white );
		theBoard[IntFile.Fg][IntRank.R1] = new Knight( Piece.PieceColour.white );
		theBoard[IntFile.Fh][IntRank.R1] = new Rook( Piece.PieceColour.white );
		// Black
		theBoard[IntFile.Fa][IntRank.R8] = new Rook( Piece.PieceColour.black );
		theBoard[IntFile.Fb][IntRank.R8] = new Knight( Piece.PieceColour.black );
		theBoard[IntFile.Fc][IntRank.R8] = new Bishop( Piece.PieceColour.black );
		theBoard[IntFile.Fd][IntRank.R8] = new Queen( Piece.PieceColour.black );
		theBoard[IntFile.Fe][IntRank.R8] = new King( Piece.PieceColour.black );
		theBoard[IntFile.Ff][IntRank.R8] = new Bishop( Piece.PieceColour.black );
		theBoard[IntFile.Fg][IntRank.R8] = new Knight( Piece.PieceColour.black );
		theBoard[IntFile.Fh][IntRank.R8] = new Rook( Piece.PieceColour.black );
	}
}
