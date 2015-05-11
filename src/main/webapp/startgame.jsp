<%@page import="com.play.scrabble.scrabble.ComputerPlayer"%>
<%@page import="com.play.scrabble.scrabble.Dictionary"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.googlecode.objectify.Key"%>
<%@page import="com.play.scrabble.db.Game"%>
<%@page import="com.googlecode.objectify.cmd.Query"%>
<%@page import="com.googlecode.objectify.Result"%>
<%@page import="com.googlecode.objectify.Objectify"%>
<%@page import="com.googlecode.objectify.ObjectifyService"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.appengine.api.users.User" %>

<%
    Objectify ofy = ObjectifyService.ofy();
    String userid = (String) session.getAttribute("userid");
    com.play.scrabble.db.User user = null;
    if (userid == null || (user = com.play.scrabble.db.User.load(userid)) == null) {
        session.invalidate();
        response.sendRedirect("/");
    }
%>

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>Scrabble</title>
  </head>
  <script type="text/javascript" src="js/jquery-2.1.4.min.js"></script>
  <script type="text/javascript">
  function play_game(gid)
  {
	  $("#playgameform input[name=gameid]").val(gid);
	  $("#playgameform").submit();
  }
  function submit_game_action(gid, act)
  {
      $("#gameactionform input[name=action]").val(act);
      $("#gameactionform input[name=gameid]").val(gid);
      $("#gameactionform").submit();
  }
  </script>

  <body>
  <form id="playgameform" action="playgame.jsp" method="post" ><input type="hidden" name="gameid"/></form>
  <form id="gameactionform" action="" method="post" ><input type="hidden" name="action"/><input type="hidden" name="gameid"/></form>
<h2>Hello <%= user.nickname %>, </h2>

<%
if ("creategame".equals(request.getParameter("action"))) { // Create game
	Game g = new Game();
	ofy.save().entity(g).now();
	String rc = Game.addPlayer(g.id, userid);
	user = com.play.scrabble.db.User.load(userid);
	if (rc!=null)
		out.print("<div style='background:red'>Error " + rc + "</div>");
	else
		out.print("<div style='background:green'>New game created</div>");
} else if (request.getParameter("join") != null) { // JOIN game
	String rc = Game.addPlayer(Long.parseLong(request.getParameter("join")), userid);
	if (rc!=null)
		out.print("<div style='background:red'>Error " + rc + "</div>");
	else
		out.print("<div style='background:green'>You have joined the game</div>");
} else if ("joincomputerplayer".equals(request.getParameter("action"))) { // JOIN computer player game
    String rc = ComputerPlayer.addPlayer(Long.parseLong(request.getParameter("gameid")));
    if (rc!=null)
        out.print("<div style='background:red'>Error " + rc + "</div>");
    else
        out.print("<div style='background:green'>" + ComputerPlayer.name +  " has joined the game</div>");
}
%>

<br/>
<table>
<%
if (user.games != null && !user.games.isEmpty()) {
	out.println("<td>ID</td><td>Players</td><td></td><td></td><td>URL for other players to join game</td>");
for(long gid : user.games) {
	Game g = Game.load(gid);
	if (g==null || g.players==null)
		continue;
	out.print("<tr><td>" + gid + "</td><td>");
	if (g.players.size() <= 1) {
		out.print("No players joined. </td><td>");
        out.print("</td><td><button onclick=\"submit_game_action('"+ gid +"', 'joincomputerplayer');\">Join a Computer player</button>");
	} else if (g.players.size() < 5) {
		out.print("You ");
		for(Key<com.play.scrabble.db.User> k : g.players) {
			com.play.scrabble.db.User u = ofy.load().key(k).get();
			if (u.id.equals(userid)) continue;
			out.print(" Vs. "+ u.nickname);
		}
		out.print("</td><td><button onclick=\"play_game('"+ gid +"');\">Play Game</button>");
        out.print("</td><td><button onclick=\"submit_game_action('"+ gid +"', 'joincomputerplayer');\">Join a Computer player</button>");
	} else {
		out.print("You Vs " + (g.players.size()-1) + " Players");
        out.print("</td><td><button onclick=\"play_game('"+ gid +"');\">Play Game</button>");
        out.print("</td><td><button onclick=\"submit_game_action('"+ gid +"', 'joincomputerplayer');\">Join a Computer player</button>");
	}
	String url = request.getRequestURL().toString() + "?join=" + gid;
	out.println("</td><td><a href=\"" + url + "\">" + url + "</a></td></tr>");
}
} else {
	out.println("You have joined no games.");
}
%>
</table>
<br/><br/>
<button onclick="submit_game_action('', 'creategame')">Create a game</button>
<br/>

<a href="logout.jsp">sign out</a>

  </body>
</html>
