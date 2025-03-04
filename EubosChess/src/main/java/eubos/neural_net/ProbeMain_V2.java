package eubos.neural_net;

import java.io.IOException;

import eubos.position.PositionManager;

public class ProbeMain_V2 {    
		
	public static void main(String[] args) {
		
		String test_evalPosA = "rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12"; 
		String test_EvalPosB = "8/8/1B6/8/8/4Kpk1/8/b7 w - - - 85";
		String fen2 = "k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1";
		String fen3 = "4kq2/8/8/8/8/8/8/3K4 w - - 0 1";
		String fen4 = "4k3/8/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		String fen5 = "rnbqkbnr/pppppppp/8/8/8/8/8/4K3 w KQkq - 0 1";
		String fen6 = "4kr2/8/8/8/8/8/8/2RK4 w - - 0 1";
		String fen7 = "4k3/8/8/8/8/8/8/2RK4 w - - 0 1";
		String fen8 = "4kr2/8/8/8/8/8/8/3K4 w - - 0 1";
        
		try {
			
			evaluate(test_evalPosA);
	        evaluate(test_EvalPosB);
	        evaluate(fen2);
	        evaluate(fen3);
	        evaluate(fen4);
	        evaluate(fen5);
	        evaluate(fen6);
	        evaluate(fen7);
	        evaluate(fen8);
	        
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	private static void evaluate(String fen) throws IOException {
		PositionManager pm = new PositionManager(fen);
		NNUE network = new NNUE(pm);
		
		int eval = network.evaluate();
		
		System.out.println("fen=" + fen + ", eval=" + eval);
	}
}
