package eubos.board;

import java.util.*;

import eubos.pieces.*;

import com.fluxchess.jcpi.models.*;

public class Board {
	private Piece[][] theBoard = new Piece[8][8];
	
	public Board() {
		setupNewGame();
	}
	
	public GenericMove findBestMove() throws IllegalNotationException {
		// for now find a random legal move for the side indicated
		// first generate the entire move list
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		for (int i: IntFile.values) {
			for (int j: IntRank.values) {
				Piece nextPiece = theBoard[i][j];
				if (nextPiece != null && nextPiece.isBlack() ) {
					// append this piece's legal moves to the entire move list
					entireMoveList.addAll(nextPiece.generateMoveList(this));
				}
			}
		}
		// secondly return a move at random
		Random randomIndex = new Random();
		Integer indexToGet = randomIndex.nextInt(entireMoveList.size());
		GenericMove bestMove = entireMoveList.get(indexToGet);
		return (bestMove);
	}
	
	public void update( GenericMove move ) {
		// Move the piece
		GenericPosition posFrom = move.from;
		Piece pieceToMove = theBoard[IntFile.valueOf(posFrom.file)][IntRank.valueOf(posFrom.rank)];
		theBoard[IntFile.valueOf(posFrom.file)][IntRank.valueOf(posFrom.rank)] = null;
		GenericPosition posTo = move.to;
		theBoard[IntFile.valueOf(posTo.file)][IntRank.valueOf(posTo.rank)] = pieceToMove;
		// Update the piece's square.
		pieceToMove.updateSquare(posTo);
	}
	
	private void setupNewGame() {
		setupBackRanks();
		setupPawns();
	}
	
	private void setupPawns() {
		for ( GenericFile file : GenericFile.values()) {
			GenericPosition pos = GenericPosition.valueOf( file, GenericRank.R2);
			theBoard[IntFile.valueOf(file)][IntRank.R2] = new Pawn( Piece.PieceColour.white, pos );
			pos = GenericPosition.valueOf( file, GenericRank.R7);
			theBoard[IntFile.valueOf(file)][IntRank.R7] = new Pawn( Piece.PieceColour.black, pos );
		}
	}
	
	private void setupBackRanks() {
		// White
		theBoard[IntFile.Fa][IntRank.R1] = new Rook( Piece.PieceColour.white, GenericPosition.a1 );
		theBoard[IntFile.Fb][IntRank.R1] = new Knight( Piece.PieceColour.white, GenericPosition.b1 );
		theBoard[IntFile.Fc][IntRank.R1] = new Bishop( Piece.PieceColour.white, GenericPosition.c1 );
		theBoard[IntFile.Fd][IntRank.R1] = new Queen( Piece.PieceColour.white, GenericPosition.d1 );
		theBoard[IntFile.Fe][IntRank.R1] = new King( Piece.PieceColour.white, GenericPosition.e1 );
		theBoard[IntFile.Ff][IntRank.R1] = new Bishop( Piece.PieceColour.white, GenericPosition.f1 );
		theBoard[IntFile.Fg][IntRank.R1] = new Knight( Piece.PieceColour.white, GenericPosition.g1 );
		theBoard[IntFile.Fh][IntRank.R1] = new Rook( Piece.PieceColour.white, GenericPosition.h1 );
		// Black
		theBoard[IntFile.Fa][IntRank.R8] = new Rook( Piece.PieceColour.black, GenericPosition.a8 );
		theBoard[IntFile.Fb][IntRank.R8] = new Knight( Piece.PieceColour.black, GenericPosition.b8 );
		theBoard[IntFile.Fc][IntRank.R8] = new Bishop( Piece.PieceColour.black, GenericPosition.c8 );
		theBoard[IntFile.Fd][IntRank.R8] = new Queen( Piece.PieceColour.black, GenericPosition.d8 );
		theBoard[IntFile.Fe][IntRank.R8] = new King( Piece.PieceColour.black, GenericPosition.e8 );
		theBoard[IntFile.Ff][IntRank.R8] = new Bishop( Piece.PieceColour.black, GenericPosition.f8 );
		theBoard[IntFile.Fg][IntRank.R8] = new Knight( Piece.PieceColour.black, GenericPosition.g8 );
		theBoard[IntFile.Fh][IntRank.R8] = new Rook( Piece.PieceColour.black, GenericPosition.h8 );
	}
}
