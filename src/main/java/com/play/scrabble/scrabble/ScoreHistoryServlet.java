package com.play.scrabble.scrabble;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import com.play.scrabble.db.Game;

@SuppressWarnings("serial")
public class ScoreHistoryServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        long gameid = 0;
        try {
            gameid = Long.parseLong(req.getParameter("gameid"));
        } catch(Exception e) {
            return;
        }
        
        try {
            Game game = Game.load(gameid);
            if (game.movehistory.isEmpty())
                return;
            ArrayList<String> res = game.getMoveHistory();
            JSONObject json = new JSONObject();
            JSONArray jscorehist = new JSONArray();
            for(String s : res)
                jscorehist.put(s);
            json.put("scorehistory", jscorehist);
            resp.getWriter().print(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        return;
    }

}
