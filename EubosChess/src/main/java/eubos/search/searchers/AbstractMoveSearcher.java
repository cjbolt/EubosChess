package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.score.PositionEvaluator;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public abstract class AbstractMoveSearcher extends Thread {

	protected EubosEngineMain eubosEngine;
	protected IChangePosition pm;
	protected IPositionAccessors pos;
	protected MiniMaxMoveGenerator mg;
	protected short initialScore;

	public AbstractMoveSearcher(EubosEngineMain eng, IChangePosition pm, IPositionAccessors pos, FixedSizeTranspositionTable hashMap) {
		super();
		this.eubosEngine = eng;
		this.pm = pm;
		this.pos = pos;
		PositionEvaluator pe = new PositionEvaluator(pos);
		initialScore = pe.evaluatePosition().getScore();
		pos.RegisterPositionEvaluator(pe);
		if (Colour.isBlack(pos.getOnMove())) {
			initialScore = (short)-initialScore;
		}
		this.mg = new MiniMaxMoveGenerator( eng, hashMap, pm, pos , pe);
	}

	public AbstractMoveSearcher(Runnable target) {
		super(target);
	}

	public AbstractMoveSearcher(String name) {
		super(name);
	}

	public AbstractMoveSearcher(ThreadGroup group, Runnable target) {
		super(group, target);
	}

	public abstract void run();

	public abstract void halt();

	protected SearchResult doFindMove(GenericMove selectedMove, List<Integer> pc, byte depth) {
		SearchResult res = null;
		try {
			res = mg.findMove(depth, pc);
		} catch( NoLegalMoveException e ) {
			EubosEngineMain.logger.info(
					String.format("AbstractMoveSearcher out of legal moves for %s", pos.getOnMove()));
		} catch(InvalidPieceException e ) {
			EubosEngineMain.logger.info(
					String.format("AbstractMoveSearcher can't find piece at %s", e.getAtPosition()));
		}
		return res;
	}

	public AbstractMoveSearcher(ThreadGroup group, String name) {
		super(group, name);
	}

	public AbstractMoveSearcher(Runnable target, String name) {
		super(target, name);
	}

	public AbstractMoveSearcher(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}

	public AbstractMoveSearcher(ThreadGroup group, Runnable target, String name,
			long stackSize) {
		super(group, target, name, stackSize);
	}

}