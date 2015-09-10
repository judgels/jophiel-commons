package org.iatoki.judgels.jophiel.controllers.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielUser;
import org.iatoki.judgels.jophiel.apis.JophielUserAPIIdentity;
import org.iatoki.judgels.play.apis.JudgelsAPIUnauthorizedException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;

public abstract class AbstractJophielClientAPIController extends AbstractJudgelsAPIController {

    protected static JophielUserAPIIdentity authenticateAsJophielUser(JophielClientAPI jophielClientAPI) {
        if (!request().hasHeader("Authorization")) {
            throw new JudgelsAPIUnauthorizedException("Basic/OAuth2 authentication required.");
        }

        String[] authorization = request().getHeader("Authorization").split(" ");

        if (authorization.length != 2) {
            throw new JudgelsAPIUnauthorizedException("Basic/OAuth2 authentication required.");
        }

        String method = authorization[0];
        String credentialsString = authorization[1];

        if ("Basic".equals(method)) {
            String decodedCredentialsString = new String(Base64.decodeBase64(credentialsString));
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(decodedCredentialsString);

            String username = credentials.getUserName();
            String password = credentials.getPassword();

            JophielUser jophielUser = jophielClientAPI.findUserByUsernameAndPassword(username, password);
            return new JophielUserAPIIdentity(jophielUser.getJid(), jophielUser.getUsername());
        } else if ("Bearer".equals(method)) {
            String accessToken = new String(Base64.decodeBase64(credentialsString));

            JophielUser jophielUser = jophielClientAPI.findUserByAccessToken(accessToken);
            return new JophielUserAPIIdentity(jophielUser.getJid(), jophielUser.getUsername());
        } else {
            throw new JudgelsAPIUnauthorizedException("Basic/OAuth2 authentication required.");
        }
    }
}
