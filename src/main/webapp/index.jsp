<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.play.scrabble.user.MyUserService" %>
<%@ page import="com.play.scrabble.db.User" %>

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>Hello App Engine</title>
  </head>

  <body>
<%
    User user = MyUserService.getCurrentUser(session);
%>
<h2>Hello, <%= (user != null) ? user.nickname : "" %></h2>
<h3>Welcome to Online Scrabble</h3>
I am Suriya priya Veluchamy, currently an M.S student at Northwestern Polytechnic University, Fremont, CA.<br/>
This is my project using html5, javascript, jquery, ajax, kineticJS, DHTMLX Message for frontend and java servlets, jsp for backend.<br/>
The web application is hosted in google appengine.<br/>
<br/>
<br/>
<table>
<tr><td><a href="http://kineticjs.com/‎">KineticJS HTML5 canvas framework</a></td>
<td>Used for drawing the scrabble gameboard, and the drag-drop interactions of letters.</td></tr>
<tr><td>javascript, jquery, ajax</td><td>Used in various interactions of the game.</td></tr>
<tr><td><a href="http://dhtmlx.github.io/message/">DHTMLX Message</a></td>
<td>For displaying score updates to user.</td></tr>
</table>
<br/>
<br/>
Game is tested to work in Firefox, Chrome, IE9.
<br/>
<br/>
<%
if (user != null) {
%>
You can <a href="startgame.jsp">Start a game</a> OR
<a href="logout.jsp">sign out</a>
<%
    } else {
%>
You can <a href=<%= MyUserService.createGuestLoginURL() %>>Sign in as a Guest</a> OR
<a href="<%= MyUserService.createGoogleLoginURL() %>">Sign in using your Google account</a>
<%
    }
%>
  </body>
</html>
