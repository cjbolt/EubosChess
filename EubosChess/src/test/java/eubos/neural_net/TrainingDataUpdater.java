package eubos.neural_net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;

public class TrainingDataUpdater {
	
	protected MiniMaxMoveGenerator classUnderTest;
	protected FixedSizeTranspositionTable hashMap;
	PositionManager pm;
	EubosEngineMain eubos_stub;
	
	@Before
	public void setUp() {
		EubosEngineMain.logger.setLevel(Level.OFF);
		eubos_stub = new EubosEngineMain();
	}
	
	private int getScoreForDepth(int searchDepth) {
		SearchResult res = classUnderTest.findMove((byte)searchDepth);
		return res.score;
	}

	protected void setupPosition(String fen) {
		pm = new PositionManager( fen );
		hashMap = new FixedSizeTranspositionTable();
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm, pm);
		classUnderTest.setEngineCallback(eubos_stub);
	}	

	@Test
	@Ignore
	public void test_UpdateReferenceTrainingData()throws IllegalNotationException, IOException {
		// Load each group of training data in turn (1M positions at a time)
		File[] files = new File("D:\\tester\\out").listFiles();
		for (File filename : files) {
			
			// Read each file in, then position by position, update the score
			try (BufferedReader in = new BufferedReader(new FileReader(filename));
				 PrintWriter out = new PrintWriter("D:\\updated\\"+filename.getName(), "UTF-8")) {
				String entry;
				while ((entry = in.readLine()) != null) {
					
					String [] tokens = entry.split("\\|");
					String fen = tokens[0];
					
					setupPosition(fen);
					boolean need_to_invert_score = (pm.onMoveIsWhite()) ? false : true;
					int score = getScoreForDepth(5);
					if (need_to_invert_score) score = -score;
					
					String updated_entry = fen + "|" + Integer.toString(score) + "|0.5";
				    out.println(updated_entry);
				    
				    System.out.print('.');
				}
			}
		}
	}
}
