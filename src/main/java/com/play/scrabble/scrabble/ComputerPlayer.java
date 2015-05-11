package com.play.scrabble.scrabble;

import java.util.ArrayList;

import com.play.scrabble.scrabble.WordInTile.Direction;
import com.play.scrabble.db.Game;
import com.play.scrabble.db.User;

public class ComputerPlayer {
    public static final String userid = "$$$charlie_the_computer$$$";
    private static final String email = "charlie_the_computer";
    public static final String name = "Charlie, The Computer";

    static {
        if (User.load(userid) == null)
            new User(userid, email, name).save();
    }

    public static String addPlayer(long gid)
    {
        return Game.addPlayer(gid, userid);
    }

    Game game;
    ArrayList<ArrayList<String>> permutelist = new ArrayList<ArrayList<String>>();
    WordInTile bestword;
    int bestscore = 0;
    private String bestpermute;
    
    public void playTurn(Game g) {
        game = g;
        
        if (!userid.equals(game.players.get(game.turn).getName()))
            return; // not our turn
        
        int wordlen, i, j, inboard, inrack, tmpscore;
        String wordrack = game.getRack(userid);
        WordInTile tmp;
        
        // load all permutations
        for (wordlen=0; wordlen < 6; wordlen++) {
            permutelist.add(getPermutationList(wordrack, wordlen));
            System.out.println("wordlen "+ wordlen + " " + permutelist.get(wordlen).size());
        }
        
        for (wordlen=5; wordlen > 2; wordlen--) {
            for (i=0; i<Scrabble.MAXROW; i++) {
                for (j=0; j<Scrabble.MAXCOL; j++) {
                    for (WordInTile.Direction dir : WordInTile.Direction.values()) {
                        inboard = getNoOfLettersInBoard(i, j, dir, wordlen);
                        if (game.letterAt(7, 7) != ' ') {
                            if (inboard <= 0 || inboard >= wordlen)
                                continue;
                        } else if (inboard != 0 || i != 7 || j != 7)
                            // first time play - lets start at center
                            continue;
                        inrack = wordlen - inboard;
                        System.out.printf("At %d,%d wordlen %d inboard %d inrack %d\n", i, j, wordlen, inboard, inrack);
                        for (String permute : permutelist.get(inrack)) {
                            String w = getWordUsingPermute(i, j, dir, wordlen, permute);
                            if (!Dictionary.checkWord(w))
                                continue;
                            System.out.println(w);
                            tmp = new WordInTile(i, j, w, dir);
                            tmpscore = tmp.getWordScore();
                            if (tmpscore > bestscore) {
                                bestscore = tmpscore;
                                bestword = tmp;
                                bestpermute = permute;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("bestscore " + bestscore + " " + bestword);
        updateBoardAndRack();
        updateScore();
        game.incTurn();
        game.sendUpdateToAllClients();
    }
    private void updateScore() {
        if (bestscore <= 0) {
            bestscore = 0;
            game.addToMoveHistory(name + " passed his turn!");
        } else
            game.addToMoveHistory(name + " scored '" + bestscore + "' points for the word - " + bestword.word);
        game.addScore(userid, bestscore);
    }
    private void updateBoardAndRack() {
        if (bestscore <= 0)
            return;
        String rack = game.getRack(userid);
        for(char ch : bestpermute.toCharArray()) {
            int ind = rack.indexOf(ch);
            assert(ind != -1);
            rack = rack.substring(0, ind) + rack.substring(ind+1) + game.getRandomLetter();
        }
        for(int i=0; i< bestword.word.length(); i++) {
            int r, c;
            if (bestword.dir == WordInTile.Direction.HORIZONTAL) {
                r = bestword.row;
                c = bestword.col + i;
            } else {
                r = bestword.row + i;
                c = bestword.col;
            }
            String s = game.tile[r];
            game.tile[r] = s.substring(0, c) + bestword.word.charAt(i) + s.substring(c+1);
        }
        game.setRack(userid, rack);
    }
    private String getWordUsingPermute(int row, int col, Direction dir,
                                       int wordlen, String permute)
    {
        String w = "";
        for(int i=0, j=0; i<wordlen; i++) {
            char ch = ' ';
            if (dir == WordInTile.Direction.VERTICAL)
                ch = game.letterAt(row+i, col);
            else
                ch = game.letterAt(row, col+i);
            if (ch == ' ')
                w += permute.charAt(j++);
            else
                w += ch;
        }
        return w;
    }
    private int getNoOfLettersInBoard(int row, int col, WordInTile.Direction dir, int wordlen)
    {
        int inboard = 0;
        if (dir == WordInTile.Direction.VERTICAL) // word starts at i,j and goes from left to right
            if (row-1 < 0 || game.letterAt(row-1, col) != ' ' || row+wordlen>=Scrabble.MAXROW || game.letterAt(row+wordlen, col) != ' ')
            return -1;
        if (dir == WordInTile.Direction.HORIZONTAL) // word starts at i,j and goes top to bottom
            if (col-1 < 0 || game.letterAt(row, col-1) != ' ' || col+wordlen>=Scrabble.MAXCOL || game.letterAt(row, col+wordlen) != ' ')
            return -1;
        for(int i=0; i<wordlen; i++) {
            if (dir == WordInTile.Direction.VERTICAL) {
                if (game.letterAt(row+i, col) != ' ')
                    inboard++;
            } else {
                if (game.letterAt(row, col+i) != ' ')
                    inboard++;
            }
        }
        return inboard;
    }

    // http://javahungry.blogspot.com/2013/06/find-all-possible-permutations-of-given.html
    private ArrayList<String> getPermutationList(String input, int len)
    {
        ArrayList<String> list = new ArrayList<String>();
        getPermutationList(input.toCharArray(), new StringBuffer(), new boolean[input.length()], len, list);
        return list;
    }
    
    private void getPermutationList(char[] in, StringBuffer outputString, boolean[] used,
                                    int neededLen, ArrayList<String> list)
    {
        if(outputString.length() == neededLen) {
            list.add(outputString.toString());
            return;
        }
        
        for( int i = 0; i < in.length; ++i )
        {
            if(used[i]) continue;
            
            outputString.append(in[i]);
            used[i] = true;
            getPermutationList(in, outputString, used, neededLen, list);
            used[i] = false;
            outputString.setLength(   outputString.length() - 1 );
        }
    }
}
