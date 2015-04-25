package org.iatoki.judgels.jophiel.commons.controllers.security;

import org.iatoki.judgels.jophiel.commons.JophielUtils;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.util.concurrent.TimeUnit;

public abstract class BaseLoggedIn extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {
        try {
            if ((context.session().containsKey("version")) && (context.session().get("version").equals(JophielUtils.getSessionVersion())) ) {
                if (context.request().method() != "GET") {
                    // for not get method giff buffer for one hour after expiration time
                    if ((context.session().containsKey("expirationTime")) && (System.currentTimeMillis() < Long.parseLong(context.session().get("expirationTime")) + TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS))) {
                        return context.session().get("username");
                    } else {
                        context.session().remove("username");
                        return null;
                    }
                } else {
                    if ((context.session().containsKey("expirationTime")) && (System.currentTimeMillis() < Long.parseLong(context.session().get("expirationTime")))) {
                        return context.session().get("username");
                    } else {
                        context.session().remove("username");
                        return null;
                    }
                }
            } else {
                context.session().remove("username");
                return null;
            }
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
