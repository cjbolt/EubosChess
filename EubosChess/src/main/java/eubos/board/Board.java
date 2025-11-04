package eubos.board;

import java.util.Map;
import java.util.Map.Entry;

import eubos.evaluation.NNUE;
import eubos.main.EubosEngineMain;
import eubos.position.CastlingManager;
import eubos.position.IAddMoves;
import eubos.position.IZobristUpdate;
import eubos.position.Move;
import eubos.position.Position;

import com.fluxchess.jcpi.models.IntRank;

public class Board {

	private IZobristUpdate hashUpdater;
	
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
	
	public long[] pieces = new long[7]; // N.b. INDEX_NONE is an empty long at index 0.
	public MaterialPhase me;
	public boolean insufficient = false;
	private long passedPawns = 0L;
	public NNUE nnue = new NNUE();
	
	public Board(Map<Integer, Integer> pieceMap) {
		allPieces = 0x0;
		whitePieces = 0x0;
		blackPieces = 0x0;
		for (int i=0; i<=INDEX_PAWN; i++) {
			pieces[i] = 0x0;
		}
		for (Entry<Integer, Integer> nextPiece : pieceMap.entrySet()) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue());
		}
		me = new MaterialPhase();
		evaluateMaterial(me);
		
		if (getWhiteKing() != 0 && getBlackKing() != 0) {
			// Initialise the accumulators for the position, if we aren't running a unit test
			NetInput input = populateNetInput();
			nnue.fullAccumulatorUpdate(input.white_pieces, input.white_squares, input.black_pieces, input.black_squares);
		}
		
		createPassedPawnsBoard();
		insufficient = isInsufficientMaterial();
	}
	
	public void createPassedPawnsBoard() {
		if (EubosEngineMain.ENABLE_ITERATIVE_PASSED_PAWN_UPDATE) {
			long pawns = getPawns(); 
			long scratchBitBoard = pawns;
			passedPawns = 0L;
			while ( scratchBitBoard != 0x0L ) {
				int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
				long bit_mask = 1L << bit_offset;
				if (isPassedPawn(bit_offset, bit_mask)) {
		    		// ...target square becomes pp for piece to move!
		    		passedPawns |= bit_mask;
		    	}
				// clear the lssb
				scratchBitBoard ^= bit_mask;
			}
		} else {
			passedPawns = 0L;
		}
	}
	
	private void evaluateMaterialBalanceAndStaticPieceMobility(boolean isWhite, MaterialPhase me) {
		int side = isWhite ? 0 : Piece.BLACK;
		long ownPieces = isWhite ? whitePieces : blackPieces; 
		int bitOffset = BitBoard.INVALID;
		long scratchBitBoard = pieces[Piece.QUEEN] & ownPieces;
		bitOffset = BitBoard.convertToBitOffset(scratchBitBoard);
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			me.numberOfPieces[side+Piece.QUEEN]++;
			me.phase -= MaterialPhase.QUEEN_PHASE;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			me.numberOfPieces[side+Piece.ROOK]++;
			me.phase -= MaterialPhase.ROOK_PHASE;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.BISHOP] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			me.numberOfPieces[side+Piece.BISHOP]++;
			me.phase -= MaterialPhase.PIECE_PHASE;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			me.numberOfPieces[side+Piece.KNIGHT]++;
			me.phase -= MaterialPhase.PIECE_PHASE;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert getPieceAtSquare(1L << bitOffset) != Piece.NONE :
					String.format("Found a Pawn at %s that isn't on Board", Position.toGenericPosition(bitOffset));
			}
			me.numberOfPieces[side+Piece.PAWN]++;
			scratchBitBoard ^= (1L << bitOffset);
		}
	}
	
	private void evaluateMaterial(MaterialPhase the_me) {
		me.phase = MaterialPhase.TOTAL_PHASE;
		evaluateMaterialBalanceAndStaticPieceMobility(true, the_me);
		evaluateMaterialBalanceAndStaticPieceMobility(false, the_me);
	}
	
	public String getAsFenString() {
		int currPiece = Piece.NONE;
		int spaceCounter = 0;
		StringBuilder fen = new StringBuilder();
		for (int rank=7; rank>=0; rank--) {
			for (int file=0; file<8; file++) {
				currPiece = getPieceAtSquare(BitBoard.positionToMask_Lut[Position.valueOf(file,rank)]);
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
	
	public boolean doMove(int move) {		
		int captureBitOffset = BitBoard.INVALID;
		
		// unload move
		int temp = move;
		int targetBitOffset = temp & 0x3F;
		temp >>>= 6;
		int originBitOffset = temp & 0x3F;
		temp >>>= 6;
		int promotedPiece = temp & 0x7;
		temp >>>= 4;
		int targetPiece = temp & 0xF;
		temp >>>= 4;
		int pieceToMove = temp & 0xF;
		temp >>>= 4;
		
		boolean isWhite = Piece.isWhite(pieceToMove);
		long initialSquareMask = 1L << originBitOffset;
		long targetSquareMask = 1L << targetBitOffset;
		long positionsMask = initialSquareMask | targetSquareMask;
		boolean isCapture = targetPiece != Piece.NONE;
		int pieceType = Piece.PIECE_NO_COLOUR_MASK & pieceToMove;
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// Check piece to move is on the bitboard, and is correct side
			assert (pieces[pieceType] & initialSquareMask) != 0: 
				String.format("Non-existant piece %s at %s for move %s", 
						Piece.toFenChar(pieceToMove), Position.toGenericPosition(BitBoard.bitToPosition_Lut[originBitOffset]), Move.toString(move));
			assert ((isWhite ? whitePieces : blackPieces) & initialSquareMask) != 0: 
				String.format("Piece %s not on colour board for move %s", 
						Piece.toFenChar(pieceToMove), Move.toString(move));
			assert (allPieces & initialSquareMask) != 0: 
				String.format("Piece %s not on all pieces board for move %s", 
						Piece.toFenChar(pieceToMove), Move.toString(move));
			assert (targetPiece & Piece.PIECE_NO_COLOUR_MASK) != Piece.DONT_CARE;
		}
		
		if (isCapture) {
			// Handle captures
			if (Move.isEnPassantCapture(move)) {
				// Handle en passant captures, don't need to do other checks in this case
				captureBitOffset = generateCaptureBitOffsetForEnPassant(pieceToMove, targetBitOffset);
			} else {
				captureBitOffset = targetBitOffset;
			}
			long captureMask = 1L << captureBitOffset;
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert (pieces[Piece.PIECE_NO_COLOUR_MASK & targetPiece] & captureMask) != 0: 
					String.format("Non-existant target piece %s at %s for move %s", 
							Piece.toFenChar(targetPiece), Position.toGenericPosition(captureBitOffset), Move.toString(move));
				assert ((isWhite ? blackPieces : whitePieces) & captureMask) != 0: 
					String.format("Piece %s not on colour board for move %s", 
							Piece.toFenChar(targetPiece), Move.toString(move));
				assert (allPieces & captureMask) != 0: 
					String.format("Piece %s not on all pieces board for move %s", 
							Piece.toFenChar(targetPiece), Move.toString(move));
				assert ((allPieces & captureMask) != 0) : String.format("Non-existant target piece %c at %s",
						Piece.toFenChar(targetPiece), Position.toGenericPosition(BitBoard.bitToPosition_Lut[captureBitOffset]));
			}
			// Remove from relevant colour bitboard
			if (isWhite) {
				blackPieces ^= captureMask;
			} else {
				whitePieces ^= captureMask;
			}
			// Remove from specific bitboard
			pieces[targetPiece & Piece.PIECE_NO_COLOUR_MASK] ^= captureMask;
			// Remove from all pieces bitboard
			allPieces ^= captureMask;
		}
		
		// Switch colour bitboard
		if (isWhite) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
		// Switch piece-specific bitboards and piece lists
		if (promotedPiece != Piece.NONE) {
			// For a promotion, need to resolve piece-specific across multiple bitboards
			pieces[INDEX_PAWN] ^= initialSquareMask;
			pieces[promotedPiece] |= targetSquareMask;
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard, pieceList and PST score
			pieces[pieceType] ^= positionsMask;
		}
		
		if (isKingInCheck(isWhite)) {
			// Switch piece bitboard
			if (promotedPiece != Piece.NONE) {
				// Remove promoted piece and replace it with a pawn
				pieces[promotedPiece] ^= targetSquareMask;	
				pieces[INDEX_PAWN] |= initialSquareMask;
			} else {
				pieces[pieceType] ^= positionsMask;
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
				long mask = 1L << captureBitOffset;
				pieces[targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= mask;
				if (isWhite) {
					blackPieces |= mask;
				} else {
					whitePieces |= mask;
				}
				allPieces |= mask;
				insufficient = false;
			}
			return true;
		}
		
		// Initialise En Passant target square
		setEnPassantTargetSq(BitBoard.INVALID);
		
		if (isCapture) {
			incrementallyUpdateStateForCapture(isWhite, targetPiece, captureBitOffset);
			insufficient = isInsufficientMaterial();
		} else {
			// Check whether the move sets the En Passant target square
			if (!moveEnablesEnPassantCapture(pieceToMove, originBitOffset, targetBitOffset)) {
				// Handle castling secondary rook moves...
				if (Move.isCastling(move)) {
					performSecondaryCastlingMove(targetBitOffset);
				}
			}
		}
		
		if (promotedPiece != Piece.NONE) {
			int fullPromotedPiece = (isWhite ? promotedPiece : promotedPiece|Piece.BLACK);
			passedPawns &= ~initialSquareMask;
			incrementallyUpdateStateForPromotion(isWhite, pieceToMove, fullPromotedPiece, originBitOffset, targetBitOffset);
		} else {
			hashUpdater.doBasicMove(targetBitOffset, originBitOffset, pieceToMove);
			updateAccumulatorsForBasicMove(isWhite, pieceToMove, originBitOffset, targetBitOffset);
			
			// Iterative update of passed pawns bitboard
			// Note: this needs to be done after the piece bit boards are updated
			// build up significant file masks, should be three or four consecutive files, re-evaluate passed pawns in those files
			if (EubosEngineMain.ENABLE_ITERATIVE_PASSED_PAWN_UPDATE) {
				long file_masks = 0L;
				if (pieceType == Piece.PAWN) {
					int ownLutColourIndex = isWhite ? 0 : 1;
					// Handle regular pawn pushes
					file_masks |= BitBoard.IterativePassedPawnNonCapture[ownLutColourIndex][originBitOffset];
					
					// Handle pawn captures
					if (targetPiece != Piece.NONE) {
						if (Piece.isPawn(targetPiece)) {
							// Pawn takes pawn, clears whole front-span of target pawn (note negation of colour)
							int enemyLutColourIndex = isWhite ? 1 : 0;
							file_masks |= BitBoard.PassedPawn_Lut[enemyLutColourIndex][targetBitOffset];
						}
						// manage file transition of capturing pawn moves
						boolean isLeft = BitBoard.getFile(targetBitOffset) < BitBoard.getFile(originBitOffset);
						file_masks |= BitBoard.IterativePassedPawnUpdateCaptures_Lut[originBitOffset][ownLutColourIndex][isLeft ? 0 : 1];
					}
				} else if (Piece.isPawn(targetPiece)) {
					// Piece takes pawn, potentially opens capture and adjacent files
					int enemyLutColourIndex = isWhite ? 1 : 0;
					file_masks |= targetSquareMask;
					file_masks |= BitBoard.PassedPawn_Lut[enemyLutColourIndex][targetBitOffset];
				} else {
					// doesn't need to be handled - can't change passed pawn bit board
				}
				if (file_masks != 0L) {
					// clear passed pawns in concerned files before re-evaluating
					// Note: vacated initial square
					passedPawns &= ~(initialSquareMask|file_masks);
					// re-evaluate
					long scratchBitBoard = getPawns() & file_masks;
					while ( scratchBitBoard != 0x0L ) {
						int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
						long pawn_mask = 1L << bit_offset;
						if (isPassedPawn(bit_offset, pawn_mask)) {
							passedPawns |= pawn_mask;
						}
						scratchBitBoard ^= pawn_mask;
					}
				}
			}
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (EubosEngineMain.ENABLE_ITERATIVE_PASSED_PAWN_UPDATE) {
				long iterativeUpdatePassedPawns = passedPawns;
				createPassedPawnsBoard();
				assert iterativeUpdatePassedPawns == passedPawns :
					String.format("Passed Pawns error iterative %s != scratch %s move = %s pawns = %s", 
						BitBoard.toString(iterativeUpdatePassedPawns), BitBoard.toString(passedPawns), 
						Move.toString(move), BitBoard.toString(this.getPawns()));
			}
			// Check piece bit boards to me num pieces consistency
			assert (me.numberOfPieces[Piece.WHITE_KNIGHT]+me.numberOfPieces[Piece.BLACK_KNIGHT]) == Long.bitCount(pieces[INDEX_KNIGHT]);
			assert (me.numberOfPieces[Piece.WHITE_BISHOP]+me.numberOfPieces[Piece.BLACK_BISHOP]) == Long.bitCount(pieces[INDEX_BISHOP]);
			assert (me.numberOfPieces[Piece.WHITE_ROOK]+me.numberOfPieces[Piece.BLACK_ROOK]) == Long.bitCount(pieces[INDEX_ROOK]);
			assert (me.numberOfPieces[Piece.WHITE_QUEEN]+me.numberOfPieces[Piece.BLACK_QUEEN]) == Long.bitCount(pieces[INDEX_QUEEN]);
			assert (me.numberOfPieces[Piece.WHITE_PAWN]+me.numberOfPieces[Piece.BLACK_PAWN]) == Long.bitCount(pieces[INDEX_PAWN]);
			assert Long.bitCount(pieces[INDEX_KING]) == 2;
			int old_score = nnue.old_evaluate(this, isWhite);
			int new_score = nnue.new_evaluate_for_assert(this, isWhite);
			assert old_score == new_score : String.format("old %d new %d", old_score, new_score);
		}
		
		return false;
	}
	
	public void undoMove(int moveToUndo) {
		// unload move
		int temp = moveToUndo;
		int originBitOffset = temp & 0x3F;
		temp >>>= 6;
		int targetBitOffset = temp & 0x3F;
		temp >>>= 6;
		int promotedPiece = temp & 0x7;
		temp >>>= 4; // Skip enP bit as well
		int targetPiece = temp & 0xF;
		temp >>>= 4;
		int originPiece = temp & 0xF;
		temp >>>= 4;
		
		boolean isWhite = Piece.isWhite(originPiece);
		long initialSquareMask = 1L << originBitOffset;
		long targetSquareMask = 1L << targetBitOffset;
		long positionsMask = initialSquareMask | targetSquareMask;
		boolean isCapture = targetPiece != Piece.NONE;
		int pieceType = Piece.PIECE_NO_COLOUR_MASK & originPiece;
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			long pieceMask = (promotedPiece != Piece.NONE) ? pieces[promotedPiece] : pieces[pieceType];
			assert (pieceMask & initialSquareMask) != 0: String.format("Non-existant piece at %s, %s",
					Position.toGenericPosition(BitBoard.bitToPosition_Lut[originBitOffset]), Move.toString(moveToUndo));
		}
		
		// Handle reversal of any castling secondary rook moves on the board
		if (Move.isCastling(moveToUndo)) {
			unperformSecondaryCastlingMove(originBitOffset);
		}
		// Switch piece bitboard
		if (promotedPiece != Piece.NONE) {
			// Remove promoted piece and replace it with a pawn
			pieces[promotedPiece] ^= initialSquareMask;	
			pieces[INDEX_PAWN] |= targetSquareMask;
			int fullPromotedPiece = (isWhite ? promotedPiece : promotedPiece|Piece.BLACK);
			me.updateWhenUndoingPromotion(fullPromotedPiece, originBitOffset, targetBitOffset);			
			updateAccumulatorsForPromotion(isWhite, originPiece, fullPromotedPiece, originBitOffset, targetBitOffset);
			
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard and accumulators
			pieces[pieceType] ^= positionsMask;			
			updateAccumulatorsForBasicMove(isWhite, originPiece, originBitOffset, targetBitOffset);
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
			int capturedPieceSquare = Move.isEnPassantCapture(moveToUndo) ? 
					generateCaptureBitOffsetForEnPassant(originPiece, originBitOffset) : originBitOffset;
			long mask = 1L << capturedPieceSquare;
			pieces[targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= mask;
			if (isWhite) {
				blackPieces |= mask;
			} else {
				whitePieces |= mask;
			}
			allPieces |= mask;
			incrementallyUpdateStateForUndoCapture(isWhite, targetPiece, capturedPieceSquare);			
			insufficient = false;
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			int old_score = nnue.old_evaluate(this, isWhite);
			int new_score = nnue.new_evaluate_for_assert(this, isWhite);
			assert old_score == new_score : String.format("old %d new %d", old_score, new_score);
		}
	}
	
	public int generateCaptureBitOffsetForEnPassant(int pieceToMove, int targetBitOffset) {
		if (pieceToMove == Piece.WHITE_PAWN) {
			targetBitOffset -= 8;
		} else if (pieceToMove == Piece.BLACK_PAWN){
			targetBitOffset += 8;
		}
		return targetBitOffset;
	}
	
	private boolean moveEnablesEnPassantCapture(int originPiece, int originBitOffset, int targetBitOffset) {
		boolean isEnPassantCapturePossible = false;
		if (Piece.isPawn(originPiece)) {
			if (BitBoard.getRank(originBitOffset) == IntRank.R2) {
				if (BitBoard.getRank(targetBitOffset) == IntRank.R4) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetBitOffset-8);
				}
			} else if (BitBoard.getRank(originBitOffset) == IntRank.R7) {
				if (BitBoard.getRank(targetBitOffset) == IntRank.R5) {
					isEnPassantCapturePossible = true;
					setEnPassantTargetSq(targetBitOffset+8);
				}
			}
		}
		return isEnPassantCapturePossible;
	}
	
	public int getKingPosition(boolean isWhite) {
		long king_mask = pieces[Piece.KING] & (isWhite ? whitePieces : blackPieces);
		return BitBoard.convertToBitOffset(king_mask);
	}
	
	public int getQueenPosition(boolean isWhite) {
		long queen_mask = pieces[Piece.QUEEN] & (isWhite ? whitePieces : blackPieces);
		return BitBoard.convertToBitOffset(queen_mask);
	}
	
	public boolean moveCausesDiscoveredCheck(int move, int kingBitOffset, boolean isWhite) {
		boolean isPinned = false;
		if (kingBitOffset == BitBoard.INVALID)
			return isPinned;
		
		int pinOffset = Move.getOriginPosition(move);
		long pinSquare = 1L << pinOffset;

		long diagonalAttacksOnKing = SquareAttackEvaluator.directDiagonalAttacksOnPosition_Lut[kingBitOffset];
		if ((pinSquare & diagonalAttacksOnKing) != 0L) {
			// We know that the pinned piece is on a diagonal with the king, but what if there is no attacker on the ray with king?
			long diagonalAttackersMask = isWhite ? getBlackDiagonal() : getWhiteDiagonal();
			if ((diagonalAttackersMask & diagonalAttacksOnKing) == 0L) return false;
			
			// What if the move is capturing the pinning piece? then it is no longer pinned...
			int targetBitOffset = Move.getTargetPosition(move);
			long targetMask = 1L << targetBitOffset;
			diagonalAttackersMask &= ~targetMask;
			// This isn't very good, it will only return here if there are no other diagonal attackers on other rays, we are only
			// really concerned with the attacker on the same ray as the pinned piece.
			if ((diagonalAttackersMask & diagonalAttacksOnKing) == 0L) return false;
			
			// temporarily move piece
			long enPassantCaptureMask = 0L;
			if (Move.isEnPassantCapture(move)) {
				// Handle en passant captures
				enPassantCaptureMask = 1L << generateCaptureBitOffsetForEnPassant(Move.getOriginPiece(move), targetBitOffset);
				allPieces &= ~enPassantCaptureMask;
			}
			allPieces &= ~pinSquare;
			allPieces |= targetMask;
			
			long kingMask = 1L << kingBitOffset;
			if (pinOffset > kingBitOffset) {
				// indicates either up left or upright direction
				long upLeftMask = SquareAttackEvaluator.directAttacksOnPositionUpLeft_Lut[kingBitOffset];			
				if ((upLeftMask & pinSquare) != 0L) {
					// Up left, is attacker on that line?
					if (((upLeftMask & targetMask) == 0L) && (upLeftMask & diagonalAttackersMask) != 0L) {
						isPinned = (diagonalAttackersMask & BitBoard.upLeftAttacks(kingMask, this.getEmpty())) != 0L;
					}
				} else {
					// must be up right
					long upRightMask = SquareAttackEvaluator.directAttacksOnPositionUpRight_Lut[kingBitOffset];
					if (((upRightMask & targetMask) == 0L) && (upRightMask & diagonalAttackersMask) != 0L) {
						isPinned = (diagonalAttackersMask & BitBoard.upRightAttacks(kingMask, this.getEmpty())) != 0L;
					}
				}
			} else {
				// indicates either down left or down right direction
				long downLeftMask = SquareAttackEvaluator.directAttacksOnPositionDownLeft_Lut[kingBitOffset];			
				if ((downLeftMask & pinSquare) != 0L) {
					// Down left, is attacker on that line?
					if (((downLeftMask & targetMask) == 0L) && (downLeftMask & diagonalAttackersMask) != 0L) {
						isPinned = (diagonalAttackersMask & BitBoard.downLeftAttacks(kingMask, this.getEmpty())) != 0L;
					}
				} else {
					// must be down right		
					long downRightMask = SquareAttackEvaluator.directAttacksOnPositionDownRight_Lut[kingBitOffset];
					if (((downRightMask & targetMask) == 0L) && (downRightMask & diagonalAttackersMask) != 0L) {
						isPinned = (diagonalAttackersMask & BitBoard.downRightAttacks(kingMask, this.getEmpty())) != 0L;
					}
				}
			}
			// replace piece
			if (!Move.isCapture(move) || enPassantCaptureMask != 0L) {
				allPieces &= ~targetMask;
			}
			allPieces |= enPassantCaptureMask;
			allPieces |= pinSquare;
		} else {
			long rankFileAttacksOnKing = SquareAttackEvaluator.directRankFileAttacksOnPosition_Lut[kingBitOffset];
			if ((pinSquare & rankFileAttacksOnKing) != 0L) {
				// We know that the pinned piece is on a rank file with the king
				long rankFileAttackersMask = isWhite ? getBlackRankFile() : getWhiteRankFile();
				if ((rankFileAttackersMask & rankFileAttacksOnKing) == 0L) return isPinned;
				
				// what if move is capturing the pinning piece? then it is ok
				int targetBitOffset = Move.getTargetPosition(move);
				long targetMask = 1L << targetBitOffset;
				rankFileAttackersMask &= ~targetMask;
				if ((rankFileAttackersMask & rankFileAttacksOnKing) == 0L) return isPinned;
				
				// temporarily move piece
				long enPassantCaptureMask = 0L;
				if (Move.isEnPassantCapture(move)) {
					// Handle en passant captures
					enPassantCaptureMask = 1L << generateCaptureBitOffsetForEnPassant(Move.getOriginPiece(move), targetBitOffset);
					allPieces &= ~enPassantCaptureMask;
				}
				allPieces &= ~pinSquare;
				allPieces |= targetMask;
				
				long kingMask = 1L << kingBitOffset;
				if (pinOffset > kingBitOffset) {
					// indicates either up or right direction
					long rightMask = SquareAttackEvaluator.directAttacksOnPositionRight_Lut[kingBitOffset];			
					if ((rightMask & pinSquare) != 0L) {
						if (((rightMask & targetMask) == 0L) && (rightMask & rankFileAttackersMask) != 0L) {
							isPinned = (rankFileAttackersMask & BitBoard.rightAttacks(kingMask, this.getEmpty())) != 0L;
						}
					} else {
						long upMask = SquareAttackEvaluator.directAttacksOnPositionUp_Lut[kingBitOffset];
						if (((upMask & targetMask) == 0L) && (upMask & rankFileAttackersMask) != 0L) {
							isPinned = (rankFileAttackersMask & BitBoard.upAttacks(kingMask, this.getEmpty())) != 0L;
						}
					}
				} else {
					// indicates either left or down direction
					long leftMask = SquareAttackEvaluator.directAttacksOnPositionLeft_Lut[kingBitOffset];			
					if ((leftMask & pinSquare) != 0L) {
						if (((leftMask & targetMask) == 0L) && (leftMask & rankFileAttackersMask) != 0L) {
							isPinned = (rankFileAttackersMask & BitBoard.leftAttacks(kingMask, this.getEmpty())) != 0L;
						}
					} else {
						// Down
						long downMask = SquareAttackEvaluator.directAttacksOnPositionDown_Lut[kingBitOffset];
						if (((downMask & targetMask) == 0L) && (downMask & rankFileAttackersMask) != 0L) {
							isPinned = (rankFileAttackersMask & BitBoard.downAttacks(kingMask, this.getEmpty())) != 0L;
						}
					}
				}
				// replace piece
				if (!Move.isCapture(move) || enPassantCaptureMask != 0L) {
					allPieces &= ~targetMask;
				}
				allPieces |= enPassantCaptureMask;
				allPieces |= pinSquare;
			}
		}
		return isPinned;
	}
	
	private static final long LIGHT_SQUARES_MASK = 0x55AA55AA55AA55AAL;
	private static final long DARK_SQUARES_MASK = 0xAA55AA55AA55AA55L; 
	
	public boolean moveCouldLeadToOwnKingDiscoveredCheck(int move, int kingBitOffset, boolean isWhite) {
		// Attackers
		long attackingQueensMask = isWhite ? getBlackQueens() : getWhiteQueens();
		long attackingRooksMask = isWhite ? getBlackRooks() : getWhiteRooks();
		long attackingBishopsMask = isWhite ? getBlackBishops() : getWhiteBishops();

		// Create masks of attackers
		boolean isKingOnDarkSq = ((1L << kingBitOffset) & DARK_SQUARES_MASK) != 0;
		long pertinentBishopMask = attackingBishopsMask & ((isKingOnDarkSq) ? DARK_SQUARES_MASK : LIGHT_SQUARES_MASK);
		long diagonalAttackersMask = attackingQueensMask | pertinentBishopMask;
		long rankFileAttackersMask = attackingQueensMask | attackingRooksMask;
		
		// Establish if the initial square is on a multiple square slider mask from the king position, for which there is a potential pin
		long pinSquare = 1L << Move.getOriginPosition(move);
		long diagonalAttacksOnKing = SquareAttackEvaluator.directDiagonalAttacksOnPosition_Lut[kingBitOffset];
		if ((diagonalAttackersMask & diagonalAttacksOnKing) != 0L) {
			if ((pinSquare & diagonalAttacksOnKing) != 0L) return true;
		}
		long rankFileAttacksOnKing = SquareAttackEvaluator.directRankFileAttacksOnPosition_Lut[kingBitOffset];
		if ((rankFileAttackersMask & rankFileAttacksOnKing) != 0L) {
			if ((pinSquare & rankFileAttacksOnKing) != 0L) return true;
		}
		return false;
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
	
	private void switchBitBoards(int originPiece, long positionsMask, boolean isWhite) {
		pieces[originPiece] ^= positionsMask;
		// Switch colour bitboard
		if (isWhite) {
			whitePieces ^= positionsMask;
		} else {
			blackPieces ^= positionsMask;
		}
		// Switch all pieces bitboard
		allPieces ^= positionsMask;
	}
	
	private void removeFromBitBoards(int targetPieceNoColour, long pieceToPickUp, boolean isWhite) {
		// Remove from relevant colour bitboard
		if (isWhite) {
			blackPieces &= ~pieceToPickUp;
		} else {
			whitePieces &= ~pieceToPickUp;
		}
		// remove from specific bitboard
		pieces[targetPieceNoColour] &= ~pieceToPickUp;
		// Remove from all pieces bitboard
		allPieces &= ~pieceToPickUp;
	}
	
	public void replaceOnBitBoards(int targetPieceNoColour, long pieceToPickUp, boolean isWhite) {
		pieces[targetPieceNoColour] |= pieceToPickUp;
		// Set on colour bitboard
		if (isWhite) {
			blackPieces |= pieceToPickUp;
		} else {
			whitePieces |= pieceToPickUp;
		}
		// Set on all pieces bitboard
		allPieces |= pieceToPickUp;
	}
	
	private long getCaptureSquareMask(int move, int pieceToMove, int targetBitOffset) {
		int captureBitOffset = BitBoard.INVALID;
		if (Move.isEnPassantCapture(move)) {
			// Handle en passant captures, don't need to do other checks in this case
			captureBitOffset = generateCaptureBitOffsetForEnPassant(pieceToMove, targetBitOffset);
		} else {
			captureBitOffset = targetBitOffset;
		}
		return 1L << captureBitOffset;
	}
	
	private boolean isIllegalCheckHelper(int move, boolean needToEscapeMate, int pieceToMove, boolean isWhite) {
		boolean isIllegal = false;
		int kingBitOffset = getKingPosition(isWhite);
		boolean isKingMoving = Piece.isKing(pieceToMove);
		boolean doCheck = needToEscapeMate || isKingMoving;
		if (EubosEngineMain.ENABLE_PINNED_TO_KING_CHECK_IN_ILLEGAL_DETECTION) {
			if (!doCheck && moveCausesDiscoveredCheck(move, kingBitOffset, isWhite))
				return true;
		} else {
			if (!doCheck) {
				doCheck = moveCouldLeadToOwnKingDiscoveredCheck(move, kingBitOffset, isWhite);
			}
		}
		if (doCheck) {		
			int originBitOffset = Move.getOriginPosition(move);
			int targetBitOffset = Move.getTargetPosition(move);
			int targetPieceNoColour = Move.getTargetPiece(move) & Piece.PIECE_NO_COLOUR_MASK;
			int originPieceNoColour = pieceToMove & Piece.PIECE_NO_COLOUR_MASK;
			boolean isCapture = targetPieceNoColour != Piece.NONE;
			long positionsMask = 1L << originBitOffset | 1L << targetBitOffset;
			long pieceToPickUp = BitBoard.INVALID;
			
			if (isCapture) {
				pieceToPickUp = getCaptureSquareMask(move, pieceToMove, targetBitOffset);
				removeFromBitBoards(targetPieceNoColour, pieceToPickUp, isWhite);
			}
			switchBitBoards(originPieceNoColour, positionsMask, isWhite);
			// Because of need to decide if in check, need to update when the King has moved only
			if (isKingMoving) {
				kingBitOffset = targetBitOffset; // King moved!
			}
			
			isIllegal = squareIsAttacked(kingBitOffset, isWhite);
			
			switchBitBoards(originPieceNoColour, positionsMask, isWhite);
			if (isCapture) {
				replaceOnBitBoards(targetPieceNoColour, pieceToPickUp, isWhite);
			}
		}
		return isIllegal;
	}
	
	public class PseudoPlayableMoveChecker implements IAddMoves {
		// Note test for legality is not performed by this class, that is a subsequent check
		int moveToCheckIsPlayable = Move.NULL_MOVE;
		boolean moveIsPlayable = false;
				
		private void testMove(int move) {
			if (Move.areEqual(move, moveToCheckIsPlayable)) {
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
		}

		public boolean isLegalMoveFound() { return false; }
		
		public void clearAttackedCache() {}
	}
	
	PseudoPlayableMoveChecker pmc = new PseudoPlayableMoveChecker();
	
	public boolean isPlayableMove(int move, boolean needToEscapeMate, CastlingManager castling) {
		int pieceToMove = Move.getOriginPiece(move);
		int originBitShift = Move.getOriginPosition(move);		
		if (getPieceAtSquare(1L << originBitShift) != pieceToMove) {
			return false;
		}
		
		int targetBitShift = Move.getTargetPosition(move);
		int targetPiece = Move.getTargetPiece(move);
		if (getPieceAtSquare(1L << targetBitShift) != targetPiece && !Move.isEnPassantCapture(move)) {
			return false;
		}
		
		boolean isWhite = Piece.isWhite(pieceToMove);
		// Check move can be made, i.e. it isn't a blocked Pawn two square move, blocked slider, blocked castling
		pmc.setup(move);
		switch (pieceToMove) {
		case Piece.WHITE_KING:
		case Piece.BLACK_KING:
			if (Move.isCastling(move)) {
				if (!needToEscapeMate) {
					castling.addCastlingMoves(isWhite, pmc);
				}
			} else {
				// It is enough to rely on the checks before the switch if not a castling move
				pmc.moveIsPlayable = true;
			}
			break;
		case Piece.WHITE_QUEEN:
			Piece.queen_checkMove_White(pmc, this, originBitShift, targetBitShift);
			break;
		case Piece.WHITE_ROOK:
			Piece.rook_checkMove_White(pmc, this, originBitShift, targetBitShift);
			break;
		case Piece.WHITE_BISHOP:
			Piece.bishop_checkMove_White(pmc, this, originBitShift, targetBitShift);
			break;
		case Piece.BLACK_QUEEN:
			Piece.queen_checkMove_Black(pmc, this, originBitShift, targetBitShift);
			break;
		case Piece.BLACK_ROOK:
			Piece.rook_checkMove_Black(pmc, this, originBitShift, targetBitShift);
			break;
		case Piece.BLACK_BISHOP:
			Piece.bishop_checkMove_Black(pmc, this, originBitShift, targetBitShift);
			break;
		case Piece.WHITE_KNIGHT:
		case Piece.BLACK_KNIGHT:
			pmc.moveIsPlayable = true;
			break;
		case Piece.WHITE_PAWN:
		case Piece.BLACK_PAWN:
			if (Move.isEnPassantCapture(move) && (getEnPassantTargetSq() != targetBitShift)) {
				pmc.moveIsPlayable = false;
			} else if ((BitBoard.getRank(originBitShift) == (isWhite ? IntRank.R2 : IntRank.R7)) &&
				(BitBoard.getRank(targetBitShift) == (isWhite ? IntRank.R4 : IntRank.R5))) {
				// two square pawn moves need to be checked if intermediate square is empty
				int checkAtOffset = isWhite ? originBitShift+8: originBitShift-8;
				pmc.moveIsPlayable = squareIsEmpty(1L << checkAtOffset);
			} else {
				pmc.moveIsPlayable = true;
			}
			break;
		}
		if (pmc.isPlayableMoveFound()) {
			return true; // It is valid, that is enough for now; if it is illegal this will be managed when move is tried
		}
		return false;
	}
	
	private static final long wksc_mask = BitBoard.positionToMask_Lut[Position.h1] | BitBoard.positionToMask_Lut[Position.f1];
	private static final long wqsc_mask = BitBoard.positionToMask_Lut[Position.a1] | BitBoard.positionToMask_Lut[Position.d1];
	private static final long bksc_mask = BitBoard.positionToMask_Lut[Position.h8] | BitBoard.positionToMask_Lut[Position.f8];
	private static final long bqsc_mask = BitBoard.positionToMask_Lut[Position.a8] | BitBoard.positionToMask_Lut[Position.d8];
	
	private void performSecondaryCastlingMove(int target) {
		if (target == BitBoard.g1) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			hashUpdater.doBasicMove(BitBoard.f1, BitBoard.h1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtract(convertPiece(Piece.WHITE_ROOK), BitBoard.h1, -1, BitBoard.h1);
			nnue.iterativeAccumulatorAdd(convertPiece(Piece.WHITE_ROOK), BitBoard.f1, -1, BitBoard.f1);
		} else if (target == BitBoard.c1) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			hashUpdater.doBasicMove(BitBoard.d1, BitBoard.a1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtract(convertPiece(Piece.WHITE_ROOK), BitBoard.a1, -1, BitBoard.a1);
			nnue.iterativeAccumulatorAdd(convertPiece(Piece.WHITE_ROOK), BitBoard.d1, -1, BitBoard.d1);
		} else if (target == BitBoard.g8) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			hashUpdater.doBasicMove(BitBoard.f8, BitBoard.h8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtract(-1, BitBoard.h8, convertPiece(Piece.BLACK_ROOK), BitBoard.h8);
			nnue.iterativeAccumulatorAdd(-1, BitBoard.f8, convertPiece(Piece.BLACK_ROOK), BitBoard.f8);
		} else {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			hashUpdater.doBasicMove(BitBoard.d8, BitBoard.a8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtract(-1, BitBoard.a8, convertPiece(Piece.BLACK_ROOK), BitBoard.a8);
			nnue.iterativeAccumulatorAdd(-1, BitBoard.d8, convertPiece(Piece.BLACK_ROOK), BitBoard.d8);
		}
	}
	
	private void unperformSecondaryCastlingMove(int origin) {
		if (origin == BitBoard.g1) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			hashUpdater.doBasicMove(BitBoard.h1, BitBoard.f1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtract(convertPiece(Piece.WHITE_ROOK), BitBoard.f1, -1, BitBoard.f1);
			nnue.iterativeAccumulatorAdd(convertPiece(Piece.WHITE_ROOK), BitBoard.h1, -1, BitBoard.h1);
		} else if (origin == BitBoard.c1) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			hashUpdater.doBasicMove(BitBoard.a1, BitBoard.d1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtract(convertPiece(Piece.WHITE_ROOK), BitBoard.d1, -1, BitBoard.d1);
			nnue.iterativeAccumulatorAdd(convertPiece(Piece.WHITE_ROOK), BitBoard.a1, -1, BitBoard.a1);
		} else if (origin == BitBoard.g8) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			hashUpdater.doBasicMove(BitBoard.h8, BitBoard.f8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtract(-1, BitBoard.f8, convertPiece(Piece.BLACK_ROOK), BitBoard.f8);
			nnue.iterativeAccumulatorAdd(-1, BitBoard.h8, convertPiece(Piece.BLACK_ROOK), BitBoard.h8);
		} else {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			hashUpdater.doBasicMove(BitBoard.a8, BitBoard.d8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtract(-1, BitBoard.d8, convertPiece(Piece.BLACK_ROOK), BitBoard.d8);
			nnue.iterativeAccumulatorAdd(-1, BitBoard.a8, convertPiece(Piece.BLACK_ROOK), BitBoard.a8);
		}
	}
	
	private int enPassantTargetSq = BitBoard.INVALID;
	public int getEnPassantTargetSq() {
		return enPassantTargetSq;
	}
	public void setEnPassantTargetSq(int enPassantTargetSq) {
		// TODO: add bounds checking - only certain en passant squares can be legal.
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert (enPassantTargetSq > BitBoard.h2 && enPassantTargetSq < BitBoard.a4) ||
				   (enPassantTargetSq > BitBoard.h5 && enPassantTargetSq < BitBoard.a7) ||
				   enPassantTargetSq == BitBoard.INVALID;
		}
		this.enPassantTargetSq = enPassantTargetSq;
	}
	
	public boolean squareIsEmpty(int bitOffset) {
		return (allPieces & (1L << bitOffset)) == 0;		
	}
	
	public boolean squareIsEmpty(long mask) {
		return (allPieces & mask) == 0;		
	}
	
	public boolean squareIsAttacked(int bitOffset, boolean isBlackAttacking) {
		return SquareAttackEvaluator.isAttacked(this, bitOffset, isBlackAttacking);
	}
	
	public int getPieceAtSquare(long pieceToGet) {
		int type = Piece.NONE;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert (whitePieces & pieceToGet) != 0;
			}
			type = helper(type, pieceToGet);
		}
		return type;
	}
	
	public int getPieceAtSquareEnemyWhite(long pieceToGet) {
		int type = Piece.NONE;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				return Piece.DONT_CARE;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert (whitePieces & pieceToGet) != 0;
			}
			type = helper(type, pieceToGet);
		}
		return type;
	}
	
	public int getPieceAtSquareEnemyBlack(long pieceToGet) {
		int type = Piece.NONE;
		if ((allPieces & pieceToGet) != 0) {	
			if ((blackPieces & pieceToGet) != 0) {
				type |= Piece.BLACK;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert (whitePieces & pieceToGet) != 0;
				return Piece.DONT_CARE;
			}
			type = helper(type, pieceToGet);
		}
		return type;
	}
	
	private int helper(int type, long pieceToGet) {
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
		return type;
	}
	
	public void setPieceAtSquare(int bitOffset, int pieceToPlace) {
		setPieceAtSquare(1L << bitOffset, bitOffset, pieceToPlace);
	}
	
	public void setPieceAtSquare(long mask, int bitOffset, int pieceToPlace) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert bitOffset != BitBoard.INVALID;
			assert pieceToPlace != Piece.NONE;
		}
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
		boolean inCheck = false;
		int kingBitOffset = getKingPosition(isWhite);
		if (kingBitOffset != BitBoard.INVALID) {
			inCheck = squareIsAttacked(kingBitOffset, isWhite);
		}
		return inCheck;
	}
	
	public void pickUpPieceAtSquare(long pieceToPickUp, int bitOffset, int piece) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert ((allPieces & pieceToPickUp) != 0) : String.format("Non-existant target piece %c at %s",
					Piece.toFenChar(piece), Position.toGenericPosition(BitBoard.bitToPosition_Lut[bitOffset]));
		}
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
	}
	
	public boolean isPassedPawn(int bitOffset, long bitMask) {
		boolean isPassed = true;
		boolean isWhite = (whitePieces & bitMask) != 0L;
		long mask = BitBoard.PassedPawn_Lut[isWhite ? 0 : 1][bitOffset];
		long otherSidePawns = isWhite ? getBlackPawns() : getWhitePawns();
		if ((mask & otherSidePawns) != 0) {
			isPassed  = false;
		}
		return isPassed;
	}
	
	public long getPawns() {
		return pieces[INDEX_PAWN];
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

	public boolean whiteSingleMoveMidgame(IAddMoves ml) {
		long side = whitePieces;
		long scratchBitBoard = pieces[Piece.BISHOP] & side;
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.bishop_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.knight_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.queen_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.rook_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R7]);;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.king_generateMoves_White(ml, this, bit_offset);
		}
		if (ml.isLegalMoveFound()) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public boolean blackSingleMoveMidgame(IAddMoves ml) {
		long side = blackPieces;
		long scratchBitBoard = pieces[Piece.BISHOP] & side;
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.bishop_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.knight_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.queen_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.rook_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R2]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
			if (ml.isLegalMoveFound()) {
				return true;
			}
		}
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			Piece.king_generateMoves_Black(ml, this, bit_offset);
		}
		if (ml.isLegalMoveFound()) {
			return true;
		} else {
			return false;
		}
	}
	
	public void whiteEndgame(IAddMoves ml) {
		long side = whitePieces;
		long scratchBitBoard = pieces[Piece.KING] & side;
		int bit_offset = BitBoard.INVALID;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			Piece.king_generateMoves_White(ml, this, bit_offset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R7]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.queen_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.rook_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.BISHOP] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.bishop_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.knight_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
	}
	
	public void blackEndgame(IAddMoves ml) {
		long side = blackPieces;
		long scratchBitBoard = pieces[Piece.KING] & side;
		int bit_offset = BitBoard.INVALID;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			Piece.king_generateMoves_Black(ml, this, bit_offset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R2]);;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.queen_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.rook_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.BISHOP] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.bishop_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.knight_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
	}
	
	public void whiteMidgame(IAddMoves ml) {
		long side = whitePieces;
		long scratchBitBoard = pieces[Piece.BISHOP] & side;
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.bishop_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.knight_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.queen_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.rook_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R7]);;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.king_generateMoves_White(ml, this, bit_offset);
		}
	}
	
	public void blackMidgame(IAddMoves ml) {
		long side = blackPieces;
		long scratchBitBoard = pieces[Piece.BISHOP] & side;
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.bishop_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.knight_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.queen_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.rook_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R2]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			Piece.king_generateMoves_Black(ml, this, bit_offset);
		}
	}
	
	public void getSingleQuietMove(IAddMoves ml, boolean ownSideIsWhite) {
		if (ownSideIsWhite) {
			whiteSingleMoveMidgame(ml);
		} else {
			blackSingleMoveMidgame(ml);
		}
	}
	
	public void getRegularPieceMoves(IAddMoves ml, boolean ownSideIsWhite) {
		if (ownSideIsWhite) {
			whiteMidgame(ml);
		} else {
			blackMidgame(ml);
		}
	}
	
	public void addMoves_PawnPromotions_White(IAddMoves ml) {
		long scratchBitBoard = pieces[Piece.PAWN] & whitePieces & BitBoard.RankMask_Lut[IntRank.R7];
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generatePromotionMoves_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
	}
	
	public void addMoves_PawnPromotions_Black(IAddMoves ml) {
		long scratchBitBoard = pieces[Piece.PAWN] & blackPieces & BitBoard.RankMask_Lut[IntRank.R2];
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generatePromotionMoves_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
	}
	
	public void getPawnPromotionMovesForSide(IAddMoves ml, boolean isWhite) {
		if (isWhite) {
			addMoves_PawnPromotions_White(ml);
		} else {
			addMoves_PawnPromotions_Black(ml);
		}
	}
	
	public void addMoves_CapturesExcludingPawnPromotions_White_Endgame(IAddMoves ml) {
		// Optimisations for generating move lists in extended search
		long opponentPieces = blackPieces;
		long side = whitePieces;
		long scratchBitBoard;
		int bit_offset = BitBoard.INVALID;
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
			if ((opponentPieces & kingAttacksMask) != 0) {
				Piece.king_generateMovesExtSearch_White(ml, this, bit_offset);
			}
		}
		// Only search pawn moves that cannot be a promotion
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R7]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {		
			Piece.pawn_generateMovesForExtendedSearch_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {	
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {
				Piece.queen_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {	
				Piece.rook_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.BISHOP] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {			
				Piece.bishop_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[bit_offset];
			if ((opponentPieces & knightAttacksMask) != 0) {
				Piece.knight_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
	}

	public void addMoves_CapturesExcludingPawnPromotions_White(IAddMoves ml) {
		// Optimisations for generating move lists in extended search
		long opponentPieces = blackPieces;
		long side = whitePieces;
		long scratchBitBoard;
		int bit_offset = BitBoard.INVALID;
		scratchBitBoard = pieces[Piece.BISHOP] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {			
				Piece.bishop_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[bit_offset];
			if ((opponentPieces & knightAttacksMask) != 0) {
				Piece.knight_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {
				Piece.queen_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {	
				Piece.rook_generateMovesExtSearch_White(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		// Only search pawn moves that cannot be a promotion
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R7]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMovesForExtendedSearch_White(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}		
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
			if ((opponentPieces & kingAttacksMask) != 0) {
				Piece.king_generateMovesExtSearch_White(ml, this, bit_offset);
			}
		}
	}
	
	public void addMoves_CapturesExcludingPawnPromotions_Black_Endgame(IAddMoves ml) {
		// Optimisations for generating move lists in extended search
		long opponentPieces = whitePieces;
		long side = blackPieces;
		long scratchBitBoard;
		int bit_offset = BitBoard.INVALID;
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {		
			long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
			if ((opponentPieces & kingAttacksMask) != 0) {
				Piece.king_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R2]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMovesForExtendedSearch_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {
				Piece.queen_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {	
				Piece.rook_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.BISHOP] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {			
				Piece.bishop_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[bit_offset];
			if ((opponentPieces & knightAttacksMask) != 0) {
				Piece.knight_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
	}
		
	public void addMoves_CapturesExcludingPawnPromotions_Black(IAddMoves ml) {
		long opponentPieces = whitePieces;
		long side = blackPieces;
		long scratchBitBoard;
		int bit_offset = BitBoard.INVALID;
		scratchBitBoard = pieces[Piece.BISHOP] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {			
				Piece.bishop_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long knightAttacksMask = SquareAttackEvaluator.KnightMove_Lut[bit_offset];
			if ((opponentPieces & knightAttacksMask) != 0) {
				Piece.knight_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {
				Piece.queen_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & side;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long attacksMask = SquareAttackEvaluator.directAttacksOnPosition_Lut[bit_offset];
			if ((opponentPieces & attacksMask) != 0) {	
				Piece.rook_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R2]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {	
			Piece.pawn_generateMovesForExtendedSearch_Black(ml, this, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
		}
		scratchBitBoard = pieces[Piece.KING] & side;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
			if ((opponentPieces & kingAttacksMask) != 0) {
				Piece.king_generateMovesExtSearch_Black(ml, this, bit_offset);
			}
		}
	}
	
	public void getCapturesExcludingPromotions(IAddMoves ml, boolean isWhite) {
		if (isWhite) {
			addMoves_CapturesExcludingPawnPromotions_White(ml);
		} else {
			addMoves_CapturesExcludingPawnPromotions_Black(ml);
		}
	}
		
	public boolean isInsufficientMaterial() {
		// Possible promotions
		if (pieces[Piece.PAWN] != 0)
			return false;
		
		// Major pieces
		if (pieces[Piece.QUEEN] != 0)
			return false;
		if (pieces[Piece.ROOK] != 0)
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
	
	public void forEachPiece(IForEachPieceCallback caller) {
		long scratchBitBoard = allPieces;
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			long mask = 1L << bit_offset;
			int piece = getPieceAtSquare(mask);
			caller.callback(piece, bit_offset);
			scratchBitBoard ^= mask;
		}
	}
	
	public long getEmpty() {
		return ~allPieces;
	}
	
	public boolean isPassedPawnPresent() {
		return passedPawns != 0L;
	}
	
	public long getPassedPawns() {
		return passedPawns;
	}
	
	public void setPassedPawns(long ppBitBoard) {
		passedPawns = ppBitBoard;
	}
	
	public void setHash(IZobristUpdate hash) {
		this.hashUpdater = hash;
	}
	
	public static class NetInput {
		public int white_king_sq;
		public int black_king_sq;
		public int[] white_pieces = new int[17];
		public int[] white_squares = new int[17];
		public int[] black_pieces = new int[17];
		public int[] black_squares = new int[17];
	}
	
	int [] piece_to_net_lut = {};
	
    public static final int[] PIECE_TO_NET_PIECE;
    static {
    	PIECE_TO_NET_PIECE = new int [Piece.PIECE_LENGTH];
        
        PIECE_TO_NET_PIECE[Piece.WHITE_QUEEN] = 4;
        PIECE_TO_NET_PIECE[Piece.WHITE_ROOK] = 3;
        PIECE_TO_NET_PIECE[Piece.WHITE_BISHOP] = 2;
        PIECE_TO_NET_PIECE[Piece.WHITE_KNIGHT] = 1;
        PIECE_TO_NET_PIECE[Piece.WHITE_KING] = 5;
        PIECE_TO_NET_PIECE[Piece.WHITE_PAWN] = 0;
        
        PIECE_TO_NET_PIECE[Piece.BLACK_QUEEN] = 4;
        PIECE_TO_NET_PIECE[Piece.BLACK_ROOK] = 3;
        PIECE_TO_NET_PIECE[Piece.BLACK_BISHOP] = 2;
        PIECE_TO_NET_PIECE[Piece.BLACK_KNIGHT] = 1;
        PIECE_TO_NET_PIECE[Piece.BLACK_KING] = 5;
        PIECE_TO_NET_PIECE[Piece.BLACK_PAWN] = 0;
    }
	public static int convertPiece(int pieceType) {
		return PIECE_TO_NET_PIECE[pieceType];
	}
	
	public NetInput populateNetInput() {
		NetInput input = new NetInput();
		long bb_w_king 		= getWhiteKing();
		long bb_b_king 		= getBlackKing();
		long bb_w_queens 	= getWhiteQueens();
		long bb_b_queens 	= getBlackQueens();
		long bb_w_rooks 	= getWhiteRooks();
		long bb_b_rooks 	= getBlackRooks();
		long bb_w_bishops 	= getWhiteBishops();
		long bb_b_bishops 	= getBlackBishops();
		long bb_w_knights 	= getWhiteKnights();
		long bb_b_knights 	= getBlackKnights();
		long bb_w_pawns 	= getWhitePawns();
		long bb_b_pawns 	= getBlackPawns();
		
		int index_white 	= 0;
		int index_black 	= 0;
		
		input.white_king_sq = BitBoard.convertToBitOffset(bb_w_king);
		input.white_pieces[index_white] = convertPiece(Piece.WHITE_KING);
		input.white_squares[index_white] = input.white_king_sq;
		index_white++;
		
		input.black_king_sq	= BitBoard.convertToBitOffset(bb_b_king);
		input.black_pieces[index_black] = convertPiece(Piece.BLACK_KING);
		input.black_squares[index_black] = input.black_king_sq;
		index_black++;
		
		while (bb_w_queens != 0) {
			input.white_pieces[index_white] 	= convertPiece(Piece.WHITE_QUEEN);
			input.white_squares[index_white] 	= BitBoard.convertToBitOffset(bb_w_queens);
			index_white++;
			bb_w_queens &= bb_w_queens - 1;
		}
		
		while (bb_b_queens != 0) {
			input.black_pieces[index_black] 	= convertPiece(Piece.BLACK_QUEEN);
			input.black_squares[index_black] 	= BitBoard.convertToBitOffset(bb_b_queens);
			index_black++;
			bb_b_queens &= bb_b_queens - 1;
		}
		
		while (bb_w_rooks != 0) {
			input.white_pieces[index_white] 	= convertPiece(Piece.WHITE_ROOK);
			input.white_squares[index_white] 	= BitBoard.convertToBitOffset(bb_w_rooks);
			index_white++;
			bb_w_rooks &= bb_w_rooks - 1;
		}
		
		while (bb_b_rooks != 0) {
			input.black_pieces[index_black] 	= convertPiece(Piece.BLACK_ROOK);
			input.black_squares[index_black] 	= BitBoard.convertToBitOffset(bb_b_rooks);
			index_black++;
			bb_b_rooks &= bb_b_rooks - 1;
		}
		
		while (bb_w_bishops != 0) {
			input.white_pieces[index_white] 	= convertPiece(Piece.WHITE_BISHOP);
			input.white_squares[index_white] 	= BitBoard.convertToBitOffset(bb_w_bishops);
			index_white++;
			bb_w_bishops &= bb_w_bishops - 1;
		}
		
		while (bb_b_bishops != 0) {
			input.black_pieces[index_black] 	= convertPiece(Piece.BLACK_BISHOP);
			input.black_squares[index_black] 	= BitBoard.convertToBitOffset(bb_b_bishops);
			index_black++;
			bb_b_bishops &= bb_b_bishops - 1;
		}
		
		while (bb_w_knights != 0) {
			input.white_pieces[index_white] 	= convertPiece(Piece.WHITE_KNIGHT);
			input.white_squares[index_white] 	= BitBoard.convertToBitOffset(bb_w_knights);
			index_white++;
			bb_w_knights &= bb_w_knights - 1;
		}
		
		while (bb_b_knights != 0) {
			input.black_pieces[index_black] 	= convertPiece(Piece.BLACK_KNIGHT);
			input.black_squares[index_black] 	= BitBoard.convertToBitOffset(bb_b_knights);
			index_black++;
			bb_b_knights &= bb_b_knights - 1;
		}
		
		while (bb_w_pawns != 0) {
			input.white_pieces[index_white] 	= convertPiece(Piece.WHITE_PAWN);
			input.white_squares[index_white] 	= BitBoard.convertToBitOffset(bb_w_pawns);
			index_white++;
			bb_w_pawns &= bb_w_pawns - 1;
		}
		
		while (bb_b_pawns != 0) {
			input.black_pieces[index_black] 	= convertPiece(Piece.BLACK_PAWN);
			input.black_squares[index_black] 	= BitBoard.convertToBitOffset(bb_b_pawns);
			index_black++;
			bb_b_pawns &= bb_b_pawns - 1;
		}
		
		input.white_pieces[index_white] = -1;
		input.black_pieces[index_black] = -1;
		return input;
	}
	
	public void updateAccumulatorsForPromotion(boolean isWhite, int originPiece, int fullPromotedPiece, int originBitOffset, int targetBitOffset) {	
		int promo_piece = isWhite ? convertPiece(fullPromotedPiece) : -1;
		int black_promo_piece = isWhite ? -1 : convertPiece(fullPromotedPiece);
		nnue.iterativeAccumulatorSubtract(promo_piece, originBitOffset, black_promo_piece, originBitOffset);
		int piece = isWhite ? convertPiece(originPiece) : -1;
		int black_piece = isWhite ? -1 : convertPiece(originPiece);
		nnue.iterativeAccumulatorAdd(piece, targetBitOffset, black_piece, targetBitOffset);
	}
	
	public void updateAccumulatorsForBasicMove(boolean isWhite, int pieceToMove, int originBitOffset, int targetBitOffset) {
		int white_piece = isWhite ? convertPiece(pieceToMove) : -1;
		int black_piece = isWhite ? -1 : convertPiece(pieceToMove);
		nnue.iterativeAccumulatorSubtract(white_piece, originBitOffset, black_piece, originBitOffset);
		nnue.iterativeAccumulatorAdd(white_piece, targetBitOffset, black_piece, targetBitOffset);
	}
	
	public void updateAccumulatorsForReplaceCapture(boolean isWhite, int targetPiece, int capturedPieceSquare) {
		int piece = !isWhite ? convertPiece(targetPiece) : -1;
		int black_piece = !isWhite ? -1 : convertPiece(targetPiece);
		nnue.iterativeAccumulatorAdd(piece, capturedPieceSquare, black_piece, capturedPieceSquare);
	}
	
	public void updateAccumulatorsForCapture(boolean isWhite, int targetPiece, int capturedPieceSquare) {
		int piece = !isWhite ? convertPiece(targetPiece) : -1;
		int black_piece = !isWhite ? -1 : convertPiece(targetPiece);
		nnue.iterativeAccumulatorSubtract(piece, capturedPieceSquare, black_piece, capturedPieceSquare);
	}
	
	public void incrementallyUpdateStateForCapture(boolean isWhite, int targetPiece, int captureBitOffset) {
		me.updateForCapture(targetPiece, captureBitOffset);
		hashUpdater.doCapturedPiece(captureBitOffset, targetPiece);
		updateAccumulatorsForCapture(isWhite, targetPiece, captureBitOffset);
	}
	
	public void incrementallyUpdateStateForUndoCapture(boolean isWhite, int targetPiece, int capturedPieceSquare) {
		me.updateForReplacedCapture(targetPiece, capturedPieceSquare);
		// Hash update is restored by copy when move is undone
		updateAccumulatorsForReplaceCapture(isWhite, targetPiece, capturedPieceSquare);			
	}
	
	public void incrementallyUpdateStateForPromotion(boolean isWhite, int pieceToMove, int fullPromotedPiece, int originBitOffset, int targetBitOffset) {
		me.updateWhenDoingPromotion(fullPromotedPiece, originBitOffset, targetBitOffset);
		hashUpdater.doPromotionMove(targetBitOffset, originBitOffset, pieceToMove, fullPromotedPiece);
		updateAccumulatorsForPromotion(isWhite, fullPromotedPiece, pieceToMove, originBitOffset, targetBitOffset);
	}
}