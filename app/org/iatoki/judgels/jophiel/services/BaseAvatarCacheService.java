package org.iatoki.judgels.jophiel.services;

import org.iatoki.judgels.jophiel.models.entities.AbstractAvatarCacheModel;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface BaseAvatarCacheService<M extends AbstractAvatarCacheModel> {

    void putImageUrl(String userJid, String imageUrl, String user, String ipAddress);

    URL getAvatarUrl(String userJid, String defaultAvatarUrl);

    Map<String, URL> getAvatarUrls(List<String> userJids, String defaultAvatarUrl);
}
