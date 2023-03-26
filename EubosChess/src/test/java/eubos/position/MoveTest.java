package eubos.position;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eubos.score.PawnEvalHashTable;
import eubos.search.DrawChecker;

public class MoveTest {
	MoveList ml;
	
	protected void setUpPosition(String fen) {
		PositionManager pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
		ml = new MoveList(pm, 1);
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void test_template() throws InterruptedException {
		setUpPosition("k7/8/8/8/8/8/8/6RK w - - 0 25 ");
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		MoveListIterator normal_it = ml.getNextMovesAtPly(0);
		do {
			do {
				normal_it.nextInt();
			} while (normal_it.hasNext());
			normal_it = ml.getNextMovesAtPly(0);
		} while (normal_it.hasNext());
	}
}
