package com.play.scrabble.user;
import java.util.Random;

import javax.servlet.http.HttpSession;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import com.play.scrabble.db.User;

public class MyUserService {
    UserService userService = UserServiceFactory.getUserService();

    public static MyUserService getUserService()
    {
        return new MyUserService();
    }
    
    public static User getCurrentUser(HttpSession session)
    {
        MyUserService service = getUserService();
        String userid = (String) session.getAttribute("userid");

        if (userid == null) {
            // first try google email
            com.google.appengine.api.users.User gmailuser = service.userService.getCurrentUser();
            if (gmailuser != null) {
                userid = gmailuser.getUserId();
                session.setAttribute("userid", userid);
                if (User.load(userid) == null)
                    new User(gmailuser.getUserId(), gmailuser.getEmail(),
                            gmailuser.getNickname()).save();
            }
        }
        if (userid != null)
            return User.load(userid);
        return null;
    }
    public static String getNewGuestUser(HttpSession session)
    {
        // lets create a unique guest login account
        Random rand = new Random();
        String userid;
        int tries = 100;
        do {
            userid = "guest-" + (rand.nextInt(100) + 1);
            System.out.println("guestuser " + userid);
        } while(User.load(userid) != null && tries > 0);
        if (tries > 0) {
            new User(userid, userid + "@guest.com", userid).save();
            session.setAttribute("userid", userid);
            return null;
        }
        return "Database error: cannot create guest account";
    }
    public static String createGoogleLoginURL()
    {
        return new MyUserService().userService.createLoginURL("/");
    }
    public static String createGoogleLogoutURL()
    {
        return new MyUserService().userService.createLogoutURL("/");
    }
    public static String createGuestLoginURL()
    {
        return "guestlogin.jsp";
    }
}
