package com.play.scrabble.scrabble;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import com.play.scrabble.db.Game;
import com.play.scrabble.db.User;

class PlayMoves {
    public int row, col;
	public int ind;
	public String letter;
	public PlayMoves(int row, int col, int ind, char letter) {
		super();
		this.row = row;
		this.col = col;
		this.ind = ind;
		this.letter = new Character(letter).toString();
	}
    @Override
    public String toString() {
        return "PlayMoves [row=" + row + ", col=" + col + ", ind=" + ind
                + ", letter=" + letter + "]";
    }
};

@SuppressWarnings("serial")
public class ScrabbleServlet extends HttpServlet {
    private Game game;
    private User user;
    String userid;
    long gameid;
    String letterOnTiles[][];
    ArrayList<WordInTile> words;
    String strmessage; // any error conditions
    
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    userid = (String) req.getSession().getAttribute("userid");
		if (userid == null)
			return;
		try {
			gameid = Long.parseLong(req.getParameter("gameid"));
		} catch(Exception e) {
			return;
		}
		user = User.load(userid);
		game = Game.load(gameid);
		if (user==null || game==null)
			return;

        // start with clean slate
        letterOnTiles = new String[Scrabble.MAXROW][Scrabble.MAXCOL];
        words = new ArrayList<WordInTile>();
        strmessage = null;

		if (req.getParameter("initial") != null) {
            game.sendUpdateToClient(userid);
            sendUpdate(resp);
		    return;
		} else if (req.getParameter("computerplayerturn") != null) {
            if (User.isComputerPlayer(game.players.get(game.turn).getName()))
                new ComputerPlayer().playTurn(game);
            return;
		}
				
