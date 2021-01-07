package eubos.score;

public class PiecewiseEvaluation {
	short black = 0;
	short white = 0;
	short position = 0;
	
	public PiecewiseEvaluation() {
	}
	
	public PiecewiseEvaluation(short white, short black, short position) {
		this.black = black;
		this.white = white;
		this.position = position;
	}
	
	public short getBlack() {return black;}
	public short getWhite() {return white;}
	
	public void addBlack(short toAdd) { black += toAdd; }
	public void addWhite(short toAdd) { white += toAdd; }
	
	public short getDelta() { return (short)(white-black); }

	public void addPositionWhite(short pstBoost) { position += pstBoost; }
	public void addPositionBlack(short pstBoost) { position -= pstBoost; }
	
	public short getPosition() { return position; }
}
