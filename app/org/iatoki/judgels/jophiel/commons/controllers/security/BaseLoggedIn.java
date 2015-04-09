package org.iatoki.judgels.jophiel.commons.controllers.security;

import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

public abstract class BaseLoggedIn extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {
        try {
            if (System.currentTimeMillis() < Long.parseLong(context.session().get("expirationTime"))) {
                return context.session().get("username");
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return redirect(getRedirectCall(context));
    }

    public abstract Call getRedirectCall(Http.Context context);
}
