<%@page import="com.play.scrabble.user.MyUserService"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
String res = MyUserService.getNewGuestUser(session);
if (res != null) {
    response.getWriter().println(res);
} else
    response.sendRedirect("/");
%>
