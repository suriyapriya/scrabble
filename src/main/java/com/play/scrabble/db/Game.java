package com.play.scrabble.db;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.play.scrabble.scrabble.AlphabetBag;
import com.play.scrabble.scrabble.Scrabble;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.ObjectifyService;

@Entity
public class Game {
  private static final int MOVE_HISTORY_SIZE = 20;
  
  @Id
  public Long id;
  public String[] tile;
  private String alphabetbag;	// letter tiles remaining in the bag
  public String[] rack;			// letter rack for players
  public int[] score;			// score for players
  public List<Key<User>> players;
  public int turn;
  
  public List<String> movehistory;

  public Game()
  {
	  this.id = null;
	  this.players = new ArrayList<Key<User>>();
	  this.rack = new String[Scrabble.MAXROW];
	  this.score = new int[Scrabble.MAXROW];
	  this.alphabetbag = AlphabetBag.initialBagletters;
	  for (int i=0; i<Scrabble.MAXROW; i++) {
		  rack[i] = "";
		  score[i] = 0;
	  }
	  tile = new String[Scrabble.MAXROW];
	  for (int i=0; i<Scrabble.MAXROW; i++) {
		  tile[i] = new String();
		  for (int j=0; j<Scrabble.MAXCOL; j++)
			  tile[i] += ' ';
	  }
	  this.turn = 0;
	  
	  movehistory = new ArrayList<String>();
  }
  
  public static Game load(long gid)
  {
	  return ObjectifyService.ofy().load().type(Game.class).id(gid).get();
  }
  public void save()
  {
      ObjectifyService.ofy().save().entity(this).now();
  }
  public static String addPlayer(long gid, String uid)
  {
    String rc = null;
	Game g = Game.load(gid);
	User u = User.load(uid);
	if (g==null)
		return "Invalid game id";
	if (u==null)
		return "Invalid user id";
	if (g.players==null)
		g.players = new ArrayList<Key<User>>();
	if (u.games == null)
		u.games = new ArrayList<Long>();
	Key<User> k = Key.create(User.class, uid);
	if (!g.players.contains(k))
		g.players.add(k);
	else
		rc = "You have already joined the game - error 1";
	if (!u.games.contains(gid))
		u.games.add(gid);
	else
		rc = "You have already joined the game - error 2";
	g.save();
	u.save();
	
	if (rc == null) {
		// get letters from bag and populate the rack
		int ind = g.players.size() - 1;
		g.rack[ind] = new String();
	    for (int j=0; j<Scrabble.MAXRACK; j++)
	    	g.rack[ind] += g.getRandomLetter();
	    g.save();
	}

	return rc;
  }
  
  public String getRandomLetter()
  {
      Random rand = new Random();
      int n = rand.nextInt(alphabetbag.length());
      String newletter = String.valueOf(alphabetbag.charAt(n));
      alphabetbag = alphabetbag.substring(0, n) + alphabetbag.substring(n+1);
      save();
      return newletter;
  }

  
  public int getUserInd(String uid)
  {
	  Key<User> k = Key.create(User.class, uid);
	  return players.indexOf(k);
  }
  public int getScore(String uid)
  {
	  int ind = getUserInd(uid);
	  if(ind == -1)
		  return 0;
	  return score[ind];
  }
  
  public int addScore(String uid, int addscore)
  {
	  int ind = getUserInd(uid);
	  if(ind == -1)
		  return 0;
	  score[ind] += addscore;
      save();
	  return score[ind];
  }
  
  public String getRack(String uid)
  {
	  int ind = getUserInd(uid);
	  if(ind == -1)
		  return "";
	  return rack[ind];
  }

  public String setRack(String uid, String newrack)
  {
	  int ind = getUserInd(uid);
	  if(ind == -1)
		  return "";
	  rack[ind] = newrack;
      save();
	  return rack[ind];
  }
  public void incTurn() {
	  this.turn = (this.turn+1) % players.size();
	  save();
  }
  
  public void addToMoveHistory(String str)
  {
      movehistory.add(0, str);
      if (movehistory.size() > MOVE_HISTORY_SIZE) {
          movehistory.remove(movehistory.size()-1);
      }
      save();
  }
  public ArrayList<String> getMoveHistory()
  {
      ArrayList<String> res = new ArrayList<String>();
      for(int i=0; i<movehistory.size(); i++) {
          res.add(movehistory.get(i));
      }
      return res;
  }
  
  public char letterAt(int row, int col)
  {
      if (row<0 || col<0 || row>=Scrabble.MAXROW || col>=Scrabble.MAXCOL)
          return ' ';
      return tile[row].charAt(col);
  }
  public void sendUpdateToClient(String uid)
  {
      ChannelServiceFactory.getChannelService().sendMessage(new ChannelMessage(uid, getJSONGameState(uid)));
  }
  public void sendUpdateToAllClients()
  {
      for(Key<User> k : players) {
          sendUpdateToClient(k.getName());
      }
  }

  private String getJSONGameState(String userid)
  {
      JSONObject json = new JSONObject();
      JSONArray jrack = new JSONArray();
      JSONArray jfixed = new JSONArray();
      try {
          for (int i=0; i<this.tile.length; i++)
              for (int j=0; j<this.tile[i].length(); j++) {
                  char ch = this.tile[i]. charAt(j);
                  if (ch ==' ') continue;
                  jfixed.put(String.valueOf(ch));
                  jfixed.put(i);
                  jfixed.put(j);
              }
          for (char c : getRack(userid).toLowerCase().toCharArray())
              jrack.put(new Character(c));
          json.put("fixed", jfixed);
          json.put("letterrack", jrack);
          json.put("myturn", getUserInd(userid)==turn);
          json.put("turnid", turn);
          if (getUserInd(userid) != turn) {
              String t = players.get(turn).getName();
              json.put("otherturn", User.load(t).nickname + "'s turn");
          }
          json.put("scoreboard", getHTMLScoreBoard(userid));
          return json.toString();
      } catch (JSONException e) {
          e.printStackTrace();
      }
      return null;
  }

  private String getHTMLScoreBoard(String userid) {
      String str = "<tr><td>You</td><td>" + Integer.toString(getScore(userid)) +  "</td></tr>";
      for(int i=0 ; i<players.size(); i++) {
          Key<User> k = players.get(i);
          User u = ObjectifyService.ofy().load().key(k).get();
          if (u.id.equals(userid)) continue;
          str += "<tr><td> " + u.nickname + " </td><td>" + score[i] + "</td></tr>";
      }
      return "<table><h4>Scores</h4>" + str + "</table>";
  }
}
