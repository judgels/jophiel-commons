package org.iatoki.judgels.jophiel.services;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface BaseAvatarCacheService {

    void putImageUrl(String userJid, URL imageUrl, String user, String ipAddress);

    URL getAvatarUrl(String userJid);

    Map<String, URL> getAvatarUrls(List<String> userJids);
}
