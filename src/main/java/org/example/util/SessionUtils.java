package org.example.util;

import javax.servlet.http.HttpSession;

public class SessionUtils {

    public static String getUsername(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user == null) return null;
        try {
            return (String) user.getClass().getMethod("getUsername").invoke(user);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRole(HttpSession session) {
        Object user = session.getAttribute("currentUser");
        if (user == null) return null;
        try {
            return (String) user.getClass().getMethod("getRole").invoke(user);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }

    public static boolean isStudent(HttpSession session) {
        return "student".equals(getRole(session));
    }

    public static boolean isTeacher(HttpSession session) {
        return "teacher".equals(getRole(session));
    }
}
