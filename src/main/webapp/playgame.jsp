<%@page import="com.google.appengine.api.channel.ChannelServiceFactory"%>
<%@page import="com.googlecode.objectify.Objectify"%>
<%@ page import="com.googlecode.objectify.ObjectifyService" %>
<%@page import="com.googlecode.objectify.Key"%>
<%@page import="com.play.scrabble.db.Game"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.appengine.api.users.User" %>

<%
Objectify ofy = ObjectifyService.ofy();
String userid = (String) session.getAttribute("userid");
long gameid;
if (userid == null)
	response.sendRedirect("/");
try {
	gameid = Long.parseLong(request.getParameter("gameid"));
} catch(Exception e) {
	response.sendRedirect("/");
	return;
}
com.play.scrabble.db.User user = com.play.scrabble.db.User.load(userid);
Game game = Game.load(gameid);
if (user==null || game==null)
	response.sendRedirect("/startgame.jsp");
%>
<!DOCTYPE HTML>
<html>
<head>
<script type="text/javascript" src="/_ah/channel/jsapi"></script>
<script type="text/javascript" src="js/jquery-2.1.4.min.js"></script>
<script type="text/javascript" src='js/message.js'></script>
<script src="js/kinetic-v5.0.2.min.js"></script>
<link rel="stylesheet" type="text/css" href="js/message_default.css">
<style type="text/css">
	body, html {
		height:100%;
	}
</style>
</head>
<body>
<div style="display:none;">
<img id="img_tile_plain" src="tiles/plain.png">
<img id="img_tile_tw" src="tiles/tw.png">
<img id="img_tile_tl" src="tiles/tl.png">
<img id="img_tile_dw" src="tiles/dw.png">
<img id="img_tile_dl" src="tiles/dl.png">
<img id="img_tile_center" src="tiles/center.png">
<%
for(char ch='a'; ch <= 'z'; ch++)
	out.println("<img id=\"img_alpha_"+ ch +"\" src=\"alphabets/"+ Character.toUpperCase(ch) +".png\">");
