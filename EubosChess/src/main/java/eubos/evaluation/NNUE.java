package eubos.evaluation;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import eubos.board.Board;
import eubos.board.Board.NetInput;
import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;

public class NNUE
{	
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	
	private static final int COLOR_STRIDE = 64 * 6;
	private static final int PIECE_STRIDE = 64;

	private static final int HIDDEN_SIZE = 128;
	private static final int FEATURE_SIZE = 768;

	private static final int SCALE = 400;
	private static final int QA = 255;
	private static final int QB = 64;

	private static short[][] L1Weights;
	private static short[] L1Biases;
	private static short[] L2Weights;
	private static short outputBias;

	static {
		try {
			InputStream is = NNUE.class.getResourceAsStream("/resources/quantised.bin");
			if (is == null) {
				is = NNUE.class.getResourceAsStream("/quantised.bin");
			}
			BufferedInputStream in = new BufferedInputStream(is, 16*4096);
			DataInputStream networkData = new DataInputStream(in);
			loadNetwork(networkData);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void loadNetwork(DataInputStream networkData) throws IOException {
		L1Weights = new short[FEATURE_SIZE][HIDDEN_SIZE];
		for (int i = 0; i < FEATURE_SIZE; i++) {
			for (int j = 0; j < HIDDEN_SIZE; j++)
				L1Weights[i][j] = toLittleEndian(networkData.readShort());
		}

		L1Biases = new short[HIDDEN_SIZE];
		for (int i = 0; i < HIDDEN_SIZE; i++) {
			L1Biases[i] = toLittleEndian(networkData.readShort());
		}

		L2Weights = new short[HIDDEN_SIZE * 2];
		for (int i = 0; i < HIDDEN_SIZE * 2; i++) {
			L2Weights[i] = toLittleEndian(networkData.readShort());
		}

		outputBias = toLittleEndian(networkData.readShort());
		networkData.close();
	}

	private static short toLittleEndian(short input) {
		return (short) (((input & 0xFF) << 8) | ((input & 0xFF00) >> 8));
	}
	
	private final static int screlu[] = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];
	static {
		for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE;i ++) {
			screlu[i - (int) Short.MIN_VALUE] = screlu((short)(i));
		}
	}
	
	private static int screlu(short i) {
		int v = Math.max(0, Math.min(i, QA));
		return v * v;
	}
	
