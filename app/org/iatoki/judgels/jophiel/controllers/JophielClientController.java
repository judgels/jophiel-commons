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
import org.iatoki.judgels.jophiel.JophielAuthAPI;
import org.iatoki.judgels.jophiel.JophielSessionUtils;
import org.iatoki.judgels.jophiel.services.BaseUserService;
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

    private final JophielAuthAPI jophielAuthAPI;
    private final BaseUserService userService;

    @Inject
    public JophielClientController(JophielAuthAPI jophielAuthAPI, BaseUserService userService) {
        this.jophielAuthAPI = jophielAuthAPI;
        this.userService = userService;
    }

    public Result login(String returnUri) {
        return redirect(jophielAuthAPI.getAuthRequestUri(getRedirectUri(), returnUri).toString());
    }

    @Transactional
    public Result verify() {
        URI authResponseUri;

        try {
            authResponseUri = new URI(request().uri());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        AuthenticationResponse authResponse = jophielAuthAPI.parseAuthResponse(authResponseUri);
        if (authResponse instanceof AuthenticationErrorResponse) {
            throw new RuntimeException("Authentication error");
        }

        AuthenticationSuccessResponse successResponse = (AuthenticationSuccessResponse) authResponse;

        AuthorizationCode authCode = successResponse.getAuthorizationCode();
        String returnUri = successResponse.getState().getValue();

        OIDCAccessTokenResponse accessTokenResponse = jophielAuthAPI.sendAccessTokenRequest(authCode, getRedirectUri());

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
        session("version", JophielSessionUtils.getSessionVersion());

        userService.upsertUser(userJid, accessToken.toString(), refreshToken.toString(), idToken.serialize(), expirationTime);

        refreshUserInfo(accessToken.toString());

        return redirect(returnUri);
    }

    public Result profile() {
        return redirect(JophielClientControllerUtils.getInstance().getUserEditProfileUrl());
    }

    public Result logout(String returnUri) {
        session().clear();
        return redirect(JophielClientControllerUtils.getInstance().getServiceLogoutUrl(returnUri));
    }

    private void refreshUserInfo(String accessToken) {
        UserInfoResponse userInfoResponse = jophielAuthAPI.getUserInfoRequest(accessToken);
        if (userInfoResponse instanceof UserInfoSuccessResponse) {
            UserInfoSuccessResponse userInfoSuccessResponse = (UserInfoSuccessResponse) userInfoResponse;
            if (userInfoSuccessResponse.getUserInfo().getName() != null) {
                session("name", userInfoSuccessResponse.getUserInfo().getName());
            }
            session("username", userInfoSuccessResponse.getUserInfo().getPreferredUsername());
            session("avatar", userInfoSuccessResponse.getUserInfo().getPicture().toString());
        } else {
            UserInfoErrorResponse userInfoErrorResponse = (UserInfoErrorResponse) userInfoResponse;

            //todo handle this
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
