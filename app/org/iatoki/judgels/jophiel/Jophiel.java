package org.iatoki.judgels.jophiel;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.iatoki.judgels.AbstractJudgelsClient;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.jophiel.services.impls.AbstractBaseAvatarCacheServiceImpl;
import play.mvc.Http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Jophiel extends AbstractJudgelsClient {

    private final ClientID clientId;
    private final Secret clientSecret;
    private final Lock activityLock;

    public Jophiel(String baseUrl, String clientJid, String clientSecret) {
        super(baseUrl, clientJid, clientSecret);
        this.clientId = new ClientID(clientJid);
        this.clientSecret = new Secret(clientSecret);
        this.activityLock = new ReentrantLock();
    }

    boolean sendUserActivityMessages(String accessToken, List<UserActivityMessage> activityLogList) throws IOException {
        CloseableHttpClient httpClient = getHttpClient();

        List<NameValuePair> params = ImmutableList.of(
              new BasicNameValuePair("userActivities", new Gson().toJson(activityLogList))
        );

        HttpPost request = new HttpPost(getEndpoint("/userActivities"));
        request.setEntity(new UrlEncodedFormEntity(params));
        request.setHeader(new BasicHeader("Authorization", "Bearer " + Base64.encodeBase64String(accessToken.getBytes())));

        boolean success = false;
        String result = executeHttpRequest(httpClient, request);
        if (result != null) {
            BasicResponse response = new Gson().fromJson(result, BasicResponse.class);
            if (response.success) {
                success = true;
            }
        }

        return success;
    }

    public String verifyUsername(String username) throws IOException {
        CloseableHttpClient httpClient = getHttpClient();

        List<NameValuePair> params = ImmutableList.of(
              new BasicNameValuePair("username", username)
        );

        HttpGet request = new HttpGet(getEndpoint("/verifyUsername", params));

        String result = executeHttpRequest(httpClient, request);
        if (result != null) {
            UserVerifyResponse verifyResponse = new Gson().fromJson(result, UserVerifyResponse.class);
            if (verifyResponse.success) {
                result = verifyResponse.jid;
            } else {
                result = null;
            }
        }

        return result;
    }

    public UserInfo getUserByUserJid(String userJid) throws IOException {
        CloseableHttpClient httpClient = getHttpClient();

        List<NameValuePair> params = ImmutableList.of(
              new BasicNameValuePair("userJid", userJid)
        );

        HttpGet request = new HttpGet(getEndpoint("/userInfoByJid", params));

        UserInfo userInfo = null;
        String result = executeHttpRequest(httpClient, request);
        if (result != null) {
            userInfo = new Gson().fromJson(result, UserInfo.class);
        }

        return userInfo;
    }

    public URI getAuthRequestUri(URI redirectUri, String returnUri) {
        URI endpoint = getEndpoint("/auth");
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
        URI endpoint = getEndpoint("/token");

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

    public URI getServiceProfileUri(String returnUri) {
        List<NameValuePair> params = ImmutableList.of(
                new BasicNameValuePair("continueUrl", returnUri)
        );

        return getEndpoint("/serviceProfile", params);
    }

    public URI getServiceLogout(String returnUri) {
        List<NameValuePair> params = ImmutableList.of(
                new BasicNameValuePair("continueUrl", returnUri)
        );

        return getEndpoint("/serviceLogout", params);
    }

    public UserInfoResponse getUserInfoRequest(String accessToken) {
        HTTPRequest httpRequest;
        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, getEndpoint("/userinfo").toURL());
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
            return UserInfoResponse.parse(httpResponse);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAutoCompleteEndPoint() {
        try {
            return getEndpoint("/userAutoComplete").toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLinkedClientsEndPoint() {
        try {
            return getEndpoint("/linkedClients").toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URL getDefaultAvatarUrl() throws MalformedURLException {
        return getEndpoint("/assets/images/avatar/avatar-default.png").toURL();
    }

    public Lock getActivityLock() {
        return activityLock;
    }

    @Override
    protected String getClientName() {
        return "Jophiel";
    }

    public static void updateUserAvatarCache(AbstractBaseAvatarCacheServiceImpl<?> avatarCacheService) {
        if (IdentityUtils.getUserJid() != null) {
            try {
                avatarCacheService.putImageUrl(IdentityUtils.getUserJid(), new URL(Http.Context.current().session().get("avatar")), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
            } catch (MalformedURLException e) {
                throw new IllegalStateException("This exception should not happened", e);
            }
        }
    }

    public static String getSessionVersion() {
        return "1";
    }
}
