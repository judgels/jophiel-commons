package org.iatoki.judgels.jophiel.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.apache.commons.codec.binary.Base64;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.jophiel.User;
import play.Play;
import play.libs.Json;
import play.mvc.Http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class JophielUtils {

    private JophielUtils() {
        // prevent instantiation
    }

    public static ClientID getClientJid() {
        String clientId = Play.application().configuration().getString("jophiel.clientJid");
        if (clientId == null) {
            throw new IllegalStateException("jophiel.clientJid not found in configuration");
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

    public static String verifyUsername(String username) {
        HTTPRequest httpRequest;
        try {
            httpRequest = new HTTPRequest(HTTPRequest.Method.GET, getEndpoint("verifyUser").toURL());
            httpRequest.setAuthorization("Bearer " + JophielUtils.getEncodedAccessToken());
            httpRequest.setQuery("username=" + username);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try {
            HTTPResponse httpResponse = httpRequest.send();
            if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
                JsonNode jsonNode = Json.parse(httpResponse.getContent());
                if (jsonNode.get("success").asBoolean()) {
                    return jsonNode.get("jid").asText();
                } else {
                    return null;
                }
            } else {
                return null;
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
                User user = new User(response.get("id").asInt(), response.get("jid").asText(), response.get("username").asText(), response.get("name").asText(), response.get("email").asText(), new URL(response.get("profilePictureUrl").asText()));

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

    public static URL getDefaultAvatarUrl() throws MalformedURLException {
        return getEndpoint("assets/images/avatar/avatar-default.png").toURL();
    }

    public static void updateUserAvatarCache(AbstractAvatarCacheService<?> avatarCacheService) {
        if (IdentityUtils.getUserJid() != null) {
            try {
                avatarCacheService.putImageUrl(IdentityUtils.getUserJid(), new URL(Http.Context.current().session().get("avatar")), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
