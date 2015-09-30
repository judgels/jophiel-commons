package org.iatoki.judgels.jophiel.controllers;

import org.apache.http.client.utils.URIBuilder;
import org.iatoki.judgels.jophiel.services.impls.AbstractBaseAvatarCacheServiceImpl;
import org.iatoki.judgels.play.IdentityUtils;
import play.mvc.Http;

import java.net.URISyntaxException;

public final class JophielClientControllerUtils {

    private static JophielClientControllerUtils INSTANCE;

    private final String baseUrl;

    private JophielClientControllerUtils(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUserViewProfileUrl() {
        return baseUrl + "/profiles/search";
    }

    public String getRegisterUrl() {
        return baseUrl + "/register";
    }

    public String getUserDefaultAvatarUrl() {
        return baseUrl + "/assets/images/avatar/avatar-default.png";
    }

    public String getServiceLogoutUrl(String returnUri) {
        try {
            return new URIBuilder(baseUrl).setPath("/serviceLogout").addParameter("continueUrl", returnUri).build().toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void buildInstance(String baseUrl) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("JophielClientControllerUtils instance has already been built");
        }
        INSTANCE = new JophielClientControllerUtils(baseUrl);
    }

    public static JophielClientControllerUtils getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("JophielClientControllerUtils instance has not been built");
        }
        return INSTANCE;
    }

    public static void updateUserAvatarCache(AbstractBaseAvatarCacheServiceImpl<?> avatarCacheService) {
        if (IdentityUtils.getUserJid() != null) {
            avatarCacheService.putImageUrl(IdentityUtils.getUserJid(), Http.Context.current().session().get("avatar"), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
        }
    }
}
