package org.iatoki.judgels.jophiel.controllers;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import org.apache.commons.codec.binary.Base64;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.jophiel.Jophiel;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public final class JophielClientController extends Controller {

    private final Jophiel jophiel;
    private final BaseUserService userService;

    public JophielClientController(Jophiel jophiel, BaseUserService userService) {
        this.jophiel = jophiel;
        this.userService = userService;
    }

    public Result login(String returnUri) {
        URI endpoint = jophiel.getEndpoint("auth");
        ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
        Scope scope = Scope.parse("openid offline_access");
        ClientID clientId = new ClientID(jophiel.getClientJid());
        URI redirectUri = getRedirectUri();

        State state = new State(returnUri);
        Nonce nonce = new Nonce();

        AuthenticationRequest authRequest = new AuthenticationRequest(endpoint, responseType, scope, clientId, redirectUri, state, nonce);

        URI authRequestUri;
        try {
            authRequestUri = authRequest.toURI();
        } catch (SerializeException e) {
            throw new RuntimeException(e);
        }

        return redirect(authRequestUri.toString());
    }

    @Transactional
    public Result verify() {
        URI authResponseRequestUri;

        try {
            authResponseRequestUri = new URI(request().uri());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        AuthenticationResponse authResponse;
        try {
            authResponse = AuthenticationResponseParser.parse(authResponseRequestUri);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (authResponse instanceof AuthenticationErrorResponse) {
            throw new RuntimeException("Authentication error");
        }

        AuthenticationSuccessResponse successResponse = (AuthenticationSuccessResponse) authResponse;

        AuthorizationCode authCode = successResponse.getAuthorizationCode();
        String returnUri = successResponse.getState().getValue();

        URI endpoint = jophiel.getEndpoint("token");
        ClientAuthentication clientAuth = new ClientSecretBasic(new ClientID(jophiel.getClientJid()), new Secret(jophiel.getClientSecret()));
        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(authCode, getRedirectUri());
        Scope scope = Scope.parse("openid offline_access");

        TokenRequest tokenRequest = new TokenRequest(endpoint, clientAuth, grant, scope);

        HTTPRequest httpRequest;
        try {
            httpRequest = tokenRequest.toHTTPRequest();
        } catch (SerializeException e) {
            throw new RuntimeException(e);
        }

        HTTPResponse httpResponse;
        try {
            httpResponse = httpRequest.send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        OIDCAccessTokenResponse accessTokenResponse;
        try {
            accessTokenResponse = OIDCAccessTokenResponse.parse(httpResponse);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

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
        try {
            returnUri = org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.afterProfile(returnUri).absoluteURL(request(), request().secure());
            URI profileUri = jophiel.getEndpoint("serviceProfile/" + URLEncoder.encode(returnUri, "UTF-8"));

            return redirect(profileUri.toString() + "");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Result afterProfile(String returnUri) {
        refreshUserInfo(userService.getUserTokensByUserJid(IdentityUtils.getUserJid()).getAccessToken());
        return redirect(returnUri);
    }

    public void refreshUserInfo(String accessToken) {
        HTTPRequest httpRequest;
        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, jophiel.getEndpoint("userinfo").toURL());
            httpRequest.setAuthorization("Bearer "+ Base64.encodeBase64String(accessToken.getBytes()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        HTTPResponse httpResponse;
        try {
            httpResponse = httpRequest.send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResponse);
            if (userInfoResponse instanceof UserInfoSuccessResponse) {
                UserInfoSuccessResponse userInfoSuccessResponse = (UserInfoSuccessResponse) userInfoResponse;
                session("name", userInfoSuccessResponse.getUserInfo().getName());
                session("email", userInfoSuccessResponse.getUserInfo().getEmail().toString());
                session("username", userInfoSuccessResponse.getUserInfo().getPreferredUsername());
                session("avatar", userInfoSuccessResponse.getUserInfo().getPicture().toString());
            } else {
                UserInfoErrorResponse userInfoErrorResponse = (UserInfoErrorResponse) userInfoResponse;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public Result logout(String returnUri) {
        try {
            URI logoutUri = jophiel.getEndpoint("serviceLogout/" + URLEncoder.encode(returnUri, "UTF-8"));

            session().clear();
            return redirect(logoutUri.toString() + "");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
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
