package eubos.board;

import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.CastlingManager;
import eubos.position.IAddMoves;
import eubos.position.Move;
import eubos.position.Position;
import eubos.score.PiecewiseEvaluation;

import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntRank;

public class Board {
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
			Piece.MATERIAL_VALUE_KING + 
			Piece.MATERIAL_VALUE_ROOK + 
			Piece.MATERIAL_VALUE_KNIGHT + 
			(4 * Piece.MATERIAL_VALUE_PAWN);
	
	static final int ENDGAME_MATERIAL_THRESHOLD_WITHOUT_QUEENS =
			Piece.MATERIAL_VALUE_KING + 
			Piece.MATERIAL_VALUE_ROOK + 
			Piece.MATERIAL_VALUE_KNIGHT +
			Piece.MATERIAL_VALUE_BISHOP +
			(4 * Piece.MATERIAL_VALUE_PAWN);
	
	private PieceList pieceLists = new PieceList(this);
	
	public PiecewiseEvaluation me;
	
	PawnAttackAggregator paa;
	public PawnKnightAttackAggregator pkaa;
	
	public Board( Map<Integer, Integer> pieceMap,  Piece.Colour initialOnMove ) {
		paa = new PawnAttackAggregator();
		pkaa = new PawnKnightAttackAggregator();
		allPieces = 0x0;
		whitePieces = 0x0;
		blackPieces = 0x0;
		for (int i=0; i<=INDEX_PAWN; i++) {
			pieces[i] = 0x0;
		}
		for ( Entry<Integer, Integer> nextPiece : pieceMap.entrySet() ) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue() );
		}
		me = new PiecewiseEvaluation();
		evaluateMaterial(me);
	}
	
	public static String reportStaticDataSizes() {
		StringBuilder s = new StringBuilder();
		int bytecountofstatics = 0; //Piece.PAWN_WHITE_WEIGHTINGS.length + Piece.PAWN_BLACK_WEIGHTINGS.length + Piece.KNIGHT_WEIGHTINGS.length + Piece.KING_ENDGAME_WEIGHTINGS.length + Piece.KING_MIDGAME_WEIGHTINGS.length;
		s.append(String.format("PieceSquareTables %d bytes\n", bytecountofstatics));
		int len = 0;
		for(int i = 0; i < BitBoard.PassedPawn_Lut.length; i++)
		{
		    len += BitBoard.PassedPawn_Lut[i].length;
		}
		s.append(String.format("PassedPawn_Lut %d bytes\n", len*8));
		s.append(String.format("FileMask_Lut %d bytes\n", BitBoard.FileMask_Lut.length*8));
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
	
	public int doMove(int move) {
		int capturePosition = Position.NOPOSITION;
		int pieceToMove = Move.getOriginPiece(move);
		boolean isWhite = Piece.isWhite(pieceToMove);
		int originSquare = Move.getOriginPosition(move);
		int targetSquare = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
		int promotedPiece = Move.getPromotion(move);
		long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
		long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
		long positionsMask = initialSquareMask | targetSquareMask;
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert (pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] & initialSquareMask) != 0: 
				String.format("Non-existant piece at %s", Position.toGenericPosition(originSquare));
		}
		
		// Initialise En Passant target square
		setEnPassantTargetSq(Position.NOPOSITION);
		
		if (targetPiece != Piece.NONE) {
			// Handle captures
			if (Move.isEnPassantCapture(move)) {
				// Handle en passant captures, don't need to do other checks in this case
				capturePosition = generateCapturePositionForEnPassant(pieceToMove, targetSquare);
			} else {
				capturePosition = targetSquare;
			}
			pickUpPieceAtSquare(capturePosition, targetPiece);
			// Incrementally update opponent material after capture, at the correct capturePosition
		    subtractMaterialAndPositionForCapture(targetPiece, capturePosition);
		} else {
			// Check whether the move sets the En Passant target square
			if (!moveEnablesEnPassantCapture(pieceToMove, originSquare, targetSquare)) {
				// Handle castling secondary rook moves...
				if (Piece.isKing(pieceToMove)) {
					performSecondaryCastlingMove(move);
				}
			}
		}
		
		// Switch piece-specific bitboards and piece lists
		if (promotedPiece != Piece.NONE) {
			// For a promotion, need to resolve piece-specific across multiple bitboards
			pieces[INDEX_PAWN] &= ~initialSquareMask;
			pieces[promotedPiece] |= targetSquareMask;
			int fullPromotedPiece = (isWhite ? promotedPiece : promotedPiece|Piece.BLACK);
			pieceLists.updatePiece(pieceToMove, fullPromotedPiece, originSquare, targetSquare);
			updateMaterialAndPositionForDoingPromotion(fullPromotedPiece, originSquare, targetSquare);
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] ^= positionsMask;
			pieceLists.updatePiece(pieceToMove, originSquare, targetSquare);
			// Update PST
			me.position -= Piece.PIECE_SQUARE_TABLES[pieceToMove][originSquare];
			me.position += Piece.PIECE_SQUARE_TABLES[pieceToMove][targetSquare];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[pieceToMove][originSquare];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[pieceToMove][targetSquare];
		}
		// Switch colour bitboard
		if (isWhite) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// check material incrementally updated against from scratch
			PiecewiseEvaluation scratch_me = new PiecewiseEvaluation();
			evaluateMaterial(scratch_me);
			assert scratch_me != me;
			assert scratch_me.material == me.material;
			assert scratch_me.position == me.position;
			assert scratch_me.positionEndgame == me.positionEndgame;
			assert scratch_me.phase == me.phase;
		}
		
		return capturePosition;
	}
	
	public int undoMove(int moveToUndo) {
		int capturedPieceSquare = Position.NOPOSITION;
		int originPiece = Move.getOriginPiece(moveToUndo);
		boolean isWhite = Piece.isWhite(originPiece);
		int originSquare = Move.getOriginPosition(moveToUndo);
		int targetSquare = Move.getTargetPosition(moveToUndo);
		int targetPiece = Move.getTargetPiece(moveToUndo);
		int promotedPiece = Move.getPromotion(moveToUndo);
		long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
		long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
		long positionsMask = initialSquareMask | targetSquareMask;
		boolean isCapture = targetPiece != Piece.NONE;
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			long pieceMask = (promotedPiece != Piece.NONE) ? pieces[promotedPiece] : pieces[Piece.PIECE_NO_COLOUR_MASK & originPiece];
			assert (pieceMask & initialSquareMask) != 0: String.format("Non-existant piece at %s, %s",
					Position.toGenericPosition(originSquare), Move.toString(moveToUndo));
		}
		
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
			int fullPromotedPiece = (isWhite ? promotedPiece : promotedPiece|Piece.BLACK);
			pieceLists.updatePiece(fullPromotedPiece, originPiece, originSquare, targetSquare);
			updateMaterialAndPositionForUndoingPromotion(fullPromotedPiece, originSquare, targetSquare);
		} else {
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & originPiece] ^= positionsMask;
			pieceLists.updatePiece(originPiece, originSquare, targetSquare);
			me.position -= Piece.PIECE_SQUARE_TABLES[originPiece][originSquare];
			me.position += Piece.PIECE_SQUARE_TABLES[originPiece][targetSquare];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[originPiece][originSquare];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[originPiece][targetSquare];
		}
		// Switch colour bitboard
		if (isWhite) {
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
			// Replace captured piece in incremental material update, at the correct capture square
			addMaterialAndPositionForReplacedCapture(targetPiece, capturedPieceSquare);
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// check material incrementally updated against from scratch
			PiecewiseEvaluation scratch_me = new PiecewiseEvaluation();
			evaluateMaterial(scratch_me);
			assert scratch_me != me;
			assert scratch_me.material == me.material;
			assert scratch_me.position == me.position;
			assert scratch_me.positionEndgame == me.positionEndgame;
			assert scratch_me.phase == me.phase;
		}
		
		return capturedPieceSquare;
	}
	
	private void subtractMaterialAndPositionForCapture(int currPiece, int atPos) {
		me.numberOfPieces[currPiece]--;
		me.material -= Piece.PIECE_TO_MATERIAL_LUT[currPiece];
		me.position -= Piece.PIECE_SQUARE_TABLES[currPiece][atPos];
		me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[currPiece][atPos];
		me.phase += Piece.PIECE_PHASE[currPiece];
	}
	
	private void addMaterialAndPositionForReplacedCapture(int currPiece, int atPos) {
		me.numberOfPieces[currPiece]++;
		me.material += Piece.PIECE_TO_MATERIAL_LUT[currPiece];
		me.position += Piece.PIECE_SQUARE_TABLES[currPiece][atPos];
		me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[currPiece][atPos];
		me.phase -= Piece.PIECE_PHASE[currPiece];
	}
	
	private void updateMaterialAndPositionForDoingPromotion(int promoPiece, int oldPos, int newPos) {
		int pawnToRemove = (promoPiece & Piece.BLACK)+Piece.PAWN;
		me.numberOfPieces[pawnToRemove]--;
		me.numberOfPieces[promoPiece]++;
		
		me.material -= Piece.PIECE_TO_MATERIAL_LUT[pawnToRemove];
		me.material += Piece.PIECE_TO_MATERIAL_LUT[promoPiece];
		
		me.position -= Piece.PIECE_SQUARE_TABLES[pawnToRemove][oldPos];
		me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[pawnToRemove][oldPos];
		me.position += Piece.PIECE_SQUARE_TABLES[promoPiece][newPos];
		me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[promoPiece][newPos];
		
		me.phase -= Piece.PIECE_PHASE[promoPiece];
	}
	
	private void updateMaterialAndPositionForUndoingPromotion(int promoPiece, int oldPos, int newPos) {
		int pawnToReplace = (promoPiece & Piece.BLACK)+Piece.PAWN;
		me.numberOfPieces[pawnToReplace]++;
		me.numberOfPieces[promoPiece]--;
		
		me.material += Piece.PIECE_TO_MATERIAL_LUT[pawnToReplace];
		me.material -= Piece.PIECE_TO_MATERIAL_LUT[promoPiece];
		
		me.position += Piece.PIECE_SQUARE_TABLES[pawnToReplace][newPos];
		me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[pawnToReplace][newPos];
		me.position -= Piece.PIECE_SQUARE_TABLES[promoPiece][oldPos];
		me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[promoPiece][oldPos];
		
		me.phase += Piece.PIECE_PHASE[promoPiece];
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
		if (Piece.isPawn(originPiece)) {
			if (Position.getRank(originSquare) == IntRank.R2) {
				if (Position.getRank(targetSquare) == IntRank.R4) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetSquare-16);
				}
			} else if (Position.getRank(originSquare) == IntRank.R7) {
				if (Position.getRank(targetSquare) == IntRank.R5) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetSquare+16);
				}
			}
		}
		return isEnPassantCapturePossible;
	}
	
	private int getKingPosition(boolean isWhite) {
		return pieceLists.getKingPos(isWhite);
	}
	
	public boolean moveCouldLeadToOwnKingDiscoveredCheck(int move, int kingPosition) {
		// Establish if the initial square is on a multiple square slider mask from the king position
		int atSquare = Move.getOriginPosition(move);
		long square = BitBoard.positionToMask_Lut[atSquare];
		long attackingSquares = SquareAttackEvaluator.directAttacksOnPosition_Lut[kingPosition];
		return ((square & attackingSquares) != 0);
	}
	
    /*
    Don't need to update promotions or piece lists or to track material, as this change is only being done to determine if a move is illegal (i.e. would result in check).
    Only do those operations that bear on this, and such that are needed to preserve state.
    */
	public boolean isIllegalMove(int move, boolean needToEscapeMate) {
		int pieceToMove = Move.getOriginPiece(move);
		boolean isWhite = Piece.isWhite(pieceToMove);
		return isIllegalCheckHelper(move, needToEscapeMate, pieceToMove, isWhite);
	}
	
	private boolean isIllegalCheckHelper(int move, boolean needToEscapeMate, int pieceToMove, boolean isWhite) {
		boolean isIllegal = false;
		boolean isKing = Piece.isKing(pieceToMove);
		int kingPosition = getKingPosition(isWhite);
		if (needToEscapeMate || isKing || moveCouldLeadToOwnKingDiscoveredCheck(move, kingPosition)) {
		
			int capturePosition = Position.NOPOSITION;
			int originSquare = Move.getOriginPosition(move);
			int targetSquare = Move.getTargetPosition(move);
			int targetPiece = Move.getTargetPiece(move);
			boolean isCapture = targetPiece != Piece.NONE;
			long initialSquareMask = BitBoard.positionToMask_Lut[originSquare];
			long targetSquareMask = BitBoard.positionToMask_Lut[targetSquare];
			long positionsMask = initialSquareMask | targetSquareMask;
			
			long pieceToPickUp = Position.NOPOSITION;
			
			if (isCapture) {
				// Handle captures
				if (Move.isEnPassantCapture(move)) {
					// Handle en passant captures, don't need to do other checks in this case
					capturePosition = generateCapturePositionForEnPassant(pieceToMove, targetSquare);
				} else {
					capturePosition = targetSquare;
				}
				pieceToPickUp = BitBoard.positionToMask_Lut[capturePosition];
				// Remove from relevant colour bitboard
				if (isWhite) {
					blackPieces &= ~pieceToPickUp;
				} else {
					whitePieces &= ~pieceToPickUp;
				}
				// remove from specific bitboard
				pieces[targetPiece & Piece.PIECE_NO_COLOUR_MASK] &= ~pieceToPickUp;
				// Remove from all pieces bitboard
				allPieces &= ~pieceToPickUp;
			}
			
			// Simplification don't consider promotions between piece-specific bitboards and piece lists
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] ^= positionsMask;
			// Switch colour bitboard
			if (isWhite) {
				whitePieces ^= positionsMask;
			} else {
				blackPieces ^= positionsMask;
			}
			// Switch all pieces bitboard
			allPieces ^= positionsMask;
			// Because of need to check if in check, need to update for King only
			if (isKing) {
				pieceLists.updatePiece(pieceToMove, originSquare, targetSquare);
				kingPosition = targetSquare; // King moved!
			}
			
			isIllegal = squareIsAttacked(kingPosition, isWhite);
			
			// Switch piece bitboard
			// Piece type doesn't change across boards
			pieces[Piece.PIECE_NO_COLOUR_MASK & pieceToMove] ^= positionsMask;
			// Switch colour bitboard
			if (isWhite) {
				whitePieces ^= positionsMask;
			} else {
				blackPieces ^= positionsMask;
			}
			// Switch all pieces bitboard
			allPieces ^= positionsMask;
			// Because of need to check if in check, need to update for King only
			if (isKing) {
				pieceLists.updatePiece(pieceToMove, targetSquare, originSquare);
			}
			// Undo any capture that had been previously performed.
			if (isCapture) {
				// Set on piece-specific bitboard
				pieces[targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= pieceToPickUp;
				// Set on colour bitboard
				if (isWhite) {
					blackPieces |= pieceToPickUp;
				} else {
					whitePieces |= pieceToPickUp;
				}
				// Set on all pieces bitboard
				allPieces |= pieceToPickUp;
			}
		}
		return isIllegal;
	}
	
	public class PseudoPlayableMoveChecker implements IAddMoves {
		
		// Note test for legality is not performed by this class, that is a subsequent check
		
		int moveToCheckIsPlayable = Move.NULL_MOVE;
		boolean moveIsPlayable = false;
		boolean moveToCheckIsPromotion = false;
				
		private void testMove(int move) {
			if (Move.areEqualForBestKiller(move, moveToCheckIsPlayable)) {
				moveIsPlayable = true;
			} else if (moveToCheckIsPromotion && Move.isQueenPromotion(move)) {
				// An under promotion is always playable if the queen promotion is playable
				moveIsPlayable = true;
			}
		}
		public void addPrio(int move) {
			testMove(move);
		}
		
		public void addNormal(int move) {
			testMove(move);
		}
		
		public boolean isPlayableMoveFound() {
			return moveIsPlayable;
		}

		public void setup(int move) {
			moveIsPlayable = false;
			moveToCheckIsPlayable = move;
			moveToCheckIsPromotion = Move.isPromotion(move);
		}

		public boolean isLegalMoveFound() { return false; }
		
		public void clearAttackedCache() {}
	}
	
	PseudoPlayableMoveChecker pmc = new PseudoPlayableMoveChecker();
	
	public boolean isPlayableMove(int move, boolean needToEscapeMate, CastlingManager castling) {
		int pieceToMove = Move.getOriginPiece(move);
		int originSquare = Move.getOriginPosition(move);
		int targetSquare = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
		
		if (getPieceAtSquare(originSquare) != pieceToMove) {
			return false;
		}
		if (getPieceAtSquare(targetSquare) != targetPiece && !Move.isEnPassantCapture(move)) {
			return false;
		}
		if (Move.isEnPassantCapture(move) && (getEnPassantTargetSq() != targetSquare)) {
			return false;
		}
		
		boolean isWhite = Piece.isWhite(pieceToMove);
		
		// Check move can be made, i.e. it isn't blocked Pawn two square, slider
		pmc.setup(move);
		switch (Piece.PIECE_NO_COLOUR_MASK & pieceToMove) {
		case Piece.KING:
			if (castling.isCastlingMove(move)) {
				if (!needToEscapeMate) {
					castling.addCastlingMoves(isWhite, pmc);
				}
			} else {
				// It is enough to rely on the checks before the switch if not a castling move
				pmc.moveIsPlayable = true;
			}
			break;
		case Piece.QUEEN:
			Piece.queen_generateMoves(pmc, this, originSquare, isWhite);
			break;
		case Piece.ROOK:
			Piece.rook_generateMoves(pmc, this, originSquare, isWhite);
			break;
		case Piece.BISHOP:
			Piece.bishop_generateMoves(pmc, this, originSquare, isWhite);
			break;
		case Piece.KNIGHT:
			pmc.moveIsPlayable = true;
			break;
		case Piece.PAWN:
			if ((Position.getRank(originSquare) == (isWhite ? IntRank.R2 : IntRank.R7))) {
				// two square pawn moves need to be checked if intermediate square is empty
				int checkSquare = isWhite ? originSquare+16: originSquare-16;
				pmc.moveIsPlayable = squareIsEmpty(checkSquare);
			} else {
				pmc.moveIsPlayable = true;
			}
			break;
		}
		if (!pmc.isPlayableMoveFound()) {
			return false;
		}
		
		// It is valid, unless illegal
		return !isIllegalCheckHelper(move, needToEscapeMate, pieceToMove, isWhite);
	}
	
	private static final long wksc_mask = BitBoard.positionToMask_Lut[Position.h1] | BitBoard.positionToMask_Lut[Position.f1];
	private static final long wqsc_mask = BitBoard.positionToMask_Lut[Position.a1] | BitBoard.positionToMask_Lut[Position.d1];
	private static final long bksc_mask = BitBoard.positionToMask_Lut[Position.h8] | BitBoard.positionToMask_Lut[Position.f8];
	private static final long bqsc_mask = BitBoard.positionToMask_Lut[Position.a8] | BitBoard.positionToMask_Lut[Position.d8];
	
	private void performSecondaryCastlingMove(int move) {
		if (Move.areEqual(move, CastlingManager.wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.h1, Position.f1);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.h1];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.f1];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.h1];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.f1];
		} else if (Move.areEqual(move, CastlingManager.wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.a1, Position.d1);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.a1];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.d1];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.a1];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.d1];
		} else if (Move.areEqual(move, CastlingManager.bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.h8, Position.f8);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.h8];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.f8];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.h8];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.f8];
		} else if (Move.areEqual(move, CastlingManager.bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.a8, Position.d8);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.a8];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.d8];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.a8];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.d8];
		}
	}
	
	private void unperformSecondaryCastlingMove(int move) {
		if (Move.areEqual(move, CastlingManager.undo_wksc)) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.f1, Position.h1);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.f1];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.h1];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.f1];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.h1];
		} else if (Move.areEqual(move, CastlingManager.undo_wqsc)) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			pieceLists.updatePiece(Piece.WHITE_ROOK, Position.d1, Position.a1);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.d1];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.a1];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.d1];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.WHITE_ROOK][Position.a1];
		} else if (Move.areEqual(move, CastlingManager.undo_bksc)) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.f8, Position.h8);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.f8];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.h8];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.f8];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.h8];
		} else if (Move.areEqual(move, CastlingManager.undo_bqsc)) {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			pieceLists.updatePiece(Piece.BLACK_ROOK, Position.d8, Position.a8);
			me.position -= Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.d8];
			me.position += Piece.PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.a8];
			me.positionEndgame -= Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.d8];
			me.positionEndgame += Piece.ENDGAME_PIECE_SQUARE_TABLES[Piece.BLACK_ROOK][Position.a8];
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
	
	public boolean squareIsAttacked( int atPos, boolean isBlackAttacking ) {
		return SquareAttackEvaluator.isAttacked(this, atPos, isBlackAttacking);
	}
	
	public int getPieceAtSquare( int atPos ) {
		int type = Piece.NONE;
		long pieceToGet = BitBoard.positionToMask_Lut[atPos];;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
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
	
	public int getPieceAtSquareIfEnemy( int atPos, boolean ownSideIsWhite ) {
		int type = Piece.NONE;
		long pieceToGet = BitBoard.positionToMask_Lut[atPos];
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				if (!ownSideIsWhite) return Piece.DONT_CARE;
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
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
		if (EubosEngineMain.ENABLE_ASSERTS) {
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
	
	public boolean isKingInCheck(boolean isWhite) {
		int kingSquare = getKingPosition(isWhite);
		return squareIsAttacked(kingSquare, isWhite);
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
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert false : String.format("Non-existant target piece %c at %s", Piece.toFenChar(piece), Position.toGenericPosition(atPos));
			}
			piece = Piece.NONE;
		}
		return piece;
	}
	
	public int countDoubledPawnsForSide(Colour side) {
		int doubledCount = 0;
		long pawns = Colour.isWhite(side) ? getWhitePawns() : getBlackPawns();
		for (int file : IntFile.values) {
			long mask = BitBoard.FileMask_Lut[file];
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
		long mask = BitBoard.PassedPawn_Lut[side.ordinal()][atPos];
		long otherSidePawns = Colour.isWhite(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & otherSidePawns) != 0) {
			isPassed  = false;
		}
		return isPassed;
	}
	
	public boolean isBackwardsPawn(int atPos, Colour side) {
		boolean isBackwards = true;
		long mask = BitBoard.BackwardsPawn_Lut[side.ordinal()][atPos];
		long ownSidePawns = Colour.isBlack(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & ownSidePawns) != 0) {
			isBackwards  = false;
		}
		return isBackwards;
	}
	
	public boolean isIsolatedPawn(int atPos, Colour side) {
		boolean isIsolated = true;
		long mask = BitBoard.IsolatedPawn_Lut[atPos];
		long ownSidePawns = Colour.isBlack(side) ? getBlackPawns() : getWhitePawns();
		if ((mask & ownSidePawns) != 0) {
			isIsolated  = false;
		}
		return isIsolated;
	}
	
	class allPiecesOnBoardIterator implements PrimitiveIterator.OfInt {	
		private int[] pieces = null;
		private int count = 0;
		private int next = 0;

		allPiecesOnBoardIterator()  {
			pieces = new int[64];
			buildIterList(allPieces);
		}

		allPiecesOnBoardIterator( int typeToIterate )  {
			pieces = new int[64];
			long bitBoardToIterate;
			if (typeToIterate == Piece.WHITE_PAWN) {
				bitBoardToIterate = getWhitePawns();
			} else if (typeToIterate == Piece.BLACK_PAWN) {
				bitBoardToIterate = getBlackPawns();
			} else {
				bitBoardToIterate = 0x0;
			}
			buildIterList(bitBoardToIterate);
		}

		private void buildIterList(long bitBoardToIterate) {
			PrimitiveIterator.OfInt iter = BitBoard.iterator(bitBoardToIterate);
			while (iter.hasNext()) {
				int bit_index = iter.nextInt();
				pieces[count++] = BitBoard.bitToPosition_Lut[bit_index];
			}
		}	

		public boolean hasNext() {
			return next < pieces.length && next < count;
		}

		public Integer next() {
			assert false; // should always use nextInt()
			return pieces[next++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
		}

		@Override
		public int nextInt() {
			return pieces[next++];
		}
	}

	public PrimitiveIterator.OfInt iterator() {
		// default iterator returns all the pieces on the board, not all positions
		return new allPiecesOnBoardIterator( );
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
	
	public long getBlackDiagonal() {
		return blackPieces & (pieces[INDEX_QUEEN] | pieces[INDEX_BISHOP]);
	}
	
	public long getBlackRankFile() {
		return blackPieces & (pieces[INDEX_QUEEN] | pieces[INDEX_ROOK]);
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
	
	public long getWhiteDiagonal() {
		return whitePieces & (pieces[INDEX_QUEEN] | pieces[INDEX_BISHOP]);
	}
	
	public long getWhiteRankFile() {
		return whitePieces & (pieces[INDEX_QUEEN] | pieces[INDEX_ROOK]);
	}
	
	public boolean isOnHalfOpenFile(GenericPosition atPos, int type) {
		boolean isHalfOpen = false;
		long fileMask = BitBoard.FileMask_Lut[IntFile.valueOf(atPos.file)];
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
	
	class PawnAttackAggregator implements IForEachPieceCallback {
		long attackMask = 0L;
		boolean attackerIsBlack = false;
		
		public void callback(int piece, int position) {
			attackMask |= (attackerIsBlack ?
					SquareAttackEvaluator.BlackPawnAttacksFromPosition_Lut[position] : 
						SquareAttackEvaluator.WhitePawnAttacksFromPosition_Lut[position]);
		}
		
		public long getPawnAttacks(boolean attackerIsBlack) {
			this.attackerIsBlack = attackerIsBlack;
			this.attackMask = 0L;
			forEachPawnOfSide(this, attackerIsBlack);
			return attackMask;
		}
	}
	
	public class PawnKnightAttackAggregator implements IForEachPieceCallback {
		
		public final int[] BLACK_ATTACKERS = {Piece.BLACK_PAWN, Piece.BLACK_KNIGHT};
		public final int[] WHITE_ATTACKERS = {Piece.WHITE_PAWN, Piece.WHITE_KNIGHT};
		
		long attackMask = 0L;
		
		public void callback(int piece, int position) {
			long mask = 0L;
			switch(piece) {
			case Piece.WHITE_PAWN:
				mask = SquareAttackEvaluator.WhitePawnAttacksFromPosition_Lut[position];
				break;
			case Piece.BLACK_PAWN:
				mask = SquareAttackEvaluator.BlackPawnAttacksFromPosition_Lut[position];
				break;
			case Piece.WHITE_KNIGHT:
			case Piece.BLACK_KNIGHT:
				mask = SquareAttackEvaluator.KnightMove_Lut[position];
				break;
			default:
				break;
			}
			attackMask |= mask;
		}
		
		public long getAttacks(boolean attackerIsBlack) {
			attackMask = 0L;
			pieceLists.forEachPieceOfTypeDoCallback(this, attackerIsBlack ? BLACK_ATTACKERS: WHITE_ATTACKERS);
			return attackMask;
		}
	}
	
	public void getRegularPieceMoves(IAddMoves ml, boolean ownSideIsWhite, boolean captures) {
		if (me.isEndgame()) {
			pieceLists.addMovesEndgame(ml, ownSideIsWhite, captures);
		} else {
			pieceLists.addMovesMiddlegame(ml, ownSideIsWhite, captures);
		}
	}
	
	public void getPawnPromotionMovesForSide(IAddMoves ml, boolean isWhite) {
		pieceLists.addMoves_PawnPromotions(ml, isWhite);
	}
	
	public void getCapturesExcludingPromotions(IAddMoves ml, boolean isWhite) {
		pieceLists.addMoves_CapturesExcludingPawnPromotions(ml, isWhite);
	}
	
	public class LegalMoveChecker implements IAddMoves {
		
		boolean legalMoveFound = false;
				
		public void addPrio(int move) {
			if (!isIllegalMove(move, true)) {
				legalMoveFound = true;
			}
		}
		
		public void addNormal(int move) {
			assert false;
		}
		
		public boolean isLegalMoveFound() {
			return legalMoveFound;
		}

		public void clearAttackedCache() {
		}
	}
	
	LegalMoveChecker lmc = new LegalMoveChecker();
	
	public boolean validPriorityMoveExists(boolean ownSideIsWhite) {
		boolean legalMoveExists = false;
		lmc.legalMoveFound = false;
		legalMoveExists = pieceLists.validCaptureMoveExists(lmc, ownSideIsWhite);
		return legalMoveExists;
	}
	
	public void evaluateMaterial(PiecewiseEvaluation the_me) {
		pieceLists.evaluateMaterialBalanceAndStaticPieceMobility(true, the_me);
		pieceLists.evaluateMaterialBalanceAndStaticPieceMobility(false, the_me);
		the_me.setPhase();
	}
	
	int calculateDiagonalMobility(long bishops, long queens) {
		long empty = ~allPieces;
		int mobility_score = 0x0;
		long diagonal_sliders = bishops | queens;
		if (queens != 0) {
			if (bishops != 0) {
				long mobility_mask_1 = BitBoard.downLeftOccludedEmpty(diagonal_sliders, empty) ^ diagonal_sliders;
				long mobility_mask_2 = BitBoard.upRightOccludedEmpty(diagonal_sliders, empty) ^ diagonal_sliders;
				if ((mobility_mask_1 & mobility_mask_2) == 0x0) {
					mobility_score = Long.bitCount(mobility_mask_1 | mobility_mask_2);
				} else {
					mobility_score = Long.bitCount(mobility_mask_1) + Long.bitCount(mobility_mask_2);
				}
				mobility_mask_1 = BitBoard.downRightOccludedEmpty(diagonal_sliders, empty) ^ diagonal_sliders;
				mobility_mask_2 = BitBoard.upLeftOccludedEmpty(diagonal_sliders, empty) ^ diagonal_sliders;
				if ((mobility_mask_1 & mobility_mask_2) == 0x0) {
					mobility_score += Long.bitCount(mobility_mask_1 | mobility_mask_2);
				} else {
					mobility_score += Long.bitCount(mobility_mask_1) + Long.bitCount(mobility_mask_2);
				}
			} else {
				// Assume that if it is just queens, then material is so unbalanced that it doesn't matter that they can intersect
				long mobility_mask = BitBoard.downLeftOccludedEmpty(diagonal_sliders, empty);
				mobility_mask |= BitBoard.upRightOccludedEmpty(diagonal_sliders, empty);
				mobility_mask |= BitBoard.downRightOccludedEmpty(diagonal_sliders, empty);
				mobility_mask |= BitBoard.upLeftOccludedEmpty(diagonal_sliders, empty);
				mobility_score = Long.bitCount(mobility_mask ^ diagonal_sliders);
			}
		} else if (bishops != 0) {
			// Assume that if it is just bishops, they can't intersect, which allows optimisation
			if (diagonal_sliders != 0) {
				long mobility_mask = BitBoard.downLeftOccludedEmpty(diagonal_sliders, empty);
				mobility_mask |= BitBoard.upRightOccludedEmpty(diagonal_sliders, empty);
				mobility_mask |= BitBoard.downRightOccludedEmpty(diagonal_sliders, empty);
				mobility_mask |= BitBoard.upLeftOccludedEmpty(diagonal_sliders, empty);
				mobility_score = Long.bitCount(mobility_mask ^ diagonal_sliders);
			}
		}
		return mobility_score;
	}
	
	int calculateRankFileMobility(long rooks, long queens) {
		long empty = ~allPieces;
		int mobility_score = 0x0;
		long rank_file_sliders = rooks | queens;
		if (rooks != 0) {
			long mobility_mask_1 = BitBoard.leftOccludedEmpty(rank_file_sliders, empty) ^ rank_file_sliders;
			long mobility_mask_2 = BitBoard.rightOccludedEmpty(rank_file_sliders, empty) ^ rank_file_sliders;
			if ((mobility_mask_1 & mobility_mask_2) == 0x0) {
				mobility_score = Long.bitCount(mobility_mask_1 | mobility_mask_2);
			} else {
				mobility_score = Long.bitCount(mobility_mask_1) + Long.bitCount(mobility_mask_2);
			}
			
			mobility_mask_1 = BitBoard.upOccludedEmpty(rank_file_sliders, empty) ^ rank_file_sliders;
			mobility_mask_2 = BitBoard.downOccludedEmpty(rank_file_sliders, empty) ^ rank_file_sliders;
			if ((mobility_mask_1 & mobility_mask_2) == 0x0) {
				mobility_score += Long.bitCount(mobility_mask_1 | mobility_mask_2);
			} else {
				mobility_score += Long.bitCount(mobility_mask_1) + Long.bitCount(mobility_mask_2);
			}
		}
		else if (rank_file_sliders != 0) {
			// Assume single queen, as material likely unbalanced, this optimisation should be fine
			long mobility_mask = BitBoard.leftOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= BitBoard.rightOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= BitBoard.downOccludedEmpty(rank_file_sliders, empty);
			mobility_mask |= BitBoard.upOccludedEmpty(rank_file_sliders, empty);
			mobility_score = Long.bitCount(mobility_mask ^ rank_file_sliders);
		}
		return mobility_score;
	}
	
	public void calculateDynamicMobility(PiecewiseEvaluation me) {
		int mobility_score = 0x0;
		// White Bishop and Queen
		long white_queens = getWhiteQueens();
		mobility_score = calculateDiagonalMobility(getWhiteBishops(), white_queens);
		me.dynamicPosition += (short)(mobility_score*2);

		// White Rook and Queen
		mobility_score = calculateRankFileMobility(getWhiteRooks(), white_queens);
		me.dynamicPosition += (short)(mobility_score*2);
		
		// Black Bishop and Queen
		long black_queens = getBlackQueens();
		mobility_score = calculateDiagonalMobility(getBlackBishops(), black_queens);
		me.dynamicPosition -= (short)(mobility_score*2);
		
		// Black Rook and Queen
		mobility_score = calculateRankFileMobility(getBlackRooks(), black_queens);
		me.dynamicPosition -= (short)(mobility_score*2);
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
		int numWhiteBishops = me.numberOfPieces[Piece.WHITE_BISHOP];
		int numWhiteKnights = me.numberOfPieces[Piece.WHITE_KNIGHT];
		int numBlackBishops = me.numberOfPieces[Piece.BLACK_BISHOP];
		int numBlackKnights = me.numberOfPieces[Piece.BLACK_KNIGHT];
		
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
	
	private static final long LIGHT_SQUARES_MASK = 0x55AA55AA55AA55AAL;
	private static final long DARK_SQUARES_MASK = 0xAA55AA55AA55AA55L; 
	
	private static long[] knightKingSafetyMask_Lut = new long[128];
	static {
		for (int atPos : Position.values) {
			knightKingSafetyMask_Lut[atPos] = buildKnightAttacksForKingZone(atPos);
		}
	}
	private static long buildKnightAttacksForKingZone(int atPos) {
		long mask = 0;
		long kingZone = SquareAttackEvaluator.KingMove_Lut[atPos];
		for (int knightPos : Position.values) {
			long knightMask = SquareAttackEvaluator.KnightMove_Lut[knightPos];
			if ((knightMask & kingZone) != 0) {
				mask |= BitBoard.positionToMask_Lut[knightPos];
			}
		}
		return mask;
	}
	
	static final long[][][] emptySquareMask_Lut = new long[128][SquareAttackEvaluator.allDirect.length][];
	static {
		for (int square : Position.values) {
			int [][] forSqArray = SquareAttackEvaluator.directPieceMove_Lut[square];
			int j=0;
			for (int[] dir : forSqArray) {
				long [] mask = new long[dir.length];
				int i=0;
				for (int sq : dir) {
					mask[i++] = BitBoard.positionToMask_Lut[sq];
				}
				emptySquareMask_Lut[square][j++] = mask;
			}
		}
	}
	
	public int evaluateKingSafety(Piece.Colour side) {
		int evaluation = 0;
		boolean isWhite = Piece.Colour.isWhite(side);

		// King
		long kingMask = isWhite ? getWhiteKing() : getBlackKing();
		boolean isKingOnDarkSq = (kingMask & DARK_SQUARES_MASK) != 0;
		// Attackers
		long attackingQueensMask = isWhite ? getBlackQueens() : getWhiteQueens();
		long attackingRooksMask = isWhite ? getBlackRooks() : getWhiteRooks();
		long attackingBishopsMask = isWhite ? getBlackBishops() : getWhiteBishops();
		long attackingKnightsMask = isWhite ? getBlackKnights() : getWhiteKnights();

		// create masks of attackers
		long pertinentBishopMask = attackingBishopsMask & ((isKingOnDarkSq) ? DARK_SQUARES_MASK : LIGHT_SQUARES_MASK);
		long diagonalAttackersMask = attackingQueensMask | pertinentBishopMask;
		long rankFileAttackersMask = attackingQueensMask | attackingRooksMask;
		
		// First score according to King exposure on open diagonals
		int numPotentialAttackers = Long.bitCount(diagonalAttackersMask);
		int kingPos = pieceLists.getKingPos(isWhite);
		long mobility_mask = 0x0;
		if (numPotentialAttackers > 0) {
			long blockers = isWhite ? getWhitePawns() : getBlackPawns();
			long defendingBishopsMask = isWhite ? getWhiteBishops() : getBlackBishops();
			// only own side pawns should block an attack ray, not any piece, so don't use empty mask as propagator
			long inDirection = BitBoard.downLeftOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.upLeftOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.upRightOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.downRightOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			evaluation = Long.bitCount(mobility_mask ^ kingMask) * 2 * -numPotentialAttackers;
		}
		
		// Then score according to King exposure on open rank/files
		numPotentialAttackers = Long.bitCount(rankFileAttackersMask);
		if (numPotentialAttackers > 0) {
			mobility_mask = 0x0;
			long blockers = isWhite ? getWhitePawns() : getBlackPawns();
			long defendingRooksMask = isWhite ? getWhiteRooks() : getBlackRooks();
			long inDirection = BitBoard.downOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.upOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.rightOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.leftOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			evaluation += Long.bitCount(mobility_mask ^ kingMask) * 2 * -numPotentialAttackers;
		}
		
		// Then account for Knight proximity to the adjacent squares around the King
		long pertintentKnightsMask = attackingKnightsMask & knightKingSafetyMask_Lut[kingPos];
		evaluation += -8*Long.bitCount(pertintentKnightsMask);
			
		return evaluation;
	}
	
	public void forEachPiece(IForEachPieceCallback caller) {
		pieceLists.forEachPieceDoCallback(caller);
	}
	
	public void forEachPawnOfSide(IForEachPieceCallback caller, boolean isBlack) {
		pieceLists.forEachPawnOfSideDoCallback(caller, isBlack);
	}
	
	public long getEmpty() {
		return ~allPieces;
	}
}
