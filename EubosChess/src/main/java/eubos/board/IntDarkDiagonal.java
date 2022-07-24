package eubos.board;

import eubos.position.Position;

public final class IntDarkDiagonal {

	  public static final int MASK = 0xF;
	  // Up right
	  public static final int Da7_b8 = 0;
	  public static final int Da5_d8 = 1;
	  public static final int Da3_f8 = 2;
	  public static final int Da1_h8 = 3;
	  public static final int Dc1_h6 = 4;
	  public static final int De1_h4 = 5;
	  public static final int Dg1_h2 = 6;
	  // up left
	  public static final int Dc1_a3 = 7;
	  public static final int De1_a5 = 8;
	  public static final int Dg1_a7 = 9;
	  public static final int Dh2_b8 = 10;
	  public static final int Dh4_d8 = 11;
	  public static final int Dh6_f8 = 12;
	  public static final int NODIAG = 13;

	  public static final int[] values = {
			  Da7_b8, Da5_d8, Da3_f8, Da1_h8, Dc1_h6, De1_h4, Dg1_h2,
			  Dc1_a3, De1_a5, Dg1_a7, Dh2_b8, Dh4_d8, Dh6_f8
	  };
	  
	  static final long[] DarkDiagonalMasks = new long[15];
	  static {
		  DarkDiagonalMasks[Da7_b8] = BitBoard.valueOf(new int[] {Position.a7, Position.b8});
		  DarkDiagonalMasks[Da5_d8] = BitBoard.valueOf(new int[] {Position.a5, Position.b6, Position.c7, Position.d8});
		  DarkDiagonalMasks[Da3_f8] = BitBoard.valueOf(new int[] {Position.a3, Position.b4, Position.c5, Position.d6, Position.e7, Position.f8});
		  DarkDiagonalMasks[Da1_h8] = BitBoard.valueOf(new int[] {Position.a1, Position.b2, Position.c3, Position.d4, Position.e5, Position.f6, Position.g7, Position.h8});
		  DarkDiagonalMasks[Dc1_h6] = BitBoard.valueOf(new int[] {Position.c1, Position.d2, Position.e3, Position.f4, Position.g5, Position.h6});
		  DarkDiagonalMasks[De1_h4] = BitBoard.valueOf(new int[] {Position.e1, Position.f2, Position.g3, Position.h4});
		  DarkDiagonalMasks[Dg1_h2] = BitBoard.valueOf(new int[] {Position.g1, Position.h2});
		  
		  DarkDiagonalMasks[Dc1_a3] = BitBoard.valueOf(new int[] {Position.c1, Position.b2, Position.a3});
		  DarkDiagonalMasks[De1_a5] = BitBoard.valueOf(new int[] {Position.e1, Position.d2, Position.c3, Position.b4, Position.a5});
		  DarkDiagonalMasks[Dg1_a7] = BitBoard.valueOf(new int[] {Position.g1, Position.f2, Position.e3, Position.d4, Position.c5, Position.b6, Position.a7});
		  DarkDiagonalMasks[Dh2_b8] = BitBoard.valueOf(new int[] {Position.h2, Position.g3, Position.f4, Position.e5, Position.d6, Position.c7, Position.b8});
		  DarkDiagonalMasks[Dh4_d8] = BitBoard.valueOf(new int[] {Position.h4, Position.g5, Position.f6, Position.e7, Position.d8});
		  DarkDiagonalMasks[Dh6_f8] = BitBoard.valueOf(new int[] {Position.h6, Position.g7, Position.f8});
	  }
};
