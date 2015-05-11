package com.play.scrabble.scrabble;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Scanner;

//Dictionary
public class Dictionary
{
	private static final String path = "WEB-INF/dictionary/words.txt";
	private static Hashtable <String, Integer>dict = new Hashtable <String, Integer>();

    static {
		FileInputStream file;
	     String s;
	     try {
	    	 Integer x = new Integer(0);
	         file = new FileInputStream(path);
	         Scanner input = new Scanner(file);
	         while(input.hasNext()) {
	             s = input.nextLine();
	             dict.put(s.toLowerCase(), x);
	         }
	         input.close();
	     } catch (FileNotFoundException e) {
	         e.printStackTrace();
	         System.exit(1);
	     }        
    }
 
	//To check words
	public static boolean checkWord(String word)
	{
		return dict.containsKey(word.toLowerCase());
	}
}
