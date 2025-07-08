package eubos.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.PositionManager;

public class NnueTrainingDataTest extends AbstractEubosIntegration{
			
	public class IndividualTrainingDataPosition {
		String fen;
		Integer score;
		PositionManager pm;
		boolean isWhite;
				
		String fen_regex =
				"([KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/]" +
				"[KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+ " +
				"[bw] [KkQq-]+ [a-h1-8-]+)";
		
		private void extractFen(String entry) throws IllegalNotationException {
			Pattern pattern = Pattern.compile(fen_regex);
			Matcher matcher = pattern.matcher(entry);
			if (matcher.find()) {
			    fen = matcher.group(1);
			} else {
				throw new IllegalNotationException(String.format("Can't match FEN %s", entry));
			}
		}
		
		private void extractScore(String entry) throws IllegalNotationException {
			int scoreIndex = entry.indexOf("|") + 1;
			int endOfBestMoveIndex = entry.indexOf("|0.5");
			score = Integer.valueOf(entry.substring(scoreIndex, endOfBestMoveIndex));
		}
		
		public IndividualTrainingDataPosition(String epd) throws IllegalNotationException {
			extractFen(epd);
			pm = new PositionManager(fen+" 0 0");
			isWhite = pm.onMoveIsWhite();
			extractScore(epd);
		}
	};
	
	public boolean runTest(IndividualTrainingDataPosition test) throws IOException, InterruptedException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+test.fen+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX));
		return trainingDataMonitor(test.isWhite ? test.score : -test.score);
	}
	
	public void runThroughDataFile(String filename) throws IllegalNotationException, IOException, InterruptedException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
			Path resourceDirectory = Paths.get("src","test","resources");
			String absolutePath = resourceDirectory.toFile().getAbsolutePath();
			int numPassed = 0, total = 0;
			try {
				Scanner scanner = new Scanner(new File(absolutePath+"/"+filename));
				while (scanner.hasNextLine()) {
					try {
						IndividualTrainingDataPosition entry = new IndividualTrainingDataPosition(scanner.nextLine());
						startupEngine(String.format("entry %d", total));
						boolean passed = runTest(entry);
						if (passed) numPassed++;
						total++;
						System.err.println(String.format("passed %d of %d --- overall pass rate is %2.1f%%",
								numPassed, total, numPassed*100.0f/total));
						shutdownEngine();
						commands.clear();
					} catch (IllegalNotationException e) {
						e.printStackTrace();
					}
				}
				scanner.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
		
	@Test
	public void test_verify_nn_training() throws IOException, InterruptedException, IllegalNotationException {
		runThroughDataFile("random_moves_1.txt");		
	}
}
