<%@ page import="com.play.scrabble.user.MyUserService" %>
<%@ page import="com.play.scrabble.db.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
User user = MyUserService.getCurrentUser(session);
session.invalidate();
if (user.isGuestUser()) {
    response.sendRedirect("/");
} else {
    response.sendRedirect(MyUserService.createGoogleLogoutURL());
}
%>
