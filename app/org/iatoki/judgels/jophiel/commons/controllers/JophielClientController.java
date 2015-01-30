package org.iatoki.judgels.jophiel.commons.controllers;

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
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
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
import org.iatoki.judgels.jophiel.commons.JophielUtils;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public final class JophielClientController extends Controller {

    public static Result login(String returnUri) {
        URI endpoint = JophielUtils.getEndpoint("auth");
        ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
        Scope scope = Scope.parse("openid");
        ClientID clientId = JophielUtils.getClientId();
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

    public static Result verify() {
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

        URI endpoint = JophielUtils.getEndpoint("token");
        ClientAuthentication clientAuth = new ClientSecretBasic(JophielUtils.getClientId(), JophielUtils.getClientSecret());
        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(authCode, getRedirectUri());
        Scope scope = new Scope("openid");

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
        JWT idToken = accessTokenResponse.getIDToken();

        ReadOnlyJWTClaimsSet claimsSet;
        try {
            claimsSet = idToken.getJWTClaimsSet();
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }

        String userJid = claimsSet.getSubject();
        long expirationTime = System.currentTimeMillis() + Long.valueOf(accessTokenResponse.getCustomParams().get("expire_in").toString());

        session().clear();
        session("userJid", userJid);
        session("accessToken", accessToken.toString());
        session("expirationTime", "" + expirationTime);
        session("idToken", idToken.toString());

        BearerAccessToken bearerAccessToken;
        try {
            bearerAccessToken = BearerAccessToken.parse(httpResponse.getContentAsJSONObject());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, JophielUtils.getEndpoint("userinfo").toURL());
            httpRequest.setAuthorization("Bearer "+ Base64.encodeBase64String(accessToken.toString().getBytes()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

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
            } else {
                UserInfoErrorResponse userInfoErrorResponse = (UserInfoErrorResponse) userInfoResponse;
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return redirect(returnUri);
    }

    public static Result logout(String returnUri) {
        session().clear();
        return redirect(returnUri);
    }

    private static URI getRedirectUri() {
        try {
            return new URI(routes.JophielClientController.verify().absoluteURL(request()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
