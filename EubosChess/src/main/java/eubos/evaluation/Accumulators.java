package eubos.evaluation;

public class Accumulators
{
	private final NNUE.NNUEAccumulator whiteAccumulator;
	private final NNUE.NNUEAccumulator blackAccumulator;
	
	public Accumulators()
	{
		whiteAccumulator = new NNUE.NNUEAccumulator();
		blackAccumulator = new NNUE.NNUEAccumulator();
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
	
	public void iterativeAccumulatorAdd(int white_piece, int white_square, int black_piece, int black_square)
	{
		if (white_piece != -1) {
			whiteAccumulator.add(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.WHITE));
			blackAccumulator.add(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.BLACK));
		}
		if (black_piece != -1) {
			whiteAccumulator.add(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.WHITE));
			blackAccumulator.add(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.BLACK));
		}
	}
	
	public void iterativeAccumulatorSubtract(int white_piece, int white_square, int black_piece, int black_square)
	{
		if (white_piece != -1) {
			whiteAccumulator.subtract(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.WHITE));
			blackAccumulator.subtract(NNUE.getIndex(white_square, NNUE.WHITE, white_piece, NNUE.BLACK));
		}
		if (black_piece != -1) {
			whiteAccumulator.subtract(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.WHITE));
			blackAccumulator.subtract(NNUE.getIndex(black_square, NNUE.BLACK, black_piece, NNUE.BLACK));
		}
	}
	
	public NNUE.NNUEAccumulator getWhiteAccumulator()
	{
		return whiteAccumulator;
	}
	
	public NNUE.NNUEAccumulator getBlackAccumulator()
	{
		return blackAccumulator;
	}
}
