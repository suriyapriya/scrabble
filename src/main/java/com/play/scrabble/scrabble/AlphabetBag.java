package com.play.scrabble.scrabble;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

//Bag with all alphabet letter tiles from which random letters are taken
public class AlphabetBag
{
  public static String initialBagletters;
  private static int letterValues[];
  
  //To load the letter tile distribution to hashmap
  static {
      try 
      {
    	  initialBagletters = "";
    	  letterValues = new int[255];
    	  FileInputStream file = new FileInputStream("WEB-INF/dictionary/distribution.txt");
          Scanner input = new Scanner(file);
          while(input.hasNext())
          {
        	  String token[] = input.nextLine().toLowerCase().split(" ");
              for(int j = 0; j < Integer.parseInt(token[2]); j++) {
                  letterValues[token[0].charAt(0)] = Integer.parseInt(token[1]);
                  initialBagletters += token[0];
              }
          }
          input.close();
      }
      catch (FileNotFoundException e)
      {
          e.printStackTrace();
      }
  }
  
  public static int getLetterValue(char ch)
  {
	  return letterValues[ch];
  }
}