	private final static int crelu[] = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];
	static {
		for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE;i ++) {
			crelu[i - (int) Short.MIN_VALUE] = crelu((short)(i));
		}
	}
	
	private static int crelu(short i) {
		return Math.max(0, Math.min(i, QA));
	}

	private static int getIndex(int square, int piece_side, int piece_type, int perspective) {
		return perspective == WHITE
				? piece_side * COLOR_STRIDE + piece_type * PIECE_STRIDE
						+ square
				: (piece_side ^ 1) * COLOR_STRIDE + piece_type * PIECE_STRIDE
						+ (square ^ 0b111000);
	}
	
	/* Used to manage updates of the accumulator buffers based on the feature array. */
	public class NNUEAccumulator
	{
		public short[] values = new short[NNUE.HIDDEN_SIZE];

		public NNUEAccumulator() {
			System.arraycopy(NNUE.L1Biases, 0, values, 0, NNUE.HIDDEN_SIZE);
		}

		public void reset() {
			System.arraycopy(NNUE.L1Biases, 0, values, 0, NNUE.HIDDEN_SIZE);
		}

		public void add(int featureIndex) {
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++) {
				values[i] += NNUE.L1Weights[featureIndex][i];
			}
		}
		
		public void subtract(int featureIndex) {
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++) {
				values[i] -= NNUE.L1Weights[featureIndex][i];
			}
		}
	}
	
	private final NNUEAccumulator whiteAccumulator;
	private final NNUEAccumulator blackAccumulator;
	
	public NNUE()
	{
		whiteAccumulator = new NNUEAccumulator();
		blackAccumulator = new NNUEAccumulator();
	}
	
	public void fullAccumulatorUpdate(int[] white_pieces, int[] white_squares, int[] black_pieces, int[] black_squares)
	{
		whiteAccumulator.reset();
		blackAccumulator.reset();
		
		for (int i = 0; i < white_pieces.length; i++) {
			if (white_pieces[i] == -1) {
				break;
			}
			whiteAccumulator.add(NNUE.getIndex(white_squares[i], NNUE.WHITE, white_pieces[i], NNUE.WHITE));
			blackAccumulator.add(NNUE.getIndex(white_squares[i], NNUE.WHITE, white_pieces[i], NNUE.BLACK));
		}
		
		for (int i = 0; i < black_pieces.length; i++) {
			if (black_pieces[i] == -1) {
				break;
			}
			whiteAccumulator.add(NNUE.getIndex(black_squares[i], NNUE.BLACK, black_pieces[i], NNUE.WHITE));
			blackAccumulator.add(NNUE.getIndex(black_squares[i], NNUE.BLACK, black_pieces[i], NNUE.BLACK));
		}
	}
	
	public void iterativeAccumulatorAddWhite(int white_piece, int white_square)
	{
		whiteAccumulator.add(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.WHITE));
		blackAccumulator.add(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.BLACK));
	}
	
	public void iterativeAccumulatorAddBlack(int black_piece, int black_square)
	{
		whiteAccumulator.add(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.WHITE));
		blackAccumulator.add(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.BLACK));
	}

	public void iterativeAccumulatorSubtractWhite(int white_piece, int white_square)
	{
		whiteAccumulator.subtract(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.WHITE));
		blackAccumulator.subtract(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.BLACK));
	}
	
	public void iterativeAccumulatorSubtractBlack(int black_piece, int black_square)
	{
		whiteAccumulator.subtract(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.WHITE));
		blackAccumulator.subtract(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.BLACK));
	}
	
	public int evaluate(PositionManager pm) {
		return pm.onMoveIsWhite() ?
		        evaluate(whiteAccumulator, blackAccumulator) :
		        evaluate(blackAccumulator, whiteAccumulator);
	}    
    
	public int evaluate(NNUEAccumulator us, NNUEAccumulator them) {
		short[] UsValues = us.values;
		short[] ThemValues = them.values;
		
		int eval = 0;
		for (int i = 0; i < HIDDEN_SIZE; i++) {
			eval += screlu[UsValues[i] - Short.MIN_VALUE] * NNUE.L2Weights[i]
					+ screlu[ThemValues[i] - Short.MIN_VALUE] * NNUE.L2Weights[i + HIDDEN_SIZE];
		}
				
		eval /= QA;
		eval += NNUE.outputBias;
		
		eval *= SCALE;
		eval /= QA * QB;
		
		return eval;
	}	
	
	/* Just for running incremental update assertions. */	
	public int old_evaluate(Board bd, Boolean isWhite) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (bd.getWhiteKing() == 0 || bd.getBlackKing() == 0) return 0;
			NetInput input = bd.populateNetInput();
			NNUEAccumulator old_whiteAccumulator = new NNUEAccumulator();
			NNUEAccumulator old_blackAccumulator = new NNUEAccumulator();
			for (int i = 0; i < input.white_pieces.length; i++) {
				if (input.white_pieces[i] == -1) {
					break;
				}
				old_whiteAccumulator.add(NNUE.getIndex(input.white_squares[i], NNUE.WHITE, input.white_pieces[i], NNUE.WHITE));
				old_blackAccumulator.add(NNUE.getIndex(input.white_squares[i], NNUE.WHITE, input.white_pieces[i], NNUE.BLACK));
			}
			
			for (int i = 0; i < input.black_pieces.length; i++) {
				if (input.black_pieces[i] == -1) {
					break;
				}
				old_whiteAccumulator.add(NNUE.getIndex(input.black_squares[i], NNUE.BLACK, input.black_pieces[i], NNUE.WHITE));
				old_blackAccumulator.add(NNUE.getIndex(input.black_squares[i], NNUE.BLACK, input.black_pieces[i], NNUE.BLACK));
			}
			return isWhite ?
			        evaluate(old_whiteAccumulator, old_blackAccumulator) :
			        evaluate(old_blackAccumulator, old_whiteAccumulator);
		}
		return 0;
	} 
		
	public int new_evaluate_for_assert(Board bd, Boolean isWhite) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (bd.getWhiteKing() == 0 || bd.getBlackKing() == 0) return 0;
			return isWhite ?
			        evaluate(whiteAccumulator, blackAccumulator) :
			        evaluate(blackAccumulator, whiteAccumulator);
		}
		return 0;
	}
}
