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
	
	private static final int LUT_WHITE = 0;
	private static final int LUT_BLACK = 1;
	
	public long[] pieces = new long[7]; // N.b. INDEX_NONE is an empty long at index 0.
	public MaterialPhase me;
	public boolean insufficient = false;
	private long passedPawns = 0L;
	public NNUE nnue = new NNUE();
	
	class moveProcessingHelper {
		int targetBitOffset;
		int originBitOffset;
		int promotedPiece;
		int targetPiece;
		int pieceToMove;
		long initialSquareMask;
		long targetSquareMask;
		long positionsMask;
		boolean isCapture;
		int pieceType;
		
		void unpackToDoMove(int move) {
			// unload move
			targetBitOffset = move & 0x3F;
			move >>>= 6;
			originBitOffset = move & 0x3F;
			move >>>= 6;
			promotedPiece = move & 0x7;
			move >>>= 4;
			targetPiece = move & 0xF;
			move >>>= 4;
			pieceToMove = move & 0xF;
			move >>>= 4;

			initialSquareMask = 1L << mh.originBitOffset;
			targetSquareMask = 1L << mh.targetBitOffset;
			positionsMask = initialSquareMask | targetSquareMask;
			isCapture = mh.targetPiece != Piece.NONE;
			pieceType = Piece.PIECE_NO_COLOUR_MASK & mh.pieceToMove;
		}
		
		void unpackToUndoMove(int moveToUndo) {
			// unload move
			int temp = moveToUndo;
			originBitOffset = temp & 0x3F;
			temp >>>= 6;
			targetBitOffset = temp & 0x3F;
			temp >>>= 6;
			promotedPiece = temp & 0x7;
			temp >>>= 4; // Skip enP bit as well
			targetPiece = temp & 0xF;
			temp >>>= 4;
			pieceToMove = temp & 0xF;
			temp >>>= 4;
			
			initialSquareMask = 1L << originBitOffset;
			targetSquareMask = 1L << targetBitOffset;
			positionsMask = initialSquareMask | targetSquareMask;
			isCapture = targetPiece != Piece.NONE;
			pieceType = Piece.PIECE_NO_COLOUR_MASK & pieceToMove;
		}
	};
	
	moveProcessingHelper mh;
	
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
		mh = new moveProcessingHelper();
		
		if (getWhiteKing() != 0 && getBlackKing() != 0) {
			// Initialise the accumulators for the position, if we aren't running a unit test
			NetInput input = populateNetInput();
			nnue.fullAccumulatorUpdate(input.white_pieces, input.white_squares, input.black_pieces, input.black_squares);
		}
		
		createPassedPawnsBoard();
		insufficient = isInsufficientMaterial();
	}
	
	public void createPassedPawnsBoard() {
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
	
	private void basicAsserts(int move) {
		if (getPawns() == 0L) assert passedPawns == 0L :
			String.format("pawns %x passed %x fen %s", getPawns(), passedPawns, getAsFenString());
		assert Long.bitCount(pieces[INDEX_KING]) == 2;
		assert insufficient == isInsufficientMaterial() :
			String.format("Insufficient dsicrepancy flag=%b isInsufficientMaterial=%b %s", insufficient, isInsufficientMaterial(), getAsFenString());
		long white_king = getWhiteKing(), black_king = getBlackKing();
		assert white_king != 0L && black_king != 0L : 
			String.format("Missing king W=%x B=%x %s", white_king, black_king, getAsFenString());
	}
	
	private void basicStartAsserts(int move) {
		basicAsserts(move);
		
		// Check piece to move is on the bitboard, and is correct side
		assert (pieces[mh.pieceType] & mh.initialSquareMask) != 0: 
			String.format("Non-existant piece %s at %s for move %s", 
					Piece.toFenChar(mh.pieceToMove), Position.toGenericPosition(BitBoard.bitToPosition_Lut[mh.originBitOffset]), Move.toString(move));
		assert ((Piece.isWhite(mh.pieceToMove) ? whitePieces : blackPieces) & mh.initialSquareMask) != 0: 
			String.format("Piece %s not on colour board for move %s", 
					Piece.toFenChar(mh.pieceToMove), Move.toString(move));
		assert (allPieces & mh.initialSquareMask) != 0: 
			String.format("Piece %s not on all pieces board for move %s", 
					Piece.toFenChar(mh.pieceToMove), Move.toString(move));
		assert (mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK) != Piece.DONT_CARE;
	}
	
	private void captureAsserts(int move, long captureMask, int captureBitOffset) {
		assert (pieces[Piece.PIECE_NO_COLOUR_MASK & mh.targetPiece] & captureMask) != 0: 
			String.format("Non-existant target piece %s at %s for move %s", 
					Piece.toFenChar(mh.targetPiece), Position.toGenericPosition(captureBitOffset), Move.toString(move));
		assert ((!Piece.isWhite(mh.pieceToMove) ? whitePieces : blackPieces) & captureMask) != 0: 
			String.format("Piece %s not on colour board for move %s", 
					Piece.toFenChar(mh.targetPiece), Move.toString(move));
		assert (allPieces & captureMask) != 0: 
			String.format("Piece %s not on all pieces board for move %s", 
					Piece.toFenChar(mh.targetPiece), Move.toString(move));
		assert ((allPieces & captureMask) != 0) : String.format("Non-existant target piece %c at %s",
				Piece.toFenChar(mh.targetPiece), Position.toGenericPosition(BitBoard.bitToPosition_Lut[captureBitOffset]));		
	}
	
	private void endAsserts(int move) {
		// Check piece bit boards to me num pieces consistency
		assert (me.numberOfPieces[Piece.WHITE_KNIGHT]+me.numberOfPieces[Piece.BLACK_KNIGHT]) == Long.bitCount(pieces[INDEX_KNIGHT]);
		assert (me.numberOfPieces[Piece.WHITE_BISHOP]+me.numberOfPieces[Piece.BLACK_BISHOP]) == Long.bitCount(pieces[INDEX_BISHOP]);
		assert (me.numberOfPieces[Piece.WHITE_ROOK]+me.numberOfPieces[Piece.BLACK_ROOK]) == Long.bitCount(pieces[INDEX_ROOK]);
		assert (me.numberOfPieces[Piece.WHITE_QUEEN]+me.numberOfPieces[Piece.BLACK_QUEEN]) == Long.bitCount(pieces[INDEX_QUEEN]);
		assert Long.bitCount(pieces[INDEX_KING]) == 2;
		//if (!insufficient) {
			/* Both of these iterative updates are skipped if there is insufficient material, it seems forced can come here too */
			long iterativeUpdatePassedPawns = passedPawns;
			createPassedPawnsBoard();
			assert iterativeUpdatePassedPawns == passedPawns :
				String.format("Passed Pawns error iterative %s != scratch %s move = %s pawns = %s", 
					BitBoard.toString(iterativeUpdatePassedPawns), BitBoard.toString(passedPawns), 
					Move.toString(move), BitBoard.toString(this.getPawns()));
			
			int old_score = nnue.old_evaluate(this, false);
			int new_score = nnue.new_evaluate_for_assert(this, false);
			assert old_score == new_score : String.format("old %d new %d insufficient=%b", old_score, new_score, insufficient);
		//}
	}
	
	public boolean doMoveBlack(int move) {		
		int captureBitOffset = BitBoard.INVALID;
		
		// unload move
		mh.unpackToDoMove(move);
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			basicStartAsserts(move);
			assert (blackPieces & mh.initialSquareMask) != 0: 
				String.format("Piece %s not on colour board for move %s", 
						Piece.toFenChar(mh.pieceToMove), Move.toString(move));
		}
		
		if (mh.isCapture) {
			// Handle captures
			captureBitOffset = mh.targetBitOffset + (Move.isEnPassantCapture(move) ? 8 : 0);
			long captureMask = 1L << captureBitOffset;
			if (EubosEngineMain.ENABLE_ASSERTS) {
				captureAsserts(move, captureMask, captureBitOffset);
				assert (whitePieces & captureMask) != 0: 
					String.format("Piece %s not on colour board for move %s", 
							Piece.toFenChar(mh.targetPiece), Move.toString(move));
			}
			// Remove from relevant colour bitboard
			whitePieces ^= captureMask;
			// Remove from specific bitboard
			pieces[mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK] ^= captureMask;
			// Remove from all pieces bitboard
			allPieces ^= captureMask;
		}
		
		// Switch colour bitboard
		blackPieces ^= mh.positionsMask;
		// Switch all pieces bitboard
		allPieces ^= mh.positionsMask;
		// Switch piece-specific bitboards and piece lists
		if (mh.promotedPiece != Piece.NONE) {
			// For a promotion, need to resolve piece-specific across multiple bitboards
			pieces[INDEX_PAWN] ^= mh.initialSquareMask;
			pieces[mh.promotedPiece] |= mh.targetSquareMask;
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard, pieceList and PST score
			pieces[mh.pieceType] ^= mh.positionsMask;
		}
		
		if (isKingInCheck(false)) {
			// Switch piece bitboard
			if (mh.promotedPiece != Piece.NONE) {
				// Remove promoted piece and replace it with a pawn
				pieces[mh.promotedPiece] ^= mh.targetSquareMask;	
				pieces[INDEX_PAWN] |= mh.initialSquareMask;
			} else {
				pieces[mh.pieceType] ^= mh.positionsMask;
			}
			// Switch colour bitboard
			blackPieces ^= mh.positionsMask;
			// Switch all pieces bitboard
			allPieces ^= mh.positionsMask;
			
			// Undo any capture that had been previously performed.
			if (mh.isCapture) {
				long mask = 1L << captureBitOffset;
				pieces[mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= mask;
				whitePieces |= mask;
				allPieces |= mask;
			}
			return true;
		}
		
		// Initialise En Passant target square
		setEnPassantTargetSq(BitBoard.INVALID);
		
		if (mh.isCapture) {
			me.updateForCapture(mh.targetPiece, captureBitOffset);
			insufficient = isInsufficientMaterial();
			//if (!insufficient) {
				hashUpdater.doCapturedPiece(captureBitOffset, mh.targetPiece);
				updateAccumulatorsForCaptureForBlack(mh.targetPiece, captureBitOffset);
			//}
		}
//		else if (mh.promotedPiece == Piece.KNIGHT) {
//			// under promotion can result in insufficient if only one pawn, in rare conditions
//			insufficient = isInsufficientMaterial();
//		}
//		if (insufficient) {
//			if (mh.promotedPiece == Piece.KNIGHT) {
//				me.updateWhenDoingPromotion(Piece.BLACK_KNIGHT, mh.originBitOffset, mh.targetBitOffset);
//			}
//			passedPawns = 0L;
//			return false;
//		}
		
		if (mh.promotedPiece != Piece.NONE) {
			passedPawns &= ~mh.initialSquareMask;
			insufficient = isInsufficientMaterial();
			incrementallyUpdateStateForPromotionForBlack(mh.pieceToMove, mh.promotedPiece, mh.originBitOffset, mh.targetBitOffset);
		} else {
			hashUpdater.doBasicMove(mh.targetBitOffset, mh.originBitOffset, mh.pieceToMove);
			updateAccumulatorsForBasicMoveBlack(mh.pieceToMove, mh.originBitOffset, mh.targetBitOffset);
					
			// Iterative update of passed pawns bitboard
			// Note: this needs to be done after the piece bit boards are updated
			// build up significant file masks, should be three or four consecutive files, re-evaluate passed pawns in those files
			long file_masks = 0L;
			if (mh.pieceType == Piece.PAWN) {
				moveEnablesEnPassantCaptureAsBlack(mh.originBitOffset, mh.targetBitOffset);
				// Handle regular pawn pushes
				file_masks |= BitBoard.IterativePassedPawnNonCapture[LUT_BLACK][mh.originBitOffset];
				
				// Handle pawn captures
				if (mh.targetPiece != Piece.NONE) {
					if (Piece.isPawn(mh.targetPiece)) {
						// Pawn takes pawn, clears whole front-span of target pawn (note negation of colour)
						file_masks |= BitBoard.PassedPawn_Lut[LUT_WHITE][mh.targetBitOffset];
					}
					// manage file transition of capturing pawn moves
					boolean isLeft = BitBoard.getFile(mh.targetBitOffset) < BitBoard.getFile(mh.originBitOffset);
					file_masks |= BitBoard.IterativePassedPawnUpdateCaptures_Lut[mh.originBitOffset][LUT_BLACK][isLeft ? 0 : 1];
				}
			} else if (Piece.isPawn(mh.targetPiece)) {
				// Piece takes pawn, potentially opens capture and adjacent files
				file_masks |= mh.targetSquareMask;
				file_masks |= BitBoard.PassedPawn_Lut[LUT_WHITE][mh.targetBitOffset];
			} else {
				// doesn't need to be handled - can't change passed pawn bit board
				// Handle castling secondary rook moves...
				if (Move.isCastling(move)) {
					performSecondaryCastlingMoveAsBlack(mh.targetBitOffset);
				}
			}
			if (file_masks != 0L) {
				// clear passed pawns in concerned files before re-evaluating
				// Note: vacated initial square
				passedPawns &= ~(mh.initialSquareMask|file_masks);
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
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			basicAsserts(move);
			endAsserts(move);
		}
		
		return false;
	}
	
	public boolean doMoveWhite(int move) {		
		int captureBitOffset = BitBoard.INVALID;
		
		mh.unpackToDoMove(move);
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			basicStartAsserts(move);
			assert (whitePieces & mh.initialSquareMask) != 0: 
				String.format("Piece %s not on colour board for move %s", 
						Piece.toFenChar(mh.pieceToMove), Move.toString(move));
		}
		
		if (mh.isCapture) {
			captureBitOffset = mh.targetBitOffset + (Move.isEnPassantCapture(move) ? -8 : 0);
			long captureMask = 1L << captureBitOffset;
			if (EubosEngineMain.ENABLE_ASSERTS) {
				captureAsserts(move, captureMask, captureBitOffset);
				assert (blackPieces & captureMask) != 0: 
					String.format("Piece %s not on colour board for move %s", 
							Piece.toFenChar(mh.targetPiece), Move.toString(move));
			}
			// Remove from relevant colour bitboard
			blackPieces ^= captureMask;
			// Remove from specific bitboard
			pieces[mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK] ^= captureMask;
			// Remove from all pieces bitboard
			allPieces ^= captureMask;
		}
		
		// Switch colour bitboard
		whitePieces ^= mh.positionsMask;
		// Switch all pieces bitboard
		allPieces ^= mh.positionsMask;
		// Switch piece-specific bitboards and piece lists
		if (mh.promotedPiece != Piece.NONE) {
			// For a promotion, need to resolve piece-specific across multiple bitboards
			pieces[INDEX_PAWN] ^= mh.initialSquareMask;
			pieces[mh.promotedPiece] |= mh.targetSquareMask;
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard, pieceList and PST score
			pieces[mh.pieceType] ^= mh.positionsMask;
		}
		
		if (isKingInCheck(true)) {
			// Switch piece bitboard
			if (mh.promotedPiece != Piece.NONE) {
				// Remove promoted piece and replace it with a pawn
				pieces[mh.promotedPiece] ^= mh.targetSquareMask;	
				pieces[INDEX_PAWN] |= mh.initialSquareMask;
			} else {
				pieces[mh.pieceType] ^= mh.positionsMask;
			}
			// Switch colour bitboard
			whitePieces ^= mh.positionsMask;
			// Switch all pieces bitboard
			allPieces ^= mh.positionsMask;
			
			// Undo any capture that had been previously performed.
			if (mh.isCapture) {
				long mask = 1L << captureBitOffset;
				pieces[mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= mask;
				blackPieces |= mask;
				allPieces |= mask;
			}
			return true;
		}
		
		// Initialise En Passant target square
		setEnPassantTargetSq(BitBoard.INVALID);
		
		if (mh.isCapture) {
			me.updateForCapture(mh.targetPiece, captureBitOffset);
			insufficient = isInsufficientMaterial();
//			if (!insufficient) {
				hashUpdater.doCapturedPiece(captureBitOffset, mh.targetPiece);
				updateAccumulatorsForCaptureForWhite(mh.targetPiece, captureBitOffset);
//			}
		} 
//		else if (mh.promotedPiece == Piece.KNIGHT) {
//			// under promotion can result in insufficient if only one pawn, in rare conditions
//			insufficient = isInsufficientMaterial();
//		}
//		if (insufficient) {
//			if (mh.promotedPiece == Piece.KNIGHT) {
//				me.updateWhenDoingPromotion(Piece.WHITE_KNIGHT, mh.originBitOffset, mh.targetBitOffset);
//			}
//			passedPawns = 0L;
//			return false;
//		}
		
		if (mh.promotedPiece != Piece.NONE) {
			passedPawns &= ~mh.initialSquareMask;
			incrementallyUpdateStateForPromotionForWhite(mh.pieceToMove, mh.promotedPiece, mh.originBitOffset, mh.targetBitOffset);
			insufficient = isInsufficientMaterial();
		} else {
			hashUpdater.doBasicMove(mh.targetBitOffset, mh.originBitOffset, mh.pieceToMove);
			updateAccumulatorsForBasicMoveWhite(mh.pieceToMove, mh.originBitOffset, mh.targetBitOffset);
			
			// Iterative update of passed pawns bitboard
			// Note: this needs to be done after the piece bit boards are updated
			// build up significant file masks, should be three or four consecutive files, re-evaluate passed pawns in those files
			long file_masks = 0L;
			if (mh.pieceType == Piece.PAWN) {
				moveEnablesEnPassantCaptureAsWhite(mh.originBitOffset, mh.targetBitOffset);
				
				// Handle regular pawn pushes
				file_masks |= BitBoard.IterativePassedPawnNonCapture[LUT_WHITE][mh.originBitOffset];
				
				// Handle pawn captures
				if (mh.targetPiece != Piece.NONE) {
					if (Piece.isPawn(mh.targetPiece)) {
						// Pawn takes pawn, clears whole front-span of target pawn (note negation of colour)
						file_masks |= BitBoard.PassedPawn_Lut[LUT_BLACK][mh.targetBitOffset];
					}
					// manage file transition of capturing pawn moves
					boolean isLeft = BitBoard.getFile(mh.targetBitOffset) < BitBoard.getFile(mh.originBitOffset);
					file_masks |= BitBoard.IterativePassedPawnUpdateCaptures_Lut[mh.originBitOffset][LUT_WHITE][isLeft ? 0 : 1];
				}
			} else if (Piece.isPawn(mh.targetPiece)) {
				// Piece takes pawn, potentially opens capture and adjacent files
				file_masks |= mh.targetSquareMask;
				file_masks |= BitBoard.PassedPawn_Lut[LUT_BLACK][mh.targetBitOffset];
			} else {
				// Handle castling secondary rook moves...
				if (Move.isCastling(move)) {
					performSecondaryCastlingMoveAsWhite(mh.targetBitOffset);
				}
			}
			if (file_masks != 0L) {
				// clear passed pawns in concerned files before re-evaluating
				// Note: vacated initial square
				passedPawns &= ~(mh.initialSquareMask|file_masks);
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
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			basicAsserts(move);
			endAsserts(move);
		}
		
		return false;
	}
	
	public void undoMoveWhite(int moveToUndo) {
		// unload move
		mh.unpackToUndoMove(moveToUndo);
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			long pieceMask = (mh.promotedPiece != Piece.NONE) ? pieces[mh.promotedPiece] : pieces[mh.pieceType];
			assert (pieceMask & mh.initialSquareMask) != 0: String.format("Non-existant piece at %s, %s",
					Position.toGenericPosition(BitBoard.bitToPosition_Lut[mh.originBitOffset]), Move.toString(moveToUndo));
		}
		// Switch piece bitboard
		if (mh.promotedPiece != Piece.NONE) {
			// Remove promoted piece and replace it with a pawn
			pieces[mh.promotedPiece] ^= mh.initialSquareMask;	
			pieces[INDEX_PAWN] |= mh.targetSquareMask;
			me.updateWhenUndoingPromotion(mh.promotedPiece, mh.originBitOffset, mh.targetBitOffset);
			//if (!insufficient)
				updateAccumulatorsForPromotionWhite(mh.pieceToMove, mh.promotedPiece, mh.originBitOffset, mh.targetBitOffset);
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard and accumulators
			pieces[mh.pieceType] ^= mh.positionsMask;
			//if (!insufficient)
				updateAccumulatorsForBasicMoveWhite(mh.pieceToMove, mh.originBitOffset, mh.targetBitOffset);
		}
		// Switch colour bitboard
		whitePieces ^= mh.positionsMask;
		// Switch all pieces bitboard
		allPieces ^= mh.positionsMask;
		
		// Undo any capture that had been previously performed.
		if (mh.isCapture) {
			// Origin square because the move has been reversed and origin square is the original target square
			int capturedPieceSquare = mh.originBitOffset + (Move.isEnPassantCapture(moveToUndo) ? -8 : 0); 
			long mask = 1L << capturedPieceSquare;
			pieces[mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= mask;
			blackPieces |= mask;
			allPieces |= mask;
			incrementallyUpdateStateForUndoCaptureForWhite(mh.targetPiece, capturedPieceSquare);			
		} else if (Move.isCastling(moveToUndo)) {
			unperformSecondaryCastlingMoveAsWhite(mh.originBitOffset);
		}
		
		// now restored by position manager after undoing move
		insufficient = isInsufficientMaterial();

		if (EubosEngineMain.ENABLE_ASSERTS) {
			basicAsserts(moveToUndo);
			//if (!insufficient) {
				int old_score = nnue.old_evaluate(this, true);
				int new_score = nnue.new_evaluate_for_assert(this, true);
				// Get forced move can lead to insufficient material, then we don't use stack to restore eval or hash
				assert old_score == new_score : String.format("old %d new %d insufficient %b", old_score, new_score, insufficient);
			//}
		}
	}
	
	public void undoMoveBlack(int moveToUndo) {
		// unload move
		mh.unpackToUndoMove(moveToUndo);
		
		// Check assertions, if enabled in build
		if (EubosEngineMain.ENABLE_ASSERTS) {
			long pieceMask = (mh.promotedPiece != Piece.NONE) ? pieces[mh.promotedPiece] : pieces[mh.pieceType];
			assert (pieceMask & mh.initialSquareMask) != 0: String.format("Non-existant piece at %s, %s",
					Position.toGenericPosition(BitBoard.bitToPosition_Lut[mh.originBitOffset]), Move.toString(moveToUndo));
		}
		
		// Switch piece bitboard
		if (mh.promotedPiece != Piece.NONE) {
			// Remove promoted piece and replace it with a pawn
			pieces[mh.promotedPiece] ^= mh.initialSquareMask;	
			pieces[INDEX_PAWN] |= mh.targetSquareMask;
			int fullPromotedPiece = (mh.promotedPiece|Piece.BLACK);
			me.updateWhenUndoingPromotion(fullPromotedPiece, mh.originBitOffset, mh.targetBitOffset);
			//if (!insufficient)
				updateAccumulatorsForPromotionBlack(mh.pieceToMove, fullPromotedPiece, mh.originBitOffset, mh.targetBitOffset);	
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard and accumulators
			pieces[mh.pieceType] ^= mh.positionsMask;
			//if (!insufficient)
				updateAccumulatorsForBasicMoveBlack(mh.pieceToMove, mh.originBitOffset, mh.targetBitOffset);
		}
		// Switch colour bitboard
		blackPieces ^= mh.positionsMask;
		// Switch all pieces bitboard
		allPieces ^= mh.positionsMask;
		
		// Undo any capture that had been previously performed.
		if (mh.isCapture) {
			// Origin square because the move has been reversed and origin square is the original target square
			int capturedPieceSquare = mh.originBitOffset + (Move.isEnPassantCapture(moveToUndo) ? 8 : 0); 
			long mask = 1L << capturedPieceSquare;
			pieces[mh.targetPiece & Piece.PIECE_NO_COLOUR_MASK] |= mask;
			whitePieces |= mask;
			allPieces |= mask;
			incrementallyUpdateStateForUndoCaptureForBlack(mh.targetPiece, capturedPieceSquare);			
		} else if (Move.isCastling(moveToUndo)) {
			unperformSecondaryCastlingMoveAsBlack(mh.originBitOffset);
		}
		
		// now restored by position manager after undoing move
		insufficient = isInsufficientMaterial();

		if (EubosEngineMain.ENABLE_ASSERTS) {
			basicAsserts(moveToUndo);
			//if (!insufficient) {
				int old_score = nnue.old_evaluate(this, false);
				int new_score = nnue.new_evaluate_for_assert(this, false);
				assert old_score == new_score : String.format("old %d new %d insufficient %b", old_score, new_score, insufficient);
			//}
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
	
	private void moveEnablesEnPassantCaptureAsBlack(int originBitOffset, int targetBitOffset) {
		if (BitBoard.getRank(originBitOffset) == IntRank.R7) {
			if (BitBoard.getRank(targetBitOffset) == IntRank.R5) {
				setEnPassantTargetSq(targetBitOffset+8);
			}
		}
	}
	
	private void moveEnablesEnPassantCaptureAsWhite(int originBitOffset, int targetBitOffset) {
		if (BitBoard.getRank(originBitOffset) == IntRank.R2) {
			if (BitBoard.getRank(targetBitOffset) == IntRank.R4) {
				setEnPassantTargetSq(targetBitOffset-8);
			}
		}
	}
	
	public int getKingPosition(boolean isWhite) {
		long king_mask = pieces[Piece.KING] & (isWhite ? whitePieces : blackPieces);
		return BitBoard.convertToBitOffset(king_mask);
	}
	
	public int getQueenPosition(boolean isWhite) {
		long queen_mask = pieces[Piece.QUEEN] & (isWhite ? whitePieces : blackPieces);
		return BitBoard.convertToBitOffset(queen_mask);
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
	
	private void performSecondaryCastlingMoveAsWhite(int target) {
		if (target == BitBoard.g1) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			hashUpdater.doBasicMove(BitBoard.f1, BitBoard.h1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtractWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.h1);
			nnue.iterativeAccumulatorAddWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.f1);
		} else {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			hashUpdater.doBasicMove(BitBoard.d1, BitBoard.a1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtractWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.a1);
			nnue.iterativeAccumulatorAddWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.d1);
		}
	}
	
	private void performSecondaryCastlingMoveAsBlack(int target) {
		if (target == BitBoard.g8) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			hashUpdater.doBasicMove(BitBoard.f8, BitBoard.h8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtractBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.h8);
			nnue.iterativeAccumulatorAddBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.f8);
		} else {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			hashUpdater.doBasicMove(BitBoard.d8, BitBoard.a8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtractBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.a8);
			nnue.iterativeAccumulatorAddBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.d8);
		}
	}
	
	private void unperformSecondaryCastlingMoveAsWhite(int origin) {
		if (origin == BitBoard.g1) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			hashUpdater.doBasicMove(BitBoard.h1, BitBoard.f1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtractWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.f1);
			nnue.iterativeAccumulatorAddWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.h1);
		} else {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			hashUpdater.doBasicMove(BitBoard.a1, BitBoard.d1, Piece.WHITE_ROOK);
			nnue.iterativeAccumulatorSubtractWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.d1);
			nnue.iterativeAccumulatorAddWhite(convertPiece(Piece.WHITE_ROOK), BitBoard.a1);
		}
	}
	
	private void unperformSecondaryCastlingMoveAsBlack(int origin) {
		if (origin == BitBoard.g8) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			hashUpdater.doBasicMove(BitBoard.h8, BitBoard.f8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtractBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.f8);
			nnue.iterativeAccumulatorAddBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.h8);
		} else {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			hashUpdater.doBasicMove(BitBoard.a8, BitBoard.d8, Piece.BLACK_ROOK);
			nnue.iterativeAccumulatorSubtractBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.d8);
			nnue.iterativeAccumulatorAddBlack(convertPiece(Piece.BLACK_ROOK), BitBoard.a8);
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
		if (EubosEngineMain.ENABLE_ASSERTS) assert getKingPosition(isWhite) != BitBoard.INVALID;
		return squareIsAttacked(getKingPosition(isWhite), isWhite);
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
		bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
		Piece.king_generateMoves_White(ml, this, bit_offset);
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
		bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);			
		Piece.king_generateMoves_Black(ml, this, bit_offset);
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
		bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);			
		long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
		if ((opponentPieces & kingAttacksMask) != 0) {
			Piece.king_generateMovesExtSearch_White(ml, this, bit_offset);
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
		bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
		long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
		if ((opponentPieces & kingAttacksMask) != 0) {
			Piece.king_generateMovesExtSearch_White(ml, this, bit_offset);
		}
	}
	
	public void addMoves_CapturesExcludingPawnPromotions_Black_Endgame(IAddMoves ml) {
		// Optimisations for generating move lists in extended search
		long opponentPieces = whitePieces;
		long side = blackPieces;
		long scratchBitBoard;
		int bit_offset = BitBoard.INVALID;
		scratchBitBoard = pieces[Piece.KING] & side;
		bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);		
		long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
		if ((opponentPieces & kingAttacksMask) != 0) {
			Piece.king_generateMovesExtSearch_Black(ml, this, bit_offset);
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
		bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);			
		long kingAttacksMask = SquareAttackEvaluator.KingMove_Lut[bit_offset];
		if ((opponentPieces & kingAttacksMask) != 0) {
			Piece.king_generateMovesExtSearch_Black(ml, this, bit_offset);
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
	
	public void updateAccumulatorsForPromotionWhite(int originPiece, int fullPromotedPiece, int originBitOffset, int targetBitOffset) {	
		int promo_piece = convertPiece(fullPromotedPiece);
		int piece = convertPiece(originPiece);
		nnue.iterativeAccumulatorSubtractWhite(promo_piece, originBitOffset);
		nnue.iterativeAccumulatorAddWhite(piece, targetBitOffset);
	}
	
	public void updateAccumulatorsForBasicMoveWhite(int pieceToMove, int originBitOffset, int targetBitOffset) {
		int piece = convertPiece(pieceToMove);
		nnue.iterativeAccumulatorSubtractWhite(piece, originBitOffset);
		nnue.iterativeAccumulatorAddWhite(piece, targetBitOffset);
	}
	
	public void updateAccumulatorsForReplaceCaptureForWhite(int targetPiece, int capturedPieceSquare) {
		int piece = convertPiece(targetPiece);
		nnue.iterativeAccumulatorAddBlack(piece, capturedPieceSquare);
	}
	
	public void updateAccumulatorsForCaptureForWhite(int targetPiece, int capturedPieceSquare) {
		int piece = convertPiece(targetPiece);
		nnue.iterativeAccumulatorSubtractBlack(piece, capturedPieceSquare);
	}
	
	public void incrementallyUpdateStateForUndoCaptureForWhite(int targetPiece, int capturedPieceSquare) {
		me.updateForReplacedCapture(targetPiece, capturedPieceSquare);
		// Hash update is restored by copy when move is undone
		//if (!insufficient) {
			updateAccumulatorsForReplaceCaptureForWhite(targetPiece, capturedPieceSquare);
		//}
	}
	
	public void incrementallyUpdateStateForPromotionForWhite(int pieceToMove, int promotedPiece, int originBitOffset, int targetBitOffset) {
		me.updateWhenDoingPromotion(promotedPiece, originBitOffset, targetBitOffset);
		hashUpdater.doPromotionMove(targetBitOffset, originBitOffset, pieceToMove, promotedPiece);
		updateAccumulatorsForPromotionWhite(promotedPiece, pieceToMove, originBitOffset, targetBitOffset);
	}
	
	public void updateAccumulatorsForPromotionBlack(int originPiece, int fullPromotedPiece, int originBitOffset, int targetBitOffset) {	
		int promo_piece = convertPiece(fullPromotedPiece);
		int piece = convertPiece(originPiece);
		nnue.iterativeAccumulatorSubtractBlack(promo_piece, originBitOffset);
		nnue.iterativeAccumulatorAddBlack(piece, targetBitOffset);
	}
	
	public void updateAccumulatorsForBasicMoveBlack(int pieceToMove, int originBitOffset, int targetBitOffset) {
		int piece = convertPiece(pieceToMove);
		nnue.iterativeAccumulatorSubtractBlack(piece, originBitOffset);
		nnue.iterativeAccumulatorAddBlack(piece, targetBitOffset);
	}
	
	public void updateAccumulatorsForReplaceCaptureForBlack(int targetPiece, int capturedPieceSquare) {
		int piece = convertPiece(targetPiece);
		nnue.iterativeAccumulatorAddWhite(piece, capturedPieceSquare);
	}
	
	public void updateAccumulatorsForCaptureForBlack(int targetPiece, int capturedPieceSquare) {
		int piece = convertPiece(targetPiece);
		nnue.iterativeAccumulatorSubtractWhite(piece, capturedPieceSquare);
	}
	
	public void incrementallyUpdateStateForUndoCaptureForBlack(int targetPiece, int capturedPieceSquare) {
		me.updateForReplacedCapture(targetPiece, capturedPieceSquare);
		// Hash update is restored by copy when move is undone
		//if (!insufficient) {
			updateAccumulatorsForReplaceCaptureForBlack(targetPiece, capturedPieceSquare);
		//}
	}
	
	public void incrementallyUpdateStateForPromotionForBlack(int pieceToMove, int promotedPiece, int originBitOffset, int targetBitOffset) {
		int fullPromotedPiece = promotedPiece|Piece.BLACK;
		me.updateWhenDoingPromotion(fullPromotedPiece, originBitOffset, targetBitOffset);
		hashUpdater.doPromotionMove(targetBitOffset, originBitOffset, pieceToMove, fullPromotedPiece);
		updateAccumulatorsForPromotionBlack(fullPromotedPiece, pieceToMove, originBitOffset, targetBitOffset);
	}
}