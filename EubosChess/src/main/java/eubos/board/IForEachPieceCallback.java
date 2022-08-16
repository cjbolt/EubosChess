package eubos.board;

public interface IForEachPieceCallback {
	void callback(int piece, int atPos);
	boolean condition_callback(int piece, int atPos);
}
