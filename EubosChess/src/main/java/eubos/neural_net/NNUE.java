package eubos.neural_net;


import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import eubos.board.Board;
import eubos.board.Board.NetInput;
import eubos.position.PositionManager;


/**
 * Experiment by probing via Bullet NNUE with 1 layers
 */
public class NNUE
{
	
	public static final boolean DO_INCREMENTAL_UPDATES = false;
	
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	
	private static final int COLOR_STRIDE = 64 * 6;
	private static final int PIECE_STRIDE = 64;

	private static final int HIDDEN_SIZE = 1024;
	private static final int FEATURE_SIZE = 768;
	private static final int OUTPUT_BUCKETS = 8;
	private static final int DIVISOR = (32 + OUTPUT_BUCKETS - 1) / OUTPUT_BUCKETS;
	private static final int INPUT_BUCKET_SIZE = 7;
	// @formatter:off
	private static final int[] INPUT_BUCKETS = new int[]
	{
			0, 0, 1, 1, 2, 2, 3, 3,
			4, 4, 4, 4, 5, 5, 5, 5,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
	};
	// @formatter:on

	private static final int SCALE = 400;
	private static final int QA = 255;
	private static final int QB = 64;

	private static short[][] L1Weights;
	private static short[] L1Biases;
	private static short[][] L2Weights;
	private static short outputBiases[];
	
	static {
		
		try {
			
			InputStream is = null;
			
			File file = new File("./network_bagatur.nnue");
			
			if (file.exists()) {
				
				is = new FileInputStream(file);
				
			} else {
				
				is = NNUE.class.getResourceAsStream("/network_bagatur.nnue");
			}
			
			DataInputStream networkData = new DataInputStream(
					new BufferedInputStream(
							is, 16 * 4096
					)
			);
			
			loadNetwork(networkData);
		
			networkData.close();
			
		} catch (IOException e) {
			
			throw new RuntimeException(e);
		}
	}
	
	
	private final static int screlu[] = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];
	
	static
	{
		for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE;i ++)
		{
			screlu[i - (int) Short.MIN_VALUE] = screlu((short)(i));
		}
	}
	
	private static int screlu(short i) {
		int v = Math.max(0, Math.min(i, QA));
		return v * v;
	}
	
	private Accumulators accumulators;
	private PositionManager pm;
	
	public NNUE(PositionManager eubos_pm) {
		
		pm = eubos_pm;
		
		accumulators = new Accumulators(this);
	}

	
	private static void loadNetwork(DataInputStream networkData) throws IOException {
		
		L1Weights = new short[FEATURE_SIZE * INPUT_BUCKET_SIZE][HIDDEN_SIZE];

		for (int i = 0; i < FEATURE_SIZE * INPUT_BUCKET_SIZE; i++)
		{
			for (int j = 0; j < HIDDEN_SIZE; j++)
			{
				L1Weights[i][j] = toLittleEndian(networkData.readShort());
			}
		}

		L1Biases = new short[HIDDEN_SIZE];

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			L1Biases[i] = toLittleEndian(networkData.readShort());
		}

		L2Weights = new short[OUTPUT_BUCKETS][HIDDEN_SIZE * 2];

		for (int i = 0; i < HIDDEN_SIZE * 2; i++)
		{
			for (int j = 0; j < OUTPUT_BUCKETS; j++)
			{
				L2Weights[j][i] = toLittleEndian(networkData.readShort());
			}
		}

		outputBiases = new short[OUTPUT_BUCKETS];

		for (int i = 0; i < OUTPUT_BUCKETS; i++)
		{
			outputBiases[i] = toLittleEndian(networkData.readShort());
		}
		
		networkData.close();
	}

	
	private static short toLittleEndian(short input) {
		return (short) (((input & 0xFF) << 8) | ((input & 0xFF00) >> 8));
	}
	
	
	public int evaluate() {
	
		Board bd = pm.getTheBoard();
		NetInput input = bd.populateNetInput();
			
		accumulators.fullAccumulatorUpdate(input.white_king_sq, input.black_king_sq, input.white_pieces, input.white_squares, input.black_pieces, input.black_squares);
		
		int pieces_count = bd.me.getNumPieces();
		
		int eval = pm.onMoveIsWhite() ?
		        evaluate(this, accumulators.getWhiteAccumulator(), accumulators.getBlackAccumulator(), pieces_count)
		        :
		        evaluate(this, accumulators.getBlackAccumulator(), accumulators.getWhiteAccumulator(), pieces_count);
		        
		return eval;
	}    
    
	public static int evaluate(NNUE network, NNUEAccumulator us, NNUEAccumulator them, int pieces_count) {
		
		short[] L2Weights = network.L2Weights[chooseOutputBucket(pieces_count)];
		short[] UsValues = us.values;
		short[] ThemValues = them.values;
		
		int eval = 0;
		
		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			eval += screlu[UsValues[i] - Short.MIN_VALUE] * L2Weights[i]
					+ screlu[ThemValues[i] - Short.MIN_VALUE] * L2Weights[i + HIDDEN_SIZE];
		}
		
		//int eval = JNIUtils.evaluateVectorized(L2Weights, UsValues, ThemValues);
		
		eval /= QA;
		eval += network.outputBiases[chooseOutputBucket(pieces_count)];
		
		eval *= SCALE;
		eval /= QA * QB;
		
		return eval;
	}
	
	public static int chooseOutputBucket(int pieces_count) {
		return (pieces_count - 2) / DIVISOR;
	}

	public static int chooseInputBucket(int king_sq, int side) {
		return side == WHITE ? INPUT_BUCKETS[king_sq]
				: INPUT_BUCKETS[king_sq ^ 0b111000];
	}

	public static int getIndex(int square, int piece_side, int piece_type, int perspective) {
		//System.out.println("square=" + square + ", piece_side=" + piece_side + ", piece_type=" + piece_type + ", perspective=" + perspective);
		return perspective == WHITE
				? piece_side * COLOR_STRIDE + piece_type * PIECE_STRIDE
						+ square
				: (piece_side ^ 1) * COLOR_STRIDE + piece_type * PIECE_STRIDE
						+ (square ^ 0b111000);
	}
	
	public static class NNUEAccumulator
	{
		private short[] values = new short[HIDDEN_SIZE];
		private int bucketIndex;
		NNUE network;

		public NNUEAccumulator(NNUE network, int bucketIndex) {
			this.network = network;
			this.bucketIndex = bucketIndex;
			System.arraycopy(network.L1Biases, 0, values, 0, HIDDEN_SIZE);
		}

		public void reset()
		{
			System.arraycopy(network.L1Biases, 0, values, 0, HIDDEN_SIZE);
		}

		public void setBucketIndex(int bucketIndex) {
			this.bucketIndex = bucketIndex;
		}

		public void add(int featureIndex) {
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndex + bucketIndex * FEATURE_SIZE][i];
			}
		}
		
		public void sub(int featureIndex) {
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] -= network.L1Weights[featureIndex + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addsub(int featureIndexToAdd, int featureIndexToSubtract) {
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE][i];
			}
		}
	}
}