		String strplaymoves = req.getParameter("playmove");
		if (strplaymoves != null) {
		    System.out.println("strplaymoves "+ strplaymoves);
            if (game.turn != game.getUserInd(userid)) {
                strmessage = "Sorry! This is not your turn";
                sendUpdate(resp);
                return;
            }

            try {
    		    loadLetterOnTiles();
    			ArrayList<PlayMoves> movesarr = new ArrayList<PlayMoves>();
    			String playmoves[] = strplaymoves.split(",");
    			for(int i=0; i+4 <= playmoves.length; i+=4) {
    				movesarr.add(new PlayMoves(Integer.parseInt(playmoves[i]), Integer.parseInt(playmoves[i+1]),
    						Integer.parseInt(playmoves[i+3]), playmoves[i+2].charAt(0)));
    			}
    			if (movesarr.size() == 0) {
    			    // pass this turn
    			    game.addToMoveHistory(user.nickname + " passed on the turn.");
                    gotoNextTurn();
                    game.sendUpdateToAllClients();
    			    return;
    			}
                strmessage = checkRules(movesarr);
                if (strmessage != null) {
                    sendUpdate(resp);
                    return;
                }
                checkWords();
                collectWords(movesarr);
                String[] scoreupdate = updateScore();
                if (scoreupdate != null && scoreupdate.length == 2) {
                    strmessage = "Well done! You scored '" + scoreupdate[0] + "' points in this turn for the words - " + scoreupdate[1];
                    game.addToMoveHistory(user.nickname + " scored '" + scoreupdate[0] + "' points for the words - " + scoreupdate[1]);
                }
                gotoNextTurn();
                sendUpdate(resp);
                game.sendUpdateToAllClients();
		    } catch(Exception e) {
		        strmessage = "Exception occurred " + e;
		        e.printStackTrace();
		    }
		}
	}
	
    private void sendUpdate(HttpServletResponse resp) {
        resp.setContentType("application/json");
        JSONObject json = new JSONObject();
        try {
            if(strmessage != null)
                json.put("message", strmessage);
            if (User.isComputerPlayer(game.players.get(game.turn).getName()))
                json.put("computerplayerturn", 1);
            resp.getWriter().print(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String checkRules(ArrayList<PlayMoves> letterOnBoard) {
      boolean rowStatus = false;
      int row[] = new int[Scrabble.MAXRACK], col[] = new int[Scrabble.MAXRACK];
      String[][] array = new String[Scrabble.MAXROW][Scrabble.MAXCOL];
      String rack = game.getRack(userid);
      
      collectLetters(array);

      if (game.turn != game.getUserInd(userid))
          return "Sorry! This is not your turn";
      
      for(PlayMoves m : letterOnBoard) {
          if (array[m.row][m.col] != null)
              return "Invalid: Position "+m.row+" "+m.col+ " already filled.";
          if (rack.charAt(m.ind) != m.letter.charAt(0))
              return "Invalid: Letter at ind:"+m.ind+" in rack doesn't match. "+rack+" != "+m.letter;
      }
      
      // Check whether the word has more than one letter.
      if(letterOnBoard.size() == 1 && letterOnTiles[7][7] == null) {
          return "Word must be more than one letter. Please go again.";
      }
      
      // check whether the word occupy the center position
      if(letterOnTiles[7][7] == null) {
          boolean isCenterFill = false;
          for(PlayMoves m : letterOnBoard) {
              if(m.row == 7 && m.col == 7)
                  isCenterFill = true;
          }
          if(!isCenterFill) 
          {
              return "The game's first word always needs one letter on the center square. Give it another go.";
          }
      }
      for(int i = 1; i < letterOnBoard.size(); i++) {
          col[i-1] = letterOnBoard.get(i-1).col;
          if(letterOnBoard.get(i-1).row != letterOnBoard.get(i).row) {
              rowStatus = true;
              for(int j = 1; j < letterOnBoard.size(); j++) {
                  row[j-1] = letterOnBoard.get(j-1).row;
                  if(letterOnBoard.get(j-1).col != letterOnBoard.get(j).col) {
                      return "Some of the letters you placed aren't touching "
                              + "or aren't on the same line. Please go again";
                  }
              }
              break;
          }
      }

      //Check for size of letters and take action
      if(letterOnBoard.size() == 1) {
          row[0] = letterOnBoard.get(0).row;
          col[0] = letterOnBoard.get(0).col;
          sortValues(row, 1);
          collectLetters(array);
          array[row[0]][col[0]] = letterOnBoard.get(0).letter;

          if(!checkRowContinuity(array, row[0], row[0], col[0])) {
              getWordInRow(array, row[0], row[0], col[0]);
              getLinkedRowWords(array, row, col[0], 1);
          } else if(!checkColContinuity(array, col[0], col[0], row[0])) {
              getWordIncolumn(array, col[0], col[0], row[0]);
              getLinkedColWords(array, col, row[0], 1);
          } else {
              return "Some of the letters you placed aren't touching "
                      + "or aren't on the same line. Please go again";
          }
          boolean anyDictWord = false;
          for(WordInTile w : words) {
              if(Dictionary.checkWord(w.word))
                  anyDictWord = true;
          }
          if(!anyDictWord) {
              return "No dictionary words are formed in your placement. Please go again!";
          }
      } else if(rowStatus) {
          int j = letterOnBoard.size();
          row[j-1] = letterOnBoard.get(j-1).row;
          col[0] = letterOnBoard.get(0).col;
          sortValues(row, j);
          collectLetters(array);
          for(int k = 0; k < letterOnBoard.size(); k++)
          {
              for(int n = 0; n < letterOnBoard.size(); n++)
              {
                  if(letterOnBoard.get(n).row == row[k])
                      array[row[k]][col[0]] = letterOnBoard.get(n).letter;
              }
          }
          if(checkRowContinuity(array, row[0], row[j-1], col[0]))
          {
              return "Some of the letters you placed aren't touching "
                      + "or aren't on the same line. Please go again";
          } 
          String mainWord = getWordInRow(array, row[0], row[j-1], col[0]);
          if (!Dictionary.checkWord(mainWord)) {
              return "The main word '" + mainWord + "' is not a valid dictionary word.";
          }
          getLinkedRowWords(array, row, col[0], j);
      } else {
          int i = letterOnBoard.size();
          col[i-1] = letterOnBoard.get(i-1).col;
          row[0] = letterOnBoard.get(0).row;
          sortValues(col, i);
          collectLetters(array);
          for(int k = 0; k < letterOnBoard.size(); k++)
          {
              for(int n = 0; n < letterOnBoard.size(); n++)
              {
                  if(letterOnBoard.get(n).col == col[k])
                      array[row[0]][col[k]] = letterOnBoard.get(n).letter;
              }
          }
          if(checkColContinuity(array, col[0], col[i-1], row[0]))
          {
              return "Some of the letters you placed aren't touching "
                      + "or aren't on the same line. Please go again";
          }
          String mainWord = getWordIncolumn(array, col[0], col[i-1], row[0]);
          if (!Dictionary.checkWord(mainWord)) {
              return "The main word '" + mainWord + "' is not a valid dictionary word.";
          }
          getLinkedColWords(array, col, row[0], i);
      }

      return null;
    }
	
	private void loadLetterOnTiles() {
        for(int i=0; i<Scrabble.MAXROW; i++)
            for(int j=0; j<Scrabble.MAXCOL; j++)
                if(game.tile[i].charAt(j) == ' ')
                    letterOnTiles[i][j] = null;
                else
                    letterOnTiles[i][j] = new Character(game.tile[i].charAt(j)).toString();
	}

    private void collectLetters(String[][] oldtile) {
        for(int i=0; i<Scrabble.MAXROW; i++)
              for(int j=0; j<Scrabble.MAXCOL; j++)
                  oldtile[i][j] = letterOnTiles[i][j];
    }

    private void sortValues(int[] n, int count)
    {
      int temp = 0;
      for(int i = 0; i < count; i++) {
          for(int j = i; j < count; j++) {
              if(n[i] > n[j]) {
                  temp = n[i];
                  n[i] = n[j];
                  n[j] = temp;
              }                  
          }
      }
    }
    //To check row continuity
    boolean checkRowContinuity(String [][]array, int minRow, int maxRow, int col)
    {
        boolean isLetterOnBoard = false;
        for(int i = minRow; i <= maxRow; i++)
        {
            if(array[i][col] == null)
                return true;
            if(letterOnTiles[i][col] != null && array[i][col].equals(letterOnTiles[i][col]) && letterOnTiles[7][7] != null)
                isLetterOnBoard = true;
                
        }
        if(isLetterOnBoard || letterOnTiles[7][7] == null)
            return false;
        if((minRow<=0 || array[minRow-1][col] == null) && (maxRow+1>=Scrabble.MAXROW || array[maxRow+1][col] == null))
            return true;
        return false;
    }
    
    //To check column continuity 
    boolean checkColContinuity(String [][]array, int minCol, int maxCol, int row)
    {
        boolean isLetterOnBoard = false;
        for(int i = minCol; i <= maxCol; i++) {
            if(array[row][i] == null)
                return true;
            if(letterOnTiles[row][i] != null && array[row][i].equals(letterOnTiles[row][i]) && letterOnTiles[7][7] != null)
                isLetterOnBoard = true;
        }      
        if(isLetterOnBoard || letterOnTiles[7][7] == null)
            return false;
        if((minCol<=0 || array[row][minCol-1] == null) && (maxCol+1>=Scrabble.MAXCOL || array[row][maxCol+1] == null))
            return true;
        return false;
    }

    //To get the word in row-wise
    String getWordInRow(String [][]array, int minRow, int maxRow, int col)
    {
        int i, startRow;
        String s = "", s1 = "", s2 = "";
        for(i = minRow; i <= maxRow; i++) {
            if(array[i][col] != null)
            {
                s = s + array[i][col];
            }
        }
        for(i = minRow-1; i >= 0  && letterOnTiles[7][7] != null; i--) {
            if(array[i][col] == null)
            {
                break;
            }
            s1 = array[i][col] + s1;
        }
        startRow = i+1;
        for(int j = maxRow+1; j < Scrabble.MAXROW  && letterOnTiles[7][7] != null ; j++) {
            if(array[j][col] == null)
                break;
            s2 = s2 + array[j][col];
        }
        words.add(new WordInTile(startRow, col, s1 + s + s2, WordInTile.Direction.VERTICAL));
        return s1 + s + s2;
    }
    
    //To get the word in columnwise
    String getWordIncolumn(String [][]array, int minCol, int maxCol, int row)
    {
        int i, startCol;
        String s = "", s1 = "", s2 = "";
        for(i = minCol; i <= maxCol; i++) {
            if(array[row][i] != null)
            {
                s = s + array[row][i];
            }

        }
        for(i = minCol-1; i >= 0 && letterOnTiles[7][7] != null ; i--) {
            if(array[row][i] == null)
            {
                break;
            }
            s1 = array[row][i] + s1;
        }
        startCol = i+1;
        for(int j = maxCol + 1; j < Scrabble.MAXCOL && letterOnTiles[7][7] != null; j++) {
            if(array[row][j] == null)
                break;
            s2 = s2 + array[row][j];           
        }
        words.add(new WordInTile(row, startCol, s1 + s + s2, WordInTile.Direction.HORIZONTAL));
        return s1 + s + s2;
    }

    //To get the linked row words
    void getLinkedRowWords(String [][]array, int row[], int col, int count)
    {
        int n = 0;
        int colstart = col;
        for(int j = 0; j < count; j++) {
            String s = "";
            for(n = col-1; n >= 0 && row[j] >= 0; n--)
            {
                if(letterOnTiles[row[j]][n] != null)
                    s = array[row[j]][n] + s;
                else
                    break;
            }
            colstart = n + 1;
            if(letterOnTiles[7][7] != null && (col-1 >= 0 || col+1 < Scrabble.MAXCOL) && (letterOnTiles[row[j]][col-1] != null || letterOnTiles[row[j]][col+1] != null))
                s = s + array[row[j]][col];

            for(n = col+1; n < Scrabble.MAXCOL && row[j] < Scrabble.MAXROW; n++) {
                if(letterOnTiles[row[j]][n] != null)
                    s = s + array[row[j]][n];
                else
                    break;
            }
            if (!s.equals(""))
                words.add(new WordInTile(row[j], colstart, s, WordInTile.Direction.HORIZONTAL));
        }
    }
    
    //To get the linked column words
    void getLinkedColWords(String[][] array, int col[], int row, int count)
    {
        int n = 0;
        int rowstart = row;
        for(int j = 0; j < count; j++) {
            String s = "";
            for(n = row-1; n >= 0 && (col[j]) >= 0; n--) {
                if(letterOnTiles[n][col[j]] != null)
                    s = array[n][col[j]] + s;
                else
                    break;
            }
            rowstart = n + 1;

            if(letterOnTiles[7][7] != null && (row-1 >= 0 || row+1 < Scrabble.MAXROW) && (letterOnTiles[row-1][col[j]] != null || letterOnTiles[row+1][col[j]] != null))
                s = s + array[row][col[j]];

            for(n = row+1; n < Scrabble.MAXROW && (col[j]) < Scrabble.MAXROW; n++) {
                if(letterOnTiles[n][col[j]] != null)
                    s = s + array[n][col[j]];
                else
                    break;
            }
            if (!s.equals(""))
                words.add(new WordInTile(rowstart, col[j], s, WordInTile.Direction.VERTICAL));
        }
    }
    
    // To check whether the word is in dictionary
    public boolean checkWords()
    {
        if (words.size() < 1 || 
            !Dictionary.checkWord(words.get(0).word))
            return false;
        return true;
    }

    //To collect words
    void collectWords(ArrayList<PlayMoves> letterOnBoard)
    {
        String rack = game.getRack(userid);
        for(PlayMoves m : letterOnBoard) {
            // move from userrack to boardtile
            String s = game.tile[m.row];
            game.tile[m.row] = s.substring(0, m.col) + m.letter + s.substring(m.col+1);
            rack = rack.substring(0, m.ind) + game.getRandomLetter() + rack.substring(m.ind+1);
        }
        game.setRack(userid, rack);
    }
    
    //To update scores
    public String[] updateScore()
    {
        int score = 0;
        String allwords = "";
        for(WordInTile word : words) {
            score += word.getWordScore();
            if (allwords.length() > 0)
                allwords += ", ";
            allwords += word.word;
        }
        game.addScore(userid, score);
        String[] res = new String[2];
        res[0] = Integer.toString(score);
        res[1] = allwords;
        return res;
    }
    
    //To go to next turn
    void gotoNextTurn()
    {
        game.incTurn();
    }

}
