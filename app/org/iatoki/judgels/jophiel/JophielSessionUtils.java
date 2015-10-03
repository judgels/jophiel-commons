package org.iatoki.judgels.jophiel;

import play.mvc.Http;

public final class JophielSessionUtils {

    private JophielSessionUtils() {
        // prevent instantiation
    }

    public static String getSessionVersion() {
        return "1";
    }

    public static boolean isSessionValid(Http.Context context) {
        try {
            if (!context.session().containsKey("version") || !context.session().get("version").equals(getSessionVersion())) {
                context.session().remove("username");
                context.session().remove("role");
                return false;
            }

            if (context.request().method().equals("POST")) {
                return true;
            }

            if (!context.session().containsKey("expirationTime") || !(System.currentTimeMillis() < Long.parseLong(context.session().get("expirationTime")))) {
                context.session().remove("username");
                context.session().remove("role");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            context.session().remove("username");
            context.session().remove("role");
            return false;
        }
    }
}
