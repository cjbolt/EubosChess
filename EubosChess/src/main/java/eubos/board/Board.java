package eubos.board;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import eubos.main.EubosEngineMain;
import eubos.position.CastlingManager;
import eubos.position.IAddMoves;
import eubos.position.IPawnHash;
import eubos.position.IZobristUpdate;
import eubos.position.Move;
import eubos.position.Position;
import eubos.score.PiecewiseEvaluation;

import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntRank;

public class Board {

	private IZobristUpdate hashUpdater;
	private IPawnHash pawnHashUpdater;
	
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
	
	public long[] pieces = new long[7]; // N.b. INDEX_NONE is an empty long at index 0.
	private long passedPawns = 0L;
	
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
	
	public PiecewiseEvaluation me;
	public MobilityAttacksEvaluator mae;
		
	// Only used for testing!
	public CountedPawnKnightAttackAggregator cpkaa;
	
	boolean isAttacksMaskValid = false;
	
	public boolean insufficient = false;
	
	public Board(Map<Integer, Integer> pieceMap) {
		cpkaa = new CountedPawnKnightAttackAggregator();
		mae = new MobilityAttacksEvaluator(this);
		
		allPieces = 0x0;
		whitePieces = 0x0;
		blackPieces = 0x0;
		for (int i=0; i<=INDEX_PAWN; i++) {
			pieces[i] = 0x0;
		}
		for (Entry<Integer, Integer> nextPiece : pieceMap.entrySet()) {
			setPieceAtSquare( nextPiece.getKey(), nextPiece.getValue());
		}
		me = new PiecewiseEvaluation();
		evaluateMaterial(me);
		createPassedPawnsBoard();
		insufficient = isInsufficientMaterial(); //isLikelyDrawnEndgame(Piece.Colour.isWhite(initialOnMove));
	}
	
