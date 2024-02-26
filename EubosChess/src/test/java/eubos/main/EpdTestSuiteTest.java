package eubos.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;
import eubos.position.PositionManager;

public class EpdTestSuiteTest extends AbstractEubosIntegration{
			
	public class IndividualTestPosition {
		String fen;
		List<Integer> bestMoves;
		List<String> bestMoveCommands;
		String testName;
		PositionManager pm;
		boolean isWhite;
				
		String fen_regex =
				"([KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/]" +
				"[KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+[/][KkQqRrBbNnPp1-8]+ " +
				"[bw] [KkQq-]+ [a-h1-8-]+)";
		
		private void extractFen(String epd) throws IllegalNotationException {
			Pattern pattern = Pattern.compile(fen_regex);
			Matcher matcher = pattern.matcher(epd);
			if (matcher.find()) {
			    fen = matcher.group(1);
			} else {
				throw new IllegalNotationException(String.format("Can't match FEN %s", epd));
			}
		}
		
		private void extractBestMoves(String epd) throws IllegalNotationException {
			int bestMoveIndex = epd.indexOf("bm ");
			String rest = epd.substring(bestMoveIndex+"bm ".length());
			int endOfBestMoveIndex = rest.indexOf(";");
			int x = bestMoveIndex+"bm ".length();
			
			String [] bestMovesAsString = epd.substring(x, x+endOfBestMoveIndex).split(" ");
			
			// Create a list of the valid moves in the position
			MoveList ml = new MoveList(pm, 0);
			MoveListIterator it = ml.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), false, 0);
			List<Integer> moveList = ml.getList(it);
			
			for (String bestMove : bestMovesAsString) {
				int current = MoveList.getNativeMove(isWhite, moveList, bestMove);
				if (current == Move.NULL_MOVE) {
					throw new IllegalNotationException(String.format("%s didn't match any Eubos moves at fen=%s", bestMove, fen));
				}
				bestMoves.add(current);
				bestMoveCommands.add(BEST_PREFIX+Move.toGenericMove(current).toString()+CMD_TERMINATOR);
			}
		}
		
		private void extractTestName(String epd) {
			int idIndex = epd.indexOf("id ");
			String rest = epd.substring(idIndex+"id ".length());
			int endOfIdIndex = rest.indexOf(";");
			int x = idIndex+"id ".length();
			testName = epd.substring(x, x+endOfIdIndex);
		}
		
		public IndividualTestPosition(String epd) throws IllegalNotationException {
			bestMoves = new ArrayList<Integer>();
			bestMoveCommands = new ArrayList<String>();
			extractFen(epd);
			pm = new PositionManager(fen+" 0 0");
			isWhite = Piece.Colour.isWhite(pm.getOnMove());
			extractBestMoves(epd);
			extractTestName(epd);
		}
	};
	
	public List<IndividualTestPosition> loadTestSuiteFromEpd(String filename) throws IllegalNotationException {
		List<IndividualTestPosition> testList = new ArrayList<IndividualTestPosition>();
		Path resourceDirectory = Paths.get("src","test","resources");
		String absolutePath = resourceDirectory.toFile().getAbsolutePath();
		try {
			Scanner scanner = new Scanner(new File(absolutePath+"/"+filename));
			while (scanner.hasNextLine()) {
				try {
					IndividualTestPosition pos = new IndividualTestPosition(scanner.nextLine());
					testList.add(pos);
				} catch (IllegalNotationException e) {
					e.printStackTrace();
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return testList;
	}
	
	public boolean runTest(IndividualTestPosition test) throws IOException, InterruptedException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+test.fen+CMD_TERMINATOR, null));
		String[] myArray = new String[test.bestMoveCommands.size()];
		myArray = test.bestMoveCommands.toArray(myArray);
		commands.add(new MultipleAcceptableCommandPair(GO_TIME_PREFIX+"10000"+CMD_TERMINATOR, myArray));
		return performTest(12000);
	}
	
	public void runThroughTestSuite(String filename) throws IOException, InterruptedException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
			List<IndividualTestPosition> testSuite = loadTestSuiteFromEpd(filename);
			for (IndividualTestPosition test : testSuite) {
				System.err.println(String.format("Starting %s", test.testName));
				startupEngine(test.testName);
				boolean passed = runTest(test);
				System.err.println(String.format("Completed %s %s", test.testName, passed ? "Passed":"Failed"));
				shutdownEngine();
				commands.clear();
			}
		}
	}
	
	@Test
	public void test_run_wac_test_suite() throws IOException, InterruptedException, IllegalNotationException {
		runThroughTestSuite("wacnew.epd");		
	}
	
	@Test
	public void test_run_null_move_test_suite() throws IOException, InterruptedException, IllegalNotationException {
		runThroughTestSuite("null_move_test.epd");		
	}
	
	@Test
	public void test_run_bratko_kopec_test_suite() throws IOException, InterruptedException, IllegalNotationException {
		runThroughTestSuite("bratko_kopec_test.epd");		
	}
	
	@Test
	public void test_run_silent_but_deadly_test_suite() throws IOException, InterruptedException, IllegalNotationException {
		runThroughTestSuite("silent_but_deadly.epd");		
	}
	
	@Test
	public void test_run_ccr_one_hour_test_suite() throws IOException, InterruptedException, IllegalNotationException {
		runThroughTestSuite("ccr_1hr_test.epd");		
	}
	
	@Test
	public void test_run_kaufman_test_suite() throws IOException, InterruptedException, IllegalNotationException {
		runThroughTestSuite("kaufman_test.epd");		
	}
}
