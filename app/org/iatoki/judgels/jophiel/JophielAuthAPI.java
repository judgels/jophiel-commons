package org.iatoki.judgels.jophiel;

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
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public final class JophielAuthAPI {

    private final String baseUrl;
    private final ClientID clientId;
    private final Secret clientSecret;

    public JophielAuthAPI(String baseUrl, String clientJid, String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientId = new ClientID(clientJid);
        this.clientSecret = new Secret(clientSecret);
    }

    public URI getAuthRequestUri(URI redirectUri, String returnUri) {
        URI endpoint = getEndpoint("/api/oauth2/auth");
        ResponseType responseType = new ResponseType(ResponseType.Value.CODE);
        Scope scope = Scope.parse("openid offline_access");

        State state = new State(returnUri);
        Nonce nonce = new Nonce();

        AuthenticationRequest authRequest = new AuthenticationRequest(endpoint, responseType, scope, clientId, redirectUri, state, nonce);

        URI authRequestUri;
        try {
            authRequestUri = authRequest.toURI();
        } catch (SerializeException e) {
            throw new RuntimeException(e);
        }

        return authRequestUri;
    }

    public AuthenticationResponse parseAuthResponse(URI responseUri) {
        AuthenticationResponse authResponse;
        try {
            authResponse = AuthenticationResponseParser.parse(responseUri);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return authResponse;
    }

    public OIDCAccessTokenResponse sendAccessTokenRequest(AuthorizationCode authCode, URI redirectUri) {
        URI endpoint = getEndpoint("/api/oauth2/token");

        ClientAuthentication clientAuth = new ClientSecretBasic(clientId, clientSecret);
        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(authCode, redirectUri);
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

        return accessTokenResponse;
    }

    public UserInfoResponse getUserInfoRequest(String accessToken) {
        HTTPRequest httpRequest;
        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, getEndpoint("/api/oauth2/userinfo").toURL());
            httpRequest.setAuthorization("Bearer " + Base64.encodeBase64String(accessToken.getBytes()));
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
            return UserInfoResponse.parse(httpResponse);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected URI getEndpoint(String path) {
        return getEndpoint(path, null);
    }

    protected URI getEndpoint(String path, List<NameValuePair> params) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            uriBuilder.setPath(path);
            if (params != null) {
                uriBuilder.setParameters(params);
            }

            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
