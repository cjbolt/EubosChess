package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.search.Score;

public class Transposition implements ITransposition {
	protected short score;
	protected int bitfield;

	public Transposition(byte depth, short score, byte bound, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, bound, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), null);
	}
	
	public Transposition(byte depth, short score, byte bound, int bestMove, List<Integer> pv) {
		setDepthSearchedInPly(depth);
		setScore(score);
		setType(bound);
		setBestMove(bestMove);
	}
	
	@Override
	public byte getDepthSearchedInPly() {
		return (byte)((bitfield >>> 0) & 0x3F);
	}

	protected void setDepthSearchedInPly(byte depthSearchedInPly) {
		short limitedDepth = (short)Math.min(0x3F, depthSearchedInPly);
		bitfield &= ~(0x3F << 0);
		bitfield |= (short)((limitedDepth & 0x3F) << 0);
	}
	
	@Override
	public byte getType() {
		return (byte)((bitfield >>> 6) & 0x3);
	}

	protected void setType(byte type) {
		bitfield &= ~(0x3 << 6);
		bitfield |= (short)((type & 0x3) << 6);
	}
	
	@Override
	public short getScore() {
		return score;
	}

	protected void setScore(short new_score) {
		this.score = new_score;
	}

	@Override
	public int getBestMove(Board theBoard) {
		int origin = ((bitfield >>> 12) & 0x7F);
		int target = ((bitfield >>> 19) & 0x7F);
		int promo = ((bitfield >>> 26) & 0x7);
		int trans_move = Move.valueOfPromotion(origin, target, promo);
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert trans_move != Move.NULL_MOVE : "Tranposition move was null.";
			assert theBoard != null;
		}
		
		// Populate the members of the move read from the transposition table.
		int originPiece = theBoard.getPieceAtSquare(origin);
		trans_move = Move.setOriginPiece(trans_move, originPiece);
		int targetPiece = theBoard.getPieceAtSquare(target);
		if (targetPiece != Piece.NONE) {
			// Regular capture
			trans_move = Move.setCapture(trans_move, targetPiece);
		} else if (Piece.isPawn(originPiece) && theBoard.getEnPassantTargetSq() == target) {
			// En Passant capture
			int enPassantCaptureSquare = theBoard.generateCapturePositionForEnPassant(originPiece, target);
			targetPiece = theBoard.getPieceAtSquare(enPassantCaptureSquare);
			if (Piece.isPawn(targetPiece)) {
				trans_move = Move.setCapture(trans_move, targetPiece);
				trans_move |= Move.MISC_EN_PASSANT_CAPTURE_MASK;
			}
		}
		return Move.setBest(trans_move);
	}
	
	@Override
	public int getBestMove() {
		int origin = ((bitfield >>> 12) & 0x7F);
		int target = ((bitfield >>> 19) & 0x7F);
		int promo = ((bitfield >>> 26) & 0x7);
		int trans_move = Move.valueOfPromotion(origin, target, promo);
		
		return trans_move;
	}
	
	protected void setBestMove(int bestMove) {
		int origin = Move.getOriginPosition(bestMove);
		int target = Move.getTargetPosition(bestMove);
		int promo = Move.getPromotion(bestMove);
		bitfield &= ~(0x1FFFF << 12);
		bitfield |= ((origin & 0x7F) << 12);
		bitfield |= ((target & 0x7F) << 19);
		bitfield |= ((promo & 0x7) << 26);
		if (EubosEngineMain.ENABLE_ASSERTS) {
			int decode_origin = ((bitfield >>> 12) & 0x7F);
			int decode_target = ((bitfield >>> 19) & 0x7F);
			int decode_promo = ((bitfield >>> 26) & 0x7);
			assert origin == decode_origin;
			assert target == decode_target;
			assert promo == decode_promo;
		}
	}
	
	@Override
	public String report() {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s", 
				Move.toGenericMove(getBestMove()),
				getDepthSearchedInPly(),
				Score.toString(score),
				getType());
		return output;
	}
	
	@Override
	public synchronized boolean checkUpdate(
			byte new_Depth, 
			short new_score,
			byte new_bound,
			int new_bestMove, 
			List<Integer> pv) {	
		boolean updateTransposition = false;
		if (getDepthSearchedInPly() < new_Depth) {
			updateTransposition = true;	
		} else if (getDepthSearchedInPly() == new_Depth) {
			if (getType() != Score.exact) {
				updateTransposition = true;
			} else {
				// don't update, worse bound score than we currently have
			}
		} else {
			// don't update, depth is less than what we have
		}
		if (updateTransposition) {
			setDepthSearchedInPly(new_Depth);
			score = new_score;
			setType(new_bound);
			setBestMove(new_bestMove);
		}
		return updateTransposition;
	}
		
	public List<Integer> getPv() {
		return null;
	}
}
