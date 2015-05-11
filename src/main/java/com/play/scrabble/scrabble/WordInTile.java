package com.play.scrabble.scrabble;

//To maintain words and their tile positions
class WordInTile
{
 int row;
 int col;
 String word;
 enum Direction { HORIZONTAL, VERTICAL};
 Direction dir;
 
 public WordInTile(int r, int c, String w, Direction dir)
 {
     row = r;
     col = c;
     word = w;
     this.dir = dir;
 }

 public int getWordScore()
 {
     int wordscore = 0;
     int wordmultiplier = 1;
     int r, c;
     if(!Dictionary.checkWord(word))
         return 0;
     for(int j = 0; j < word.length(); j++) {
         if (dir == WordInTile.Direction.HORIZONTAL) {
             r = row;
             c = col + j;
         } else {
             r = row + j;
             c = col;
         }
         wordscore += AlphabetBag.getLetterValue(word.charAt(j)) * Scrabble.getTileLetterPointsMultiplier(r, c);
         wordmultiplier *= Scrabble.getTileWordPointsMultiplier(r, c);
     }
     return wordmultiplier * wordscore;
 }
 @Override
 public String toString() {
     return "WordInTile [row=" + row + ", col=" + col + ", word=" + word
             + ", dir=" + dir + "]";
 }
}
