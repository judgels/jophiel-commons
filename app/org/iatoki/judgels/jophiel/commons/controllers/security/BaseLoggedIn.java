package org.iatoki.judgels.jophiel.commons.controllers.security;

import org.iatoki.judgels.jophiel.commons.JophielUtils;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

public abstract class BaseLoggedIn extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {
        return JophielUtils.checkSession(context);
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return redirect(getRedirectCall(context));
    }

    public abstract Call getRedirectCall(Http.Context context);
}