%>
</div>
    <table><tr><td><div id="container"></div></td>
    <td style="vertical-align:top;"><div id="yourturn"><h4>Your turn</h4><br/><button onclick="play_turn();">Play Turn</button></div>
    <h4><div id="otherturn"></div><br/></h4>
    <div id="scoreboard"></div><br/>
    <h4>History of moves</h4><br/>
    <div id="scorehistory"></div>
    </td>
    </tr></table>
    <script defer="defer">
      var channel;
      var socket;
      
      var MAXROW = 15;
      var MAXCOL = 15;
      var MAXRACK = 8;
      var myturn = false;
      var turnid = 0;

      var rack = Array(MAXRACK);
      var tiles = Array(MAXROW);
      var usertiles = Array();
      for (var i = 0; i < MAXROW; i++) {
    	  tiles[i] = new Array(MAXCOL);
      }

      function Tile(name, i, j, img) {
    	  this.type = 'tile';
    	  this.name = name + ' ' + i + ' ' + j;
    	  this.i = i;
    	  this.j = j;
    	  this.letter = null;
    	  this.img = img;
      }
      function Rack(name, i, img) {
    	  this.type = 'rack';
    	  this.name = name + ' ' + i;
    	  this.i = i;
    	  this.letter = null;
    	  this.img = img;
      }
      function Letter(name, img, pos) {
    	  this.name = name;
    	  this.img = img;
    	  this.pos = pos;
      }
      function writeMessage(message) {
    	  // for debugging
          //text.setText(message);
          //layer.draw();
      }
      
      var stage = new Kinetic.Stage({
        container: 'container',
        width: 600,
        height: 700
      });
      var boardbase = new Kinetic.Layer();
      var layer = new Kinetic.Layer();
      var tilerack = new Kinetic.Layer();
      
      function load_initial_board()
      {
	      for(var i = 0; i < MAXROW; i++) {
	          for(var j = 0; j < MAXCOL; j++) {
	        	  var imgname = 'img_tile_plain';
	              if((j == 0 || j == (MAXROW/2) || j == MAXROW-1)
	                      &&  (i == 0|| i == (MAXCOL/2) || i == MAXCOL-1)
	                      && (j != (MAXROW/2) || i != (MAXCOL/2)))
	              {
	                  imgname = 'img_tile_tw';
	              }
	              else if((j==1 && (i == 13 || i == 1)) || (j==2 && (i == 12 || i==2))
	                      || (j==3 && (i==11 || i==3)) || (j==4 && (i==10 || i==4))
	                      || (j==10 && (i==4 || i==10)) || (j==11 && (i==3 || i==11))
	                      || (j==12 && (i==2 || i==12)) || (j==13 && (i==1 || i==13)))
	              {
	                  imgname = 'img_tile_dw';
	              }
	              else if(((j==1 || j==13) && (i==5 || i==9))
	                      || ((j==5 || j==9) && (i==1 || i==5 || i==9 || i==13)))
	              {
	                  imgname = 'img_tile_tl';
	              }
	              else if(((j==0 || j==7 || j==14) && (i==3 || i==11))
	                      || ((j==3 || j==11) &&(i==0 || i==7 || i==14))
	                      || ((j==2 || j==6 || j==8 || j==12) && (i==6 || i==8))
	                      || ((j==6 || j==8) && (i==2 || i==12)))
	              {
	                  imgname = 'img_tile_dl';
	              }
	              else if(j==7 && i==7)
	                  imgname = 'img_tile_center';
	
	              var img = new Kinetic.Image({
	                x: i*40,
	                y: j*40,
	                image: document.getElementById(imgname),
	                width: 40,
	                height: 40,
	                stroke:"White",
	                strokeWidth:10
	              });
	              img.setAttr('obj', new Tile(imgname, i, j, img));
	              tiles[i][j] = img;
	              boardbase.add(img);
	              img.on('mouseover', function() {
	                  writeMessage('Mouseover Tile' + this.getAttr('obj').name);
	              });
	              img.on('dragend', function() {
	                  writeMessage('dragend Tile' + this.getAttr('obj').name);
	              });
	              img.on('dragstart', function() {
	                  writeMessage('dragstart Tile' + this.getAttr('obj').name);
	              });
	          }
	      }
	      for(var i = 0; i < MAXRACK; i++) {
	          var img = new Kinetic.Image({
	              x: (MAXCOL-MAXRACK)*40/2 + i*40,
	              y: MAXROW*40 + 40/2,
	              image: document.getElementById('img_tile_plain'),
	              width: 40 - 2,
	              height: 40 - 2,
	              stroke:"Blue",
	              strokeWidth:10 + 4
	          });
	          img.setAttr('obj', new Rack('rack', i, img));
	          rack[i] = img;
	          boardbase.add(img);
	      }
	
	      var text = new Kinetic.Text({
	          x: 10,
	          y: 10,
	          fontFamily: 'Calibri',
	          fontSize: 24,
	          text: '',
	          fill: 'black'
	        });
	      layer.add(text);
	
	      stage.add(boardbase);
	      stage.add(layer);
	      stage.add(tilerack);
      }

      function populate_board(result) {
          if (result.fixed != null && result.letterrack != null) {
        	    tilerack.destroyChildren();
          }

          if (result.myturn != null)
              myturn = result.myturn;
          if (result.turnid != null)
        	  turnid = result.turnid;
          if (myturn) {
        	  $("#yourturn").show();
              $("#otherturn").hide();
          } else {
              $("#yourturn").hide();
              $("#otherturn").show();
              if(result.otherturn != null)
            	  $("#otherturn").html(result.otherturn);
          }
          if (result.scoreboard != null)
        	  $("#scoreboard").html(result.scoreboard);
   		  for (var ind=0; result.fixed != null && ind < result.fixed.length; ind+=3) {
   			  var i = result.fixed[ind+1];
   			  var j = result.fixed[ind+2];
   			  var t = tiles[i][j];
			  var img = new Kinetic.Image({
			    x: t.x()-2,
			    y: t.y()-2,
			    image: document.getElementById('img_alpha_' + result.fixed[ind]),
			    width: 40,
			    height: 40
			  });
			  t.getAttr('obj').letter = img;
			  tilerack.add(img);
   		  }
   		  for (var i=0; result.letterrack != null && i < result.letterrack.length && i < MAXRACK; i++) {
			  var img = new Kinetic.Image({
			    x: rack[i].x()-2,
			    y: rack[i].y()-2,
			    image: document.getElementById('img_alpha_' + result.letterrack[i]),
			    width: 40,
			    height: 40,
			    draggable: myturn
			  });
              img.setAttr('obj', new Letter(result.letterrack[i], img, rack[i]));
			  rack[i].getAttr('obj').letter = img;
			  usertiles[i] = img;
			  img.on("dragend", function(evt) {
                var thisobj = this.getAttr('obj');
			    var dst = boardbase.getIntersection(stage.getPointerPosition());
				if (!dst || !dst.getAttr('obj')) {
                    this.x(thisobj.pos.x()-2);
                    this.y(thisobj.pos.y()-2);
                    tilerack.draw();
				 	return;
				}
				var dstobj = dst.getAttr('obj');
				if (dstobj.letter) {
					// dropping on existing leter
					this.x(thisobj.pos.x()-2);
					this.y(thisobj.pos.y()-2);
					tilerack.draw();
					return;
				}
				thisobj.pos.getAttr('obj').letter = null;
				thisobj.pos = dst;
				dstobj.letter = this;
				this.x(dst.x()-2);
				this.y(dst.y()-2);
				tilerack.draw();
			  })
			  tilerack.add(img);
   		  }
		  tilerack.draw();
		  if (result.message != null)
			  dhtmlx.alert(result.message);
          load_score_history();
		  if (result.computerplayerturn != null) {
			  $.post("scrabble", {"gameid" : <%=gameid%>, "computerplayerturn" : "1"});
		  }
      }
      $(window).load(function() {
    	  load_initial_board();
    	  load_board();
      });
      function load_board()
      {
     	  channel = new goog.appengine.Channel('<%= com.google.appengine.api.channel.ChannelServiceFactory.getChannelService().createChannel(userid) %>');
   	      socket = channel.open();
   	      socket.onmessage = function(m) {
   	          populate_board(JSON.parse(m.data));
   	      }
   	      socket.onopen = function() {
              window.setTimeout(function() {
            	  $.post("scrabble", {"gameid" : <%=gameid%>, "initial" : "1"},
                         function(result) { populate_board(result); },
		                 "json");
              }, 2000);
   	      }
      }
      function load_score_history()
      {
          $.post("scorehistory",
                  {"gameid" : <%=gameid%>},
                  function(result) {
                      if (result.scorehistory != null) {
                    	  $("#scorehistory").empty();
                          for(var i=result.scorehistory.length-1; i>=0 ;i--) {
                              var ele = $('<div>' + result.scorehistory[i] + '</div>');
                              $("#scorehistory").prepend(ele);
                          }
                      }
                  },
                  "json");
      }
   	  function play_turn()
   	  {
   		  var savepos = "";
   		  for(var i=0; i<usertiles.length; i++) {
	   		  var pos = usertiles[i].getAttr('obj').pos;
	   		  var posobj = pos.getAttr('obj');
	   		  if (posobj.type == 'tile') {
	   			  var letterobj = posobj.letter.getAttr('obj');
	   	   		  savepos = savepos.concat(posobj.i + "," + posobj.j + "," + letterobj.name + "," + i + ",");
	   		  }
   		  }
	 	  if (savepos.length == 0)
     		  dhtmlx.confirm({ text:"Are you sure you want to pass your turn ?", ok:"Pass your turn", cancel:"Cancel",
    				  		   callback: function(result) { if(result) submit_result(savepos); } });
	 	  else submit_result(savepos);
   	  }
   	  function submit_result(savepos)
   	  {
   		  writeMessage('submit' + savepos);
   	   	  $.post("scrabble",
   			  	{"gameid" : <%=gameid%>, "playmove" : savepos},
                function(result) { populate_board(result); },
                "json");
   	  }
    </script>
  </body>
</html>
