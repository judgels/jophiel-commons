package org.iatoki.judgels.jophiel.services;

import org.iatoki.judgels.jophiel.UserTokens;

public interface BaseUserService {

    void upsertUser(String userJid, String accessToken, String idToken, String refreshToken, long expireTime);

    boolean existsByUserJid(String userJid);

    UserTokens getUserTokensByUserJid(String userJid);

}
