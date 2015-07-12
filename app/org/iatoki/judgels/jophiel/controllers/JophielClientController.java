package org.iatoki.judgels.jophiel.controllers;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.jophiel.Jophiel;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Named
public final class JophielClientController extends Controller {

    private final Jophiel jophiel;
    private final BaseUserService userService;

    @Inject
    public JophielClientController(Jophiel jophiel, BaseUserService userService) {
        this.jophiel = jophiel;
        this.userService = userService;
    }

    public Result login(String returnUri) {
        return redirect(jophiel.getAuthRequestUri(getRedirectUri(), returnUri).toString());
    }

    @Transactional
    public Result verify() {
        URI authResponseUri;

        try {
            authResponseUri = new URI(request().uri());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        AuthenticationResponse authResponse = jophiel.parseAuthResponse(authResponseUri);
        if (authResponse instanceof AuthenticationErrorResponse) {
            throw new RuntimeException("Authentication error");
        }

        AuthenticationSuccessResponse successResponse = (AuthenticationSuccessResponse) authResponse;

        AuthorizationCode authCode = successResponse.getAuthorizationCode();
        String returnUri = successResponse.getState().getValue();

        OIDCAccessTokenResponse accessTokenResponse = jophiel.sendAccessTokenRequest(authCode, getRedirectUri());

        AccessToken accessToken = accessTokenResponse.getAccessToken();
        RefreshToken refreshToken = accessTokenResponse.getRefreshToken();
        JWT idToken = accessTokenResponse.getIDToken();

        ReadOnlyJWTClaimsSet claimsSet;
        try {
            claimsSet = idToken.getJWTClaimsSet();
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }

        String userJid = claimsSet.getSubject();
        long expirationTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(Long.valueOf(accessTokenResponse.getCustomParams().get("expire_in").toString()), TimeUnit.SECONDS);

        session("userJid", userJid);
        session("expirationTime", expirationTime + "");
        session("version", jophiel.getSessionVersion());

        userService.upsertUser(userJid, accessToken.toString(), refreshToken.toString(), idToken.serialize(), expirationTime);

        refreshUserInfo(accessToken.toString());

        return redirect(returnUri);
    }

    public Result profile(String returnUri) {
        String wrappedReturnUri = routes.JophielClientController.afterProfile(returnUri).absoluteURL(request(), request().secure());
        URI profileUri = jophiel.getServiceProfileUri(wrappedReturnUri);

        return redirect(profileUri.toString());
    }

    @Transactional
    public Result afterProfile(String returnUri) {
        refreshUserInfo(userService.getUserTokensByUserJid(IdentityUtils.getUserJid()).getAccessToken());
        return redirect(returnUri);
    }

    public Result logout(String returnUri) {
        URI logoutUri = jophiel.getServiceLogout(returnUri);

        session().clear();
        return redirect(logoutUri.toString());
    }

    private void refreshUserInfo(String accessToken) {
        UserInfoResponse userInfoResponse = jophiel.getUserInfoRequest(accessToken);
        if (userInfoResponse instanceof UserInfoSuccessResponse) {
            UserInfoSuccessResponse userInfoSuccessResponse = (UserInfoSuccessResponse) userInfoResponse;
            session("name", userInfoSuccessResponse.getUserInfo().getName());
            session("email", userInfoSuccessResponse.getUserInfo().getEmail().toString());
            session("username", userInfoSuccessResponse.getUserInfo().getPreferredUsername());
            session("avatar", userInfoSuccessResponse.getUserInfo().getPicture().toString());
        } else {
            UserInfoErrorResponse userInfoErrorResponse = (UserInfoErrorResponse) userInfoResponse;
        }
    }

    private URI getRedirectUri() {
        try {
            return new URI(routes.JophielClientController.verify().absoluteURL(request(), request().secure()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
