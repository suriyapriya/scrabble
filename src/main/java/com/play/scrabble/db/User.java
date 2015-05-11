package com.play.scrabble.db;
import java.util.ArrayList;
import java.util.List;

import com.play.scrabble.scrabble.ComputerPlayer;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.ObjectifyService;

@Entity
public class User {
    @Id
    public String id;
    public String email;
    public String nickname;
    public List<Long> games;
    
    public User() {}
    public User(String id, String email, String nickname) 
    {
    	this.id = id;
    	this.nickname = nickname;
    	this.email = email;
    	this.games = new ArrayList<Long>();
	}
    
    public static User load(String uid)
    {
  	  return ObjectifyService.ofy().load().type(User.class).id(uid).get();
    }
    public void save()
    {
        ObjectifyService.ofy().save().entity(this).now();
    }
    public boolean isComputerPlayer()
    {
        return ComputerPlayer.userid.equals(id);
    }
    public static boolean isComputerPlayer(String id)
    {
        return ComputerPlayer.userid.equals(id);
    }
    public boolean isGuestUser()
    {
        return id.startsWith("guest-");
    }
}
