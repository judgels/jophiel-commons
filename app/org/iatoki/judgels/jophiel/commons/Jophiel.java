package org.iatoki.judgels.jophiel.commons;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.binary.Base64;
import org.iatoki.judgels.commons.IdentityUtils;
import play.libs.Json;
import play.mvc.Http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Jophiel {

    private final Lock activityLock;
    private final String clientJid;
    private final String clientSecret;
    private final String baseUrl;

    public Jophiel(String clientJid, String clientSecret, String baseUrl) {
        this.activityLock = new ReentrantLock();
        this.clientJid = clientJid;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl;
    }

    public String verifyUsername(String username) {
        try {
            URL url = getEndpoint("verifyUsername?username=" + URLEncoder.encode(username, "UTF-8")).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + getBase64Encoded(clientJid + ":" + clientSecret));
            connection.setDoOutput(true);

            connection.connect();

            InputStream is = connection.getInputStream();
            JsonNode jsonNode = Json.parse(is);
            is.close();

            if (jsonNode.get("success").asBoolean()) {
                return jsonNode.get("jid").asText();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    boolean sendUserActivities(String accessToken, List<UserActivity> activityLogList) {
        try {
            URL url = getEndpoint("userActivities").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + getBase64Encoded(accessToken));
            connection.setDoInput(true);
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write("userActivities=" + URLEncoder.encode(Json.toJson(activityLogList).toString(), "UTF-8"));
            writer.flush();
            writer.close();
            os.close();

            connection.connect();

            InputStream is = connection.getInputStream();
            JsonNode jsonNode = Json.parse(is);
            is.close();

            return jsonNode.get("success").asBoolean();
        } catch (IOException e) {
            return false;
        }
    }

    public User getUserByUserJid(String userJid) throws IOException {
        URL url = getEndpoint("userInfoByJid?userJid=" + URLEncoder.encode(userJid, "UTF-8")).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic " + getBase64Encoded(clientJid + ":" + clientSecret));
        connection.setDoOutput(true);

        connection.connect();

        InputStream is = connection.getInputStream();
        JsonNode jsonNode = Json.parse(is);
        is.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            User user = new User(jsonNode.get("id").asInt(), jsonNode.get("jid").asText(), jsonNode.get("username").asText(), jsonNode.get("name").asText(), jsonNode.get("email").asText(), new URL(jsonNode.get("profilePictureUrl").asText()), null);

            return user;
        } else {
            throw new IOException();
        }
    }

    public String getAutoCompleteEndPoint() {
        try {
            return getEndpoint("userAutoComplete").toURL().toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URI getEndpoint(String service) {
        if (baseUrl == null) {
            throw new IllegalStateException("jophiel.baseUrl not found in configuration");
        }

        try {
            return new URI(baseUrl + "/" + service);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("jophiel.baseUrl malformed in configuration");
        }
    }

    public URL getDefaultAvatarUrl() throws MalformedURLException {
        return getEndpoint("assets/images/avatar/avatar-default.png").toURL();
    }

    public Lock getActivityLock() {
        return activityLock;
    }

    public String getClientJid() {
        return clientJid;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public static void updateUserAvatarCache(AbstractAvatarCacheService<?> avatarCacheService) {
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

    private String getBase64Encoded(String s) {
        return Base64.encodeBase64String(s.getBytes());
    }
}
