package com.play.scrabble.scrabble;

//Tile class
class Tile
{
  enum Tiletype {CENTER, PLAIN, DL, TL, DW, TW};
  Tiletype type;
  String path = "tiles\\";
  String filenames[] = {"center.png", "plaintile.png", "dl.png",
                          "tl.png", "dw.png", "tw.png"};
  //Tile constructor
  public Tile(Tiletype tile)
  {
      type = tile;
  }
  
  //Get the points for the tiles
  int getWordPointsMultiplier()
  {
      if(type == Tiletype.DW)
          return 2;
      else if(type == Tiletype.TW)
          return 3;
      return 1;        
  }
  int getLetterPointsMultiplier()
  {
      if(type == Tiletype.DL)
          return 2;
      else if(type == Tiletype.TL)
          return 3;
      return 1;        
  }
}