	private void evaluateMaterialBalanceAndStaticPieceMobility(boolean isWhite, PiecewiseEvaluation me) {
		int side = isWhite ? 0 : Piece.BLACK;
		long ownPieces = isWhite ? whitePieces : blackPieces; 
		long scratchBitBoard = pieces[Piece.KING] & ownPieces;
		int bitOffset = BitBoard.INVALID;
		if (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {	
			me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.KING];
			me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.KING];
			me.addPst(side+Piece.KING, bitOffset);
		}
		scratchBitBoard = pieces[Piece.QUEEN] & ownPieces;
		bitOffset = BitBoard.convertToBitOffset(scratchBitBoard);
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.QUEEN];
			me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.QUEEN];
			me.numberOfPieces[side+Piece.QUEEN]++;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.ROOK] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.ROOK];
			me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.ROOK];
			me.numberOfPieces[side+Piece.ROOK]++;
			me.addPst(side+Piece.ROOK, bitOffset);
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.BISHOP] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.BISHOP];
			me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.BISHOP];
			me.numberOfPieces[side+Piece.BISHOP]++;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.KNIGHT] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.KNIGHT];
			me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.KNIGHT];
			me.addPst(side+Piece.KNIGHT, bitOffset);
			me.numberOfPieces[side+Piece.KNIGHT]++;
			scratchBitBoard ^= (1L << bitOffset);
		}
		scratchBitBoard = pieces[Piece.PAWN] & ownPieces;
		while (scratchBitBoard != 0L && (bitOffset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert getPieceAtSquare(1L << bitOffset) != Piece.NONE :
					String.format("Found a Pawn at %s that isn't on Board", Position.toGenericPosition(bitOffset));
			}
			me.mg_material += Piece.PIECE_TO_MATERIAL_LUT[0][side+Piece.PAWN];
			me.eg_material += Piece.PIECE_TO_MATERIAL_LUT[1][side+Piece.PAWN];
			me.addPst(side+Piece.PAWN, bitOffset);
			me.numberOfPieces[side+Piece.PAWN]++;
			scratchBitBoard ^= (1L << bitOffset);
		}
	}
	
	private void evaluateMaterial(PiecewiseEvaluation the_me) {
		evaluateMaterialBalanceAndStaticPieceMobility(true, the_me);
		evaluateMaterialBalanceAndStaticPieceMobility(false, the_me);
		the_me.setPhase();
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
		isAttacksMaskValid = false;
		
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
			// Incrementally update opponent material after capture, at the correct capturePosition
			me.updateForCapture(targetPiece, captureBitOffset);
			hashUpdater.doCapturedPiece(captureBitOffset, targetPiece);
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
			me.updateWhenDoingPromotion(fullPromotedPiece, originBitOffset, targetBitOffset);
			passedPawns &= ~initialSquareMask;
			hashUpdater.doPromotionMove(targetBitOffset, originBitOffset, pieceToMove, fullPromotedPiece);
			pawnHashUpdater.removePawn(pieceToMove, originBitOffset);
		} else {
			me.updateRegular(pieceType, pieceToMove, originBitOffset, targetBitOffset);
			hashUpdater.doBasicMove(targetBitOffset, originBitOffset, pieceToMove);
			
			// Iterative update of passed pawns bitboard
			// Note: this needs to be done after the piece bit boards are updated
			// build up significant file masks, should be three or four consecutive files, re-evaluate passed pawns in those files
			long file_masks = 0L;
			if (pieceType == Piece.PAWN) {
				int ownLutColourIndex = isWhite ? 0 : 1;
				// Handle regular pawn pushes
				file_masks |= BitBoard.IterativePassedPawnNonCapture[ownLutColourIndex][originBitOffset];
				pawnHashUpdater.movePawn(pieceToMove, originBitOffset, targetBitOffset);
				
				// Handle pawn captures
				if (targetPiece != Piece.NONE) {
					if (Piece.isPawn(targetPiece)) {
						pawnHashUpdater.removePawn(targetPiece, captureBitOffset);
						// Pawn takes pawn, clears whole front-span of target pawn (note negation of colour)
						int enemyLutColourIndex = isWhite ? 1 : 0;
						file_masks |= BitBoard.PassedPawn_Lut[enemyLutColourIndex][targetBitOffset];
					}
					// manage file transition of capturing pawn moves
					boolean isLeft = BitBoard.getFile(targetBitOffset) < BitBoard.getFile(originBitOffset);
					file_masks |= BitBoard.IterativePassedPawnUpdateCaptures_Lut[originBitOffset][ownLutColourIndex][isLeft ? 0 : 1];
				}
			} else if (Piece.isPawn(targetPiece)) {
				pawnHashUpdater.removePawn(targetPiece, captureBitOffset);
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
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// check material incrementally updated against from scratch
			PiecewiseEvaluation scratch_me = new PiecewiseEvaluation();
			evaluateMaterial(scratch_me);
			long iterativeUpdatePassedPawns = passedPawns;
			createPassedPawnsBoard();
			assert iterativeUpdatePassedPawns == passedPawns :
				String.format("Passed Pawns error iterative %s != scratch %s move = %s pawns = %s", 
					BitBoard.toString(iterativeUpdatePassedPawns), BitBoard.toString(passedPawns), 
					Move.toString(move), BitBoard.toString(this.getPawns()));
			assert scratch_me != me;
			assert scratch_me.mg_material == me.mg_material;
			assert scratch_me.eg_material == me.eg_material;
			assert scratch_me.combinedPosition == me.combinedPosition : 
				String.format("combined_scratch=%08x iterative=%08x %s", 
						scratch_me.combinedPosition, me.combinedPosition, Move.toString(move));
			assert scratch_me.phase == me.phase;
			// Check piece bit boards to me num pieces consistency
			assert (me.numberOfPieces[Piece.WHITE_KNIGHT]+me.numberOfPieces[Piece.BLACK_KNIGHT]) == Long.bitCount(pieces[INDEX_KNIGHT]);
			assert (me.numberOfPieces[Piece.WHITE_BISHOP]+me.numberOfPieces[Piece.BLACK_BISHOP]) == Long.bitCount(pieces[INDEX_BISHOP]);
			assert (me.numberOfPieces[Piece.WHITE_ROOK]+me.numberOfPieces[Piece.BLACK_ROOK]) == Long.bitCount(pieces[INDEX_ROOK]);
			assert (me.numberOfPieces[Piece.WHITE_QUEEN]+me.numberOfPieces[Piece.BLACK_QUEEN]) == Long.bitCount(pieces[INDEX_QUEEN]);
			assert (me.numberOfPieces[Piece.WHITE_PAWN]+me.numberOfPieces[Piece.BLACK_PAWN]) == Long.bitCount(pieces[INDEX_PAWN]);
			assert Long.bitCount(pieces[INDEX_KING]) == 2;
		}
		
		return false;
	}
	
	public void undoMove(int moveToUndo) {
		isAttacksMaskValid = false;
		
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
			// and update piece list
			int fullPromotedPiece = (isWhite ? promotedPiece : promotedPiece|Piece.BLACK);
			me.updateWhenUndoingPromotion(fullPromotedPiece, originBitOffset, targetBitOffset);
		} else {
			// Piece type doesn't change across boards, update piece-specific bitboard, pieceLists and PST score
			pieces[pieceType] ^= positionsMask;
			me.updateRegular(pieceType, originPiece, originBitOffset, targetBitOffset);
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
			me.updateForReplacedCapture(targetPiece, capturedPieceSquare);
			insufficient = false;
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// check material incrementally updated against from scratch
			PiecewiseEvaluation scratch_me = new PiecewiseEvaluation();
			evaluateMaterial(scratch_me);
			assert scratch_me != me;
			assert scratch_me.mg_material == me.mg_material;
			assert scratch_me.eg_material == me.eg_material;
			assert scratch_me.combinedPosition == me.combinedPosition : 
				String.format("combined_scratch=%08x iterative=%08x %s",
						scratch_me.combinedPosition, me.combinedPosition, Move.toString(moveToUndo));
			assert scratch_me.phase == me.phase;
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
			me.updateRegular(Piece.ROOK, Piece.WHITE_ROOK, BitBoard.h1, BitBoard.f1);
			hashUpdater.doBasicMove(BitBoard.f1, BitBoard.h1, Piece.WHITE_ROOK);
		} else if (target == BitBoard.c1) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			me.updateRegular(Piece.ROOK, Piece.WHITE_ROOK, BitBoard.a1, BitBoard.d1);
			hashUpdater.doBasicMove(BitBoard.d1, BitBoard.a1, Piece.WHITE_ROOK);
		} else if (target == BitBoard.g8) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			me.updateRegular(Piece.ROOK, Piece.BLACK_ROOK, BitBoard.h8, BitBoard.f8);
			hashUpdater.doBasicMove(BitBoard.f8, BitBoard.h8, Piece.BLACK_ROOK);
		} else {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			me.updateRegular(Piece.ROOK, Piece.BLACK_ROOK, BitBoard.a8, BitBoard.d8);
			hashUpdater.doBasicMove(BitBoard.d8, BitBoard.a8, Piece.BLACK_ROOK);
		}
	}
	
	private void unperformSecondaryCastlingMove(int origin) {
		if (origin == BitBoard.g1) {
			pieces[INDEX_ROOK] ^= (wksc_mask);
			whitePieces ^= (wksc_mask);
			allPieces ^= (wksc_mask);
			me.updateRegular(Piece.ROOK, Piece.WHITE_ROOK, BitBoard.f1, BitBoard.h1);
			hashUpdater.doBasicMove(BitBoard.h1, BitBoard.f1, Piece.WHITE_ROOK);
		} else if (origin == BitBoard.c1) {
			pieces[INDEX_ROOK] ^= (wqsc_mask);
			whitePieces ^= (wqsc_mask);
			allPieces ^= (wqsc_mask);
			me.updateRegular(Piece.ROOK, Piece.WHITE_ROOK, BitBoard.d1, BitBoard.a1);
			hashUpdater.doBasicMove(BitBoard.a1, BitBoard.d1, Piece.WHITE_ROOK);
		} else if (origin == BitBoard.g8) {
			pieces[INDEX_ROOK] ^= (bksc_mask);
			blackPieces ^= (bksc_mask);
			allPieces ^= (bksc_mask);
			me.updateRegular(Piece.ROOK, Piece.BLACK_ROOK, BitBoard.f8, BitBoard.h8);
			hashUpdater.doBasicMove(BitBoard.h8, BitBoard.f8, Piece.BLACK_ROOK);
		} else {
			pieces[INDEX_ROOK] ^= (bqsc_mask);
			blackPieces ^= (bqsc_mask);
			allPieces ^= (bqsc_mask);
			me.updateRegular(Piece.ROOK, Piece.BLACK_ROOK, BitBoard.d8, BitBoard.a8);
			hashUpdater.doBasicMove(BitBoard.a8, BitBoard.d8, Piece.BLACK_ROOK);
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
		if (isAttacksMaskValid) {
			long kingMask = isWhite ? getWhiteKing() : getBlackKing();
			inCheck = (kingMask & mae.basic_attacks[isWhite ? 1 : 0][3][0]) != 0L;
		} else {
			int kingBitOffset = getKingPosition(isWhite);
			if (kingBitOffset != BitBoard.INVALID) {
				inCheck = squareIsAttacked(kingBitOffset, isWhite);
			}
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
	
	public int countDoubledPawns(long pawns) {
		int doubledCount = 0;
		for (int file : IntFile.values) {
			long pawnsInFile = pawns & BitBoard.FileMask_Lut[file];
			if (pawnsInFile != 0) {
				int numPawnsInFile = Long.bitCount(pawnsInFile);
				if (numPawnsInFile > 1) {
					doubledCount += numPawnsInFile-1;
				}
			}
		}
		return doubledCount;
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
	
	public boolean isFrontspanControlledInKpk(int bitOffset, boolean isWhite, long [] own_attacks) {
		boolean isControlled = false;
		long front_span_mask = BitBoard.PawnFrontSpan_Lut[isWhite ? 0 : 1][bitOffset];
		if (((front_span_mask & own_attacks[0]) ^ front_span_mask) == 0L) {
			// Don't need to check opponent attacks, because they can't attack the frontspan, ONLY VALID for KPK
			isControlled = true;
		}
		return isControlled;
	}
	
	private long generatePawnPushMask(int bitOffset, boolean isWhite) {
		long pawnMask = 1L << bitOffset;
		if (isWhite) {
			 pawnMask <<= 8;
		} else {
			pawnMask >>= 8;
		}
		return pawnMask;
	}
	
	public boolean isPawnBlockaded(int bitOffset, boolean isWhite) {
		// Check for enemy pieces blockading
		long pawnMask = generatePawnPushMask(bitOffset, isWhite);
		long enemy_pieces = isWhite ? blackPieces : whitePieces;
		return (pawnMask & enemy_pieces) != 0L;
	}
	
	public boolean isPawnFrontspanSafe(int bitOffset, boolean isWhite, long[] own_attacks, long[] enemy_attacks, boolean heavySupport) {
		boolean isClear = true;
		// Check frontspan is controlled
		long front_span_mask = BitBoard.PawnFrontSpan_Lut[isWhite ? 0 : 1][bitOffset];
		if (heavySupport) {
			// assume full x-ray control of the front span, simplification
			long [] own_xray = Arrays.copyOf(own_attacks, own_attacks.length);
			CountedBitBoard.setBits(own_xray, front_span_mask);
			if (!CountedBitBoard.weControlContestedSquares(own_xray, enemy_attacks, front_span_mask)) {
				isClear = false;
			}
		} else if (!CountedBitBoard.weControlContestedSquares(own_attacks, enemy_attacks, front_span_mask)) {
			isClear = false;
		}
		return isClear;
	}
	
	public boolean canPawnAdvance(int bitOffset, boolean isWhite, long[] own_attacks, long[] enemy_attacks) {
		long pawnMask = generatePawnPushMask(bitOffset, isWhite);
		return CountedBitBoard.weControlContestedSquares(own_attacks, enemy_attacks, pawnMask);
	}
	
	private boolean eval(boolean isWhite, long attacksOnRearSpanMask, long pawnMask) {
		// Evaluate the attacks for the rear span defender to see if it directly defends the pawn
		long attackerMask = 0L;
		if (isWhite) {
			attackerMask = BitBoard.upAttacks(attacksOnRearSpanMask, getEmpty());
		} else {
			attackerMask = BitBoard.downAttacks(attacksOnRearSpanMask, getEmpty());
		}
		if ((attackerMask & pawnMask) != 0L) {
			return true;
		}
		return false;
	}
	
	public int checkForHeavyPieceBehindPassedPawn(int bitOffset, boolean isWhite) {
		// The pawn may be attacked/defended by a rook or queen, directly along the rear span
		boolean isDefended = false;
		boolean isAttacked = false;
		long ownPawnMask = 1L << bitOffset;
		// Use the opposite colours' front span mask as a rear span mask
	    long rearSpanMask = BitBoard.PawnFrontSpan_Lut[!isWhite ? 0 : 1][bitOffset];
	    
		long ownHeavyPiecesInRearSpanMask = rearSpanMask & (isWhite ? getWhiteRankFile() : getBlackRankFile());
		if (ownHeavyPiecesInRearSpanMask != 0L) {
			// Evaluate the attacks for the rear span defender to see if it directly defends the pawn
			isDefended = eval(isWhite, ownHeavyPiecesInRearSpanMask, ownPawnMask);
		}
		if (!isDefended) {
			long enemyHeavyPiecesInRearSpanMask = rearSpanMask & (!isWhite ? getWhiteRankFile() : getBlackRankFile());
			if (enemyHeavyPiecesInRearSpanMask != 0L) {
				// Evaluate the attacks for the rear span attacker to see if it directly attacks the pawn
				isAttacked = eval(isWhite, enemyHeavyPiecesInRearSpanMask, ownPawnMask);
			}
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert !(isAttacked && isDefended) : "Passed pawn can't be simultaneously attacked and defended";
		}
		if (isAttacked) {
			return -1;
		} else if (isDefended) {
			return +1;
		} else {
			return 0;
		}
	}
	
	public boolean isCandidatePassedPawn(int bitOffset, boolean isWhite, long[] own_pawn_attacks, long[] enemy_pawn_attacks) {
		boolean isCandidate = true;
		// Check frontspan is clear
		long front_span_mask = BitBoard.PawnFrontSpan_Lut[isWhite ? 0 : 1][bitOffset];
		long otherSidePawns = isWhite ? getBlackPawns() : getWhitePawns();
		if ((front_span_mask & otherSidePawns) != 0) {
			isCandidate  = false;
		}
		if (isCandidate) {
			isCandidate = CountedBitBoard.weControlContestedSquares(own_pawn_attacks, enemy_pawn_attacks, front_span_mask);
		}
		return isCandidate;
	}
	
	public boolean isBackwardsPawn(int bitOffset, boolean isWhite) {
		boolean isBackwards = true;
		long mask = BitBoard.BackwardsPawn_Lut[isWhite ? 0 : 1][bitOffset];
		long ownSidePawns = isWhite ? getWhitePawns() : getBlackPawns();
		if ((mask & ownSidePawns) != 0) {
			isBackwards  = false;
		}
		return isBackwards;
	}
	
	public boolean isIsolatedPawn(int bitOffset, boolean isWhite) {
		boolean isIsolated = true;
		long mask = BitBoard.IsolatedPawn_Lut[bitOffset];
		long ownSidePawns = !isWhite ? getBlackPawns() : getWhitePawns();
		if ((mask & ownSidePawns) != 0) {
			isIsolated  = false;
		}
		return isIsolated;
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
	
	public boolean isOnHalfOpenFile(GenericPosition atPos, int type) {
		boolean isHalfOpen = false;
		long fileMask = BitBoard.FileMask_Lut[IntFile.valueOf(atPos.file)];
		long otherSide = Piece.isBlack(type) ? whitePieces : blackPieces;
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
	
	public long getBasicPawnAttacks(boolean attackerIsBlack) {
		long attackMask = 0L;
		long side = attackerIsBlack ? blackPieces : whitePieces;
		long scratchBitBoard = pieces[Piece.PAWN] & side;
		if (attackerIsBlack) {
			attackMask |= BitBoard.downRightAttacks(scratchBitBoard);
			attackMask |= BitBoard.downLeftAttacks(scratchBitBoard);
		} else {
			attackMask |= BitBoard.upRightAttacks(scratchBitBoard);
			attackMask |= BitBoard.upLeftAttacks(scratchBitBoard);
		}
		return attackMask;
	}
	
	public void getCountedPawnAttacks(long[] attacksMask, boolean attackerIsBlack) {
		long side = attackerIsBlack ? blackPieces : whitePieces;
		long scratchBitBoard = pieces[Piece.PAWN] & side;
		long attacks = 0L;
		if (attackerIsBlack) {
			attacksMask[0] |= BitBoard.downRightAttacks(scratchBitBoard);
			attacks = BitBoard.downLeftAttacks(scratchBitBoard);
		} else {
			attacksMask[0] |= BitBoard.upRightAttacks(scratchBitBoard);
			attacks = BitBoard.upLeftAttacks(scratchBitBoard);
		}
		// Need to find which square(s) are attacked twice and set them in the second mask,
		// optimised for pawns, where only two squares can be simultaneously attacked by a side
		attacksMask[1] |= attacks & attacksMask[0];
		attacksMask[0] |= attacks;
	}
	
	public class CountedPawnKnightAttackAggregator implements IForEachPieceCallback {
		
		public final int[] BLACK_ATTACKERS = {Piece.BLACK_PAWN, Piece.BLACK_KNIGHT};
		public final int[] WHITE_ATTACKERS = {Piece.WHITE_PAWN, Piece.WHITE_KNIGHT};
		
		long [] attackMask;
		
		public void callback(int piece, int bitOffset) {
			long mask = 0L;
			switch(piece) {
			case Piece.WHITE_PAWN:
				mask = SquareAttackEvaluator.WhitePawnAttacksFromPosition_Lut[bitOffset];
				break;
			case Piece.BLACK_PAWN:
				mask = SquareAttackEvaluator.BlackPawnAttacksFromPosition_Lut[bitOffset];
				break;
			case Piece.WHITE_KNIGHT:
			case Piece.BLACK_KNIGHT:
				mask = SquareAttackEvaluator.KnightMove_Lut[bitOffset];
				break;
			default:
				break;
			}
			CountedBitBoard.setBits(attackMask, mask);
		}
		
		@Override
		public boolean condition_callback(int piece, int atPos) {
			return false;
		}
		
		public void getAttacks(long[] attacks, boolean attackerIsBlack) {
			this.attackMask = attacks;
			CountedBitBoard.clear(attackMask);
			for (int piece : attackerIsBlack ? BLACK_ATTACKERS: WHITE_ATTACKERS) {
				long side = attackerIsBlack ? blackPieces : whitePieces;
				long scratchBitBoard = pieces[piece&Piece.PIECE_NO_COLOUR_MASK] & side;
				
				int bit_offset = BitBoard.INVALID;
				while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
					this.callback(piece, bit_offset);
					scratchBitBoard ^= (1L << bit_offset);
				}
			}
		}
	}
	
	public long getBasicKnightAttacks(boolean attackerIsBlack) {
		long attackMask = 0L;
		long scratchBitBoard = attackerIsBlack ? getBlackKnights() : getWhiteKnights();
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			attackMask |= SquareAttackEvaluator.KnightMove_Lut[bit_offset];
			scratchBitBoard ^= (1L << bit_offset);
		}
		return attackMask;
	}

	public void getCountedKnightAttacks(long[] attacks, boolean attackerIsBlack) {
		long scratchBitBoard = attackerIsBlack ? getBlackKnights() : getWhiteKnights();
		while (scratchBitBoard != 0x0L) {
			int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
			long mask = SquareAttackEvaluator.KnightMove_Lut[bit_offset];
			CountedBitBoard.setBits(attacks, mask);
			scratchBitBoard ^= (1L << bit_offset);
		}
	}
	
	public boolean whiteSingleMoveEndgame(IAddMoves ml) {
		long side = whitePieces;
		long scratchBitBoard = pieces[Piece.KING] & side;
		int bit_offset = BitBoard.INVALID;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			Piece.king_generateMoves_White(ml, this, bit_offset);
		}
		if (ml.isLegalMoveFound()) {
			return true;
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R7]);
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_White(ml, this, bit_offset);
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
		scratchBitBoard = pieces[Piece.BISHOP] & side;
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
		return false;
	}
	
	public boolean blackSingleMoveEndgame(IAddMoves ml) {
		long side = blackPieces;
		long scratchBitBoard = pieces[Piece.KING] & side;
		int bit_offset = BitBoard.INVALID;
		if (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {			
			Piece.king_generateMoves_Black(ml, this, bit_offset);
		}
		if (ml.isLegalMoveFound()) {
			return true;
		}
		scratchBitBoard = pieces[Piece.PAWN] & side & (~BitBoard.RankMask_Lut[IntRank.R2]);;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			Piece.pawn_generateMoves_Black(ml, this, bit_offset);
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
		scratchBitBoard = pieces[Piece.BISHOP] & side;
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
		return false;
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
		if (me.isEndgame()) {
			if (ownSideIsWhite) {
				whiteSingleMoveEndgame(ml);
			} else {
				blackSingleMoveEndgame(ml);
			}
		} else {
			if (ownSideIsWhite) {
				whiteSingleMoveMidgame(ml);
			} else {
				blackSingleMoveMidgame(ml);
			}
		}
	}
	
	public void getRegularPieceMoves(IAddMoves ml, boolean ownSideIsWhite) {
		if (me.isEndgame()) {
			if (ownSideIsWhite) {
				whiteEndgame(ml);
			} else {
				blackEndgame(ml);
			}
		} else {
			if (ownSideIsWhite) {
				whiteMidgame(ml);
			} else {
				blackMidgame(ml);
			}
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
		if (me.isEndgame()) {
			if (isWhite) {
				addMoves_CapturesExcludingPawnPromotions_White_Endgame(ml);
			} else {
				addMoves_CapturesExcludingPawnPromotions_Black_Endgame(ml);
			}
		} else {
			if (isWhite) {
				addMoves_CapturesExcludingPawnPromotions_White(ml);
			} else {
				addMoves_CapturesExcludingPawnPromotions_Black(ml);
			}
		}
	}
	
	private boolean blackHasEnPrisePiece(long blackAttacks) {
		long blacks_pieces_except_king = blackPieces & ~pieces[Piece.KING];
		return (blacks_pieces_except_king & ~blackAttacks) != 0L;
	}
	
	private boolean whiteHasEnPrisePiece(long whiteAttacks) {
		long whites_pieces_except_king = whitePieces & ~pieces[Piece.KING];
		return (whites_pieces_except_king & ~whiteAttacks) != 0L;
	}
	
	public boolean potentialKnightForkOnEnemyKing(boolean onMoveIsWhite) {
		int kingBitOffset = this.getKingPosition(!onMoveIsWhite);
		long enemyKnights = pieces[Piece.KNIGHT] & (onMoveIsWhite ? whitePieces : blackPieces);
		return (enemyKnights & SquareAttackEvaluator.KnightForks_Lut[kingBitOffset]) != 0L;
	}
	
	public boolean potentialKnightCheck(boolean onMoveIsWhite) {
		int kingBitOffset = this.getKingPosition(onMoveIsWhite);
		long enemyKnights = pieces[Piece.KNIGHT] & (onMoveIsWhite ? blackPieces : whitePieces);
		return (enemyKnights & SquareAttackEvaluator.KnightForks_Lut[kingBitOffset]) != 0L;
	}
	
	public boolean isLikelyDrawnEndgame(boolean onMoveIsWhite) {
		// Possible promotions
		if (pieces[Piece.PAWN] != 0)
			return false;
		
//		if (me.phase < 2624)
//			return false;
		boolean possiblyDrawn = false;
		
		// Minor pieces
		int numWhiteBishops = me.numberOfPieces[Piece.WHITE_BISHOP];
		int numWhiteKnights = me.numberOfPieces[Piece.WHITE_KNIGHT];
		int numBlackBishops = me.numberOfPieces[Piece.BLACK_BISHOP];
		int numBlackKnights = me.numberOfPieces[Piece.BLACK_KNIGHT];
		int numWhiteMinor = numWhiteBishops + numWhiteKnights;
		int numBlackMinor = numBlackBishops + numBlackKnights;
		
		if (pieces[Piece.QUEEN] == 0) {
			int numWhiteRooks = me.numberOfPieces[Piece.WHITE_ROOK];
			int numBlackRooks = me.numberOfPieces[Piece.BLACK_ROOK];
			// (R vs 2 minor) or (R Minor vs Minor)
			if (numWhiteRooks == 1 && numWhiteMinor < 2) {
				// "R vs 2 minor" or "R Minor vs 2 minor"
				if (numBlackRooks == 0 && numBlackMinor == 2) {
					possiblyDrawn = true;
				}
				// "R Minor vs r" or "R vs r"
				if (numBlackRooks == 1 && numBlackMinor == 0) {
					possiblyDrawn = true;
				}
			}
			if (numBlackRooks == 1 && numBlackMinor < 2) {
				// "R vs 2 minor"
				if (numWhiteRooks == 0 && numWhiteMinor == 2) {
					possiblyDrawn = true;
				}				
				// R vs R Minor
				if (numWhiteRooks == 1 && numWhiteMinor == 0) {
					possiblyDrawn = true;
				}
			}	
			if (!possiblyDrawn && (numWhiteRooks != 0 || numBlackRooks != 0)) {
				// at least one rook on the board
				return false;
			}
		} else {
			if (pieces[Piece.ROOK] == 0) {
				
				int numWhiteQueens = me.numberOfPieces[Piece.WHITE_QUEEN];		
				int numBlackQueens = me.numberOfPieces[Piece.BLACK_QUEEN];
				// Q vs 2 minor
				if (numWhiteQueens == 1 && numBlackQueens == 0 && numBlackMinor >= 2) {
					possiblyDrawn = true;
				}
				if (numBlackQueens == 1 && numWhiteQueens == 0 && numWhiteMinor >= 2) {
					possiblyDrawn = true;
				}
				// Q minor vs Q
				if (numWhiteQueens == 1 && numBlackQueens == 1 && numBlackMinor == 0 && numWhiteMinor == 1) {
					possiblyDrawn = true;
				}
				if (numBlackQueens == 1 && numWhiteQueens == 1 && numWhiteMinor == 0 && numBlackMinor == 1) {
					possiblyDrawn = true;
				}
			}
			// At least one queen on the board
			if (!possiblyDrawn)
				return false;
		}
		if (possiblyDrawn) {
			// Accounts for pieces that can be taken on the next move, but doesn't account for check forks of en prise pieces
			long [][][] attacks = mae.calculateBasicAttacksAndMobility(me);
			if(onMoveIsWhite) {
				if ((attacks[0][3][0] & blackPieces) != 0L ||
					blackHasEnPrisePiece(attacks[1][3][0]) ||
					potentialKnightForkOnEnemyKing(onMoveIsWhite))
					return false;
			} else {
				if ((attacks[1][3][0] & whitePieces) != 0L ||
					whiteHasEnPrisePiece(attacks[0][3][0]) ||
					potentialKnightForkOnEnemyKing(onMoveIsWhite))
					return false;
			}
		}
		
		if (numWhiteBishops >= 2 || numBlackBishops >= 2) {
			// One side has at least two bishops
			return false;
		}
		if ((numWhiteBishops == 1 && numWhiteKnights >= 1) ||
		    (numBlackBishops == 1 && numBlackKnights >= 1))
			// One side has Knight and Bishop
			return false;
		
		// else insufficient material
		return true;
	}
	
	public boolean isLikelyDrawnEndgame(boolean onMoveIsWhite, long [][][] attacks) {
		// Possible promotions
		if (pieces[Piece.PAWN] != 0)
			return false;
		
		if (me.phase < 2624)
			return false;
		
		boolean possiblyDrawn = false;
		
		// Minor pieces
		int numWhiteBishops = me.numberOfPieces[Piece.WHITE_BISHOP];
		int numWhiteKnights = me.numberOfPieces[Piece.WHITE_KNIGHT];
		int numBlackBishops = me.numberOfPieces[Piece.BLACK_BISHOP];
		int numBlackKnights = me.numberOfPieces[Piece.BLACK_KNIGHT];
		int numWhiteMinor = numWhiteBishops + numWhiteKnights;
		int numBlackMinor = numBlackBishops + numBlackKnights;
		
		if (pieces[Piece.QUEEN] == 0) {
			int numWhiteRooks = me.numberOfPieces[Piece.WHITE_ROOK];
			int numBlackRooks = me.numberOfPieces[Piece.BLACK_ROOK];
			// (R vs 2 minor) or (R Minor vs Minor)
			if (numWhiteRooks == 1 && numWhiteMinor < 2) {
				// "R vs 2 minor" or "R Minor vs 2 minor"
				if (numBlackRooks == 0 && numBlackMinor == 2) {
					possiblyDrawn = true;
				}
				// "R Minor vs r" or "R vs r"
				if (numBlackRooks == 1 && numBlackMinor == 0) {
					possiblyDrawn = true;
				}
			}
			if (numBlackRooks == 1 && numBlackMinor < 2) {
				// "R vs 2 minor"
				if (numWhiteRooks == 0 && numWhiteMinor == 2) {
					possiblyDrawn = true;
				}				
				// R vs R Minor
				if (numWhiteRooks == 1 && numWhiteMinor == 0) {
					possiblyDrawn = true;
				}
			}	
			if (!possiblyDrawn && (numWhiteRooks != 0 || numBlackRooks != 0)) {
				// at least one rook on the board
				return false;
			}
		} else {
			if (pieces[Piece.ROOK] == 0) {
				
				int numWhiteQueens = me.numberOfPieces[Piece.WHITE_QUEEN];		
				int numBlackQueens = me.numberOfPieces[Piece.BLACK_QUEEN];
				// Q vs 2 minor
				if (numWhiteQueens == 1 && numBlackQueens == 0 && numBlackMinor >= 2) {
					possiblyDrawn = true;
				}
				if (numBlackQueens == 1 && numWhiteQueens == 0 && numWhiteMinor >= 2) {
					possiblyDrawn = true;
				}
				// Q minor vs Q
				if (numWhiteQueens == 1 && numBlackQueens == 1 && numBlackMinor == 0 && numWhiteMinor == 1) {
					possiblyDrawn = true;
				}
				if (numBlackQueens == 1 && numWhiteQueens == 1 && numWhiteMinor == 0 && numBlackMinor == 1) {
					possiblyDrawn = true;
				}
			}
			// At least one queen on the board
			if (!possiblyDrawn)
				return false;
		}
		if (possiblyDrawn) {
			if(onMoveIsWhite) {
				if ((attacks[0][3][0] & blackPieces) != 0L ||
					blackHasEnPrisePiece(attacks[1][3][0]) ||
					potentialKnightForkOnEnemyKing(onMoveIsWhite))
					return false;
			} else {
				if ((attacks[1][3][0] & whitePieces) != 0L ||
					whiteHasEnPrisePiece(attacks[0][3][0]) ||
					potentialKnightForkOnEnemyKing(onMoveIsWhite))
					return false;
			}
		}
		return possiblyDrawn;
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
	
	public void forEachPawnOfSide(IForEachPieceCallback caller, boolean isBlack) {
		long side = isBlack ? blackPieces : whitePieces;
		int piece = isBlack ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
		long scratchBitBoard = pieces[Piece.PAWN] & side;
		int bit_offset = BitBoard.INVALID;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {
			caller.callback(piece, bit_offset);
			scratchBitBoard ^= (1L << bit_offset);
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
	
	public boolean isAdvancedPassedPawnPresent() {
		if (passedPawns == 0L)
			return false;
		long advanced_white = 0x00FF_FFFF_0000_0000L;
		boolean advanced_passer = (passedPawns & whitePieces & advanced_white) != 0L;
		if (!advanced_passer) {
			long advanced_black = 0x0000_0000_FFFF_FF00L;
			advanced_passer = (passedPawns & blackPieces & advanced_black) != 0L;
		}
		return advanced_passer;
	}
	
	public void setHash(IZobristUpdate hash) {
		this.hashUpdater = hash;
	}
	
	public void setPawnHash(IPawnHash hash) {
		this.pawnHashUpdater = hash;
	}
	
	public int getPieces() {
		return Long.bitCount(allPieces);
	}
}
