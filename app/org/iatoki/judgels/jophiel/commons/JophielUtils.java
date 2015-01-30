package org.iatoki.judgels.jophiel.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.apache.commons.codec.binary.Base64;
import org.iatoki.judgels.jophiel.User;
import play.Play;
import play.libs.Json;
import play.mvc.Http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public final class JophielUtils {

    private JophielUtils() {
        // prevent instantiation
    }

    public static ClientID getClientId() {
        String clientId = Play.application().configuration().getString("jophiel.clientId");
        if (clientId == null) {
            throw new IllegalStateException("jophiel.clientId not found in configuration");
        }
        return new ClientID(clientId);
    }

    public static Secret getClientSecret() {
        String clientSecret = Play.application().configuration().getString("jophiel.clientSecret");
        if (clientSecret == null) {
            throw new IllegalStateException("jophiel.clientSecret not found in configuration");
        }
        return new Secret(clientSecret);
    }

    public static String getAccessToken() {
        return Http.Context.current().session().get("accessToken");
    }

    public static String getEncodedAccessToken() {
        return Base64.encodeBase64String(getAccessToken().getBytes());
    }

    public static boolean verifyUserJid(String userJid) {
        HTTPRequest httpRequest;
        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, getEndpoint("verifyUser").toURL());
            httpRequest.setAuthorization("Bearer "+ JophielUtils.getEncodedAccessToken());
            httpRequest.setQuery("userJid=" + userJid);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try {
            HTTPResponse httpResponse = httpRequest.send();
            if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static User getUserByJid(String userJid) {
        HTTPRequest httpRequest;
        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, getEndpoint("userInfoByJid").toURL());
            httpRequest.setAuthorization("Bearer "+ JophielUtils.getEncodedAccessToken());
            httpRequest.setQuery("userJid=" + userJid);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try {
            HTTPResponse httpResponse = httpRequest.send();
            if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
                JsonNode response = Json.parse(httpResponse.getContent());
                User user = new User(response.get("id").asInt(), response.get("jid").asText(), response.get("username").asText(), response.get("name").asText(), response.get("email").asText());

                return user;
            } else {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAutoCompleteEndPoint() {
        try {
            return getEndpoint("userAutoComplete").toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static URI getEndpoint(String service) {
        String baseUrl = Play.application().configuration().getString("jophiel.baseUrl");
        if (baseUrl == null) {
            throw new IllegalStateException("jophiel.baseUrl not found in configuration");
        }

        try {
            return new URI(baseUrl + "/" + service);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("jophiel.baseUrl malformed in configuration");
        }
    }
}
