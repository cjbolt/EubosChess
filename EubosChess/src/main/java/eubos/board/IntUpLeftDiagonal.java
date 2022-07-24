package eubos.board;

import eubos.position.Position;

public final class IntUpLeftDiagonal {

	  public static final int MASK = 0xF;
	  // Up left
	  public static final int Db1_a2 = 0;
	  public static final int Dc1_a3 = 1;
	  public static final int Dd1_a4 = 2;
	  public static final int De1_a5 = 3;
	  public static final int Df1_a6 = 4;
	  public static final int Dg1_a7 = 5;
	  public static final int Dh1_a8 = 6;
	  public static final int Dh2_b8 = 7;
	  public static final int Dh3_c8 = 8;
	  public static final int Dh4_d8 = 9;
	  public static final int Dh5_e8 = 10;
	  public static final int Dh6_f8 = 11;
	  public static final int Dh7_g8 = 12;
	  public static final int NODIAG = 13;

	  public static final int[] values = {
			  Db1_a2, Dc1_a3, Dd1_a4, De1_a5, Df1_a6, Dg1_a7,
			  Dh1_a8, 
			  Dh2_b8, Dh3_c8, Dh4_d8, Dh5_e8, Dh6_f8, Dh7_g8
	  };
	  
	  static final long[] upLeftDiagonals = new long[15];
	  static {
		  upLeftDiagonals[Db1_a2] = BitBoard.valueOf(new int[] {Position.b1, Position.a2});
		  upLeftDiagonals[Dc1_a3] = BitBoard.valueOf(new int[] {Position.c1, Position.b2, Position.a3});
		  upLeftDiagonals[Dd1_a4] = BitBoard.valueOf(new int[] {Position.d1, Position.c2, Position.b3, Position.a4});
		  upLeftDiagonals[De1_a5] = BitBoard.valueOf(new int[] {Position.e1, Position.d2, Position.c3, Position.b4, Position.a5});
		  upLeftDiagonals[Df1_a6] = BitBoard.valueOf(new int[] {Position.f1, Position.e2, Position.d3, Position.c4, Position.b5, Position.a6});
		  upLeftDiagonals[Dg1_a7] = BitBoard.valueOf(new int[] {Position.g1, Position.f2, Position.e3, Position.d4, Position.c5, Position.b6, Position.a7});
		  upLeftDiagonals[Dh1_a8] = BitBoard.valueOf(new int[] {Position.h1, Position.g2, Position.f3, Position.e4, Position.d5, Position.c6, Position.b7, Position.a8});
		  upLeftDiagonals[Dh2_b8] = BitBoard.valueOf(new int[]              {Position.h2, Position.g3, Position.f4, Position.e5, Position.d6, Position.c7, Position.b8});
		  upLeftDiagonals[Dh3_c8] = BitBoard.valueOf(new int[]                           {Position.h3, Position.g4, Position.f5, Position.e6, Position.d7, Position.c8});
		  upLeftDiagonals[Dh4_d8] = BitBoard.valueOf(new int[]                                        {Position.h4, Position.g5, Position.f6, Position.e7, Position.d8});
		  upLeftDiagonals[Dh5_e8] = BitBoard.valueOf(new int[]                                                     {Position.h5, Position.g6, Position.f7, Position.e8});
		  upLeftDiagonals[Dh6_f8] = BitBoard.valueOf(new int[]                                                                  {Position.h6, Position.g7, Position.f8});
		  upLeftDiagonals[Dh7_g8] = BitBoard.valueOf(new int[]                                                                               {Position.h7, Position.g8});
	  }
};
