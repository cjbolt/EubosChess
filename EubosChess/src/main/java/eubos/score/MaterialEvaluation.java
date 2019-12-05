package eubos.score;

public class MaterialEvaluation {
	short black = 0;
	short white = 0;
	
	public MaterialEvaluation() {
	}
	
	public short getBlack() {return black;}
	public short getWhite() {return white;}
	
	public void addBlack(int toAdd) { black += toAdd; }
	public void addWhite(int toAdd) { white += toAdd; }
	
	public short getDelta() { return (short)(white-black); }
}
