package eubos.evaluation;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import eubos.board.Board;
import eubos.board.Board.NetInput;
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
	
	public static Accumulators accumulators;
	static {
		accumulators = new Accumulators();
	}
	
	public static Accumulators old_accumulators;
	static {
		old_accumulators = new Accumulators();
	}

	public static int old_evaluate(Board bd, Boolean isWhite) {
		NetInput input = bd.populateNetInput();
		NNUE.old_accumulators.fullAccumulatorUpdate(input.white_pieces, input.white_squares, input.black_pieces, input.black_squares);
		return isWhite ?
		        evaluate(old_accumulators.getWhiteAccumulator(), old_accumulators.getBlackAccumulator()) :
		        evaluate(old_accumulators.getBlackAccumulator(), old_accumulators.getWhiteAccumulator());
	} 
	
	public static int new_evaluate_for_assert(Boolean isWhite) {
		return isWhite ?
		        evaluate(accumulators.getWhiteAccumulator(), accumulators.getBlackAccumulator()) :
		        evaluate(accumulators.getBlackAccumulator(), accumulators.getWhiteAccumulator());
	}  
	
	public static int evaluate(PositionManager pm) {
		return pm.onMoveIsWhite() ?
		        evaluate(accumulators.getWhiteAccumulator(), accumulators.getBlackAccumulator()) :
		        evaluate(accumulators.getBlackAccumulator(), accumulators.getWhiteAccumulator());
	}    
    
	public static int evaluate(NNUEAccumulator us, NNUEAccumulator them) {
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

	public static int getIndex(int square, int piece_side, int piece_type, int perspective) {
		return perspective == WHITE
				? piece_side * COLOR_STRIDE + piece_type * PIECE_STRIDE
						+ square
				: (piece_side ^ 1) * COLOR_STRIDE + piece_type * PIECE_STRIDE
						+ (square ^ 0b111000);
	}
	
	public static class NNUEAccumulator
	{
		private short[] values = new short[HIDDEN_SIZE];

		public NNUEAccumulator() {
			System.arraycopy(NNUE.L1Biases, 0, values, 0, HIDDEN_SIZE);
		}

		public void reset() {
			System.arraycopy(NNUE.L1Biases, 0, values, 0, HIDDEN_SIZE);
		}

		public void add(int featureIndex) {
			for (int i = 0; i < HIDDEN_SIZE; i++) {
				values[i] += NNUE.L1Weights[featureIndex][i];
			}
		}
		
		public void subtract(int featureIndex) {
			for (int i = 0; i < HIDDEN_SIZE; i++) {
				values[i] -= NNUE.L1Weights[featureIndex][i];
			}
		}
	}
}
