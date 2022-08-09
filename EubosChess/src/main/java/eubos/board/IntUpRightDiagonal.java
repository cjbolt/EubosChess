package eubos.board;

import eubos.position.Position;

public final class IntUpRightDiagonal {

	  public static final int MASK = 0xF;
	  // Up right
	  public static final int Da7_b8 = 0;
	  public static final int Da6_c8 = 1;
	  public static final int Da5_d8 = 2;
	  public static final int Da4_e8 = 3;
	  public static final int Da3_f8 = 4;
	  public static final int Da2_g8 = 5;
	  public static final int Da1_h8 = 6;
	  public static final int Db1_h7 = 7;
	  public static final int Dc1_h6 = 8;
	  public static final int Dd1_h5 = 9;
	  public static final int De1_h4 = 10;
	  public static final int Df1_h3 = 11;
	  public static final int Dg1_h2 = 12;
	  public static final int NODIAG = 13;

	  public static final int[] values = {
			  Da7_b8, Da6_c8, Da5_d8, Da4_e8, Da3_f8, Da2_g8,
			  Da1_h8, 
			  Db1_h7, Dc1_h6, Dd1_h5, De1_h4, Df1_h3, Dg1_h2
	  };
	  
	  static final long[] upRightDiagonals = new long[15];
	  static {
		  upRightDiagonals[Da7_b8] = BitBoard.valueOf(new int[] 																			  {Position.a7, Position.b8});
		  upRightDiagonals[Da6_c8] = BitBoard.valueOf(new int[] 																 {Position.a6, Position.b7, Position.c8});
		  upRightDiagonals[Da5_d8] = BitBoard.valueOf(new int[] 													{Position.a5, Position.b6, Position.c7, Position.d8});
		  upRightDiagonals[Da4_e8] = BitBoard.valueOf(new int[] 									   {Position.a4, Position.b5, Position.c6, Position.d7, Position.e8});
		  upRightDiagonals[Da3_f8] = BitBoard.valueOf(new int[]                           {Position.a3, Position.b4, Position.c5, Position.d6, Position.e7, Position.f8});
		  upRightDiagonals[Da2_g8] = BitBoard.valueOf(new int[]              {Position.a2, Position.b3, Position.c4, Position.d5, Position.e6, Position.f7, Position.g8});
		  upRightDiagonals[Da1_h8] = BitBoard.valueOf(new int[] {Position.a1, Position.b2, Position.c3, Position.d4, Position.e5, Position.f6, Position.g7, Position.h8});
		  upRightDiagonals[Db1_h7] = BitBoard.valueOf(new int[] {Position.b1, Position.c2, Position.d3, Position.e4, Position.f5, Position.g6, Position.h7});
		  upRightDiagonals[Dc1_h6] = BitBoard.valueOf(new int[] {Position.c1, Position.d2, Position.e3, Position.f4, Position.g5, Position.h6});
		  upRightDiagonals[Dd1_h5] = BitBoard.valueOf(new int[] {Position.d1, Position.e2, Position.f3, Position.g4, Position.h5});
		  upRightDiagonals[De1_h4] = BitBoard.valueOf(new int[] {Position.e1, Position.f2, Position.g3, Position.h4});
		  upRightDiagonals[Df1_h3] = BitBoard.valueOf(new int[] {Position.f1, Position.g2, Position.h3});
		  upRightDiagonals[Dg1_h2] = BitBoard.valueOf(new int[] {Position.g1, Position.h2});
	  }
};
