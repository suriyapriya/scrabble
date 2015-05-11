package com.play.scrabble.scrabble;

public class Scrabble {
	public static final int MAXROW = 15;
	public static final int MAXCOL = 15;
	public static final int MAXRACK = 8;
	
	private static final String[] tileinfo = {
	    "T  d       d  T",
	    " D   t   t   D ",
	    "  D   d d   D  ",
	    "d  D   d   D  d",
	    "    D     D    ",
	    " t   t   t   t ",
	    "  d   d d   d  ",
	    "   d       d   ",
        "  d   d d   d  ",
        " t   t   t   t ",
        "    D     D    ",
        "d  D   d   D  d",
        "  D   d d   D  ",
        " D   t   t   D ",
        "T  d       d  T",
	};
	
	public static final int getTileLetterPointsMultiplier(int row, int col)
	{
	    switch(tileinfo[row].charAt(col)) {
	      case 't':
	          return 3;
	      case 'd':
	          return 2;
	    }
	    return 1;
	}
    public static final int getTileWordPointsMultiplier(int row, int col)
    {
        switch(tileinfo[row].charAt(col)) {
        case 'T':
            return 3;
        case 'D':
            return 2;
      }
        return 1;
    }
}
