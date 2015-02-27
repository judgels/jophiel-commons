package org.iatoki.judgels.jophiel.commons;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.iatoki.judgels.jophiel.commons.models.daos.interfaces.BaseAvatarCacheDao;
import org.iatoki.judgels.jophiel.commons.models.domains.AbstractAvatarCacheModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class AbstractAvatarCacheService<M extends AbstractAvatarCacheModel> {
    private BaseAvatarCacheDao<M> avatarCacheDao;

    public final void setDao(BaseAvatarCacheDao<M> avatarCacheDao) {
        this.avatarCacheDao = avatarCacheDao;
    }

    public final void putImageUrl(String userJid, URL imageUrl, String user, String ipAddress) {
        if (avatarCacheDao.existsByUserJid(userJid)) {
            editImageUrl(userJid, imageUrl, user, ipAddress);
        } else {
            createImageUrl(userJid, imageUrl, user, ipAddress);
        }
    }

    public final URL getAvatarUrl(String userJid) {
        try {
            if (!avatarCacheDao.existsByUserJid(userJid)) {
                return JophielUtils.getDefaultAvatarUrl();
            } else {
                M jidCacheModel = avatarCacheDao.findByUserJid(userJid);
                return new URL(jidCacheModel.avatarUrl);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public final Map<String, URL> getAvatarUrls(List<String> userJids) {
        try {
            List<M> entries = avatarCacheDao.findByUserJids(userJids);

            Map<String, URL> displayNamesMap = Maps.newHashMap();

            for (M entry : entries) {
                displayNamesMap.put(entry.userJid, new URL(entry.avatarUrl));
            }

            for (String jid : userJids) {
                if (!displayNamesMap.containsKey(jid)) {
                    displayNamesMap.put(jid, JophielUtils.getDefaultAvatarUrl());
                }
            }

            return ImmutableMap.copyOf(displayNamesMap);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createImageUrl(String userJid, URL imageUrl, String user, String ipAddress) {
        M avatarCacheModel = avatarCacheDao.createAvatarCacheModel();

        avatarCacheModel.userJid = userJid;
        avatarCacheModel.avatarUrl = imageUrl.toString();

        avatarCacheDao.persist(avatarCacheModel, user, ipAddress);
    }

    private void editImageUrl(String userJid, URL imageUrl, String user, String ipAddress) {
        M avatarCacheModel = avatarCacheDao.findByUserJid(userJid);

        avatarCacheModel.userJid = userJid;
        avatarCacheModel.avatarUrl = imageUrl.toString();

        avatarCacheDao.edit(avatarCacheModel, user, ipAddress);
    }
}
