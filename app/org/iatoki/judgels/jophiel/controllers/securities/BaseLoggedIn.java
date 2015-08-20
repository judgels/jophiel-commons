package org.iatoki.judgels.jophiel.controllers.securities;

import org.iatoki.judgels.jophiel.Jophiel;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

public abstract class BaseLoggedIn extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {
        try {
            if (!context.session().containsKey("version") || !context.session().get("version").equals(Jophiel.getSessionVersion())) {
                context.session().remove("username");
                return null;
            }

            if (context.request().method().equals("POST")) {
                return context.session().get("username");
            }

            if (!context.session().containsKey("expirationTime") || !(System.currentTimeMillis() < Long.parseLong(context.session().get("expirationTime")))) {
                context.session().remove("username");
                return null;
            }

            return context.session().get("username");
        } catch (NumberFormatException e) {
            context.session().remove("username");
            return null;
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return redirect(getRedirectCall(context));
    }

    public abstract Call getRedirectCall(Http.Context context);
}
