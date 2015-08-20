package org.iatoki.judgels.jophiel.runnables;

import com.google.common.collect.Lists;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserActivityMessage;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.jophiel.services.UserActivityMessageService;
import play.db.jpa.JPA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserActivityMessagePusher implements Runnable {

    private final Jophiel jophiel;
    private final BaseUserService userService;
    private final UserActivityMessageService userActivityMessageService;

    public UserActivityMessagePusher(Jophiel jophiel, BaseUserService userService, UserActivityMessageService userActivityMessageService) {
        this.jophiel = jophiel;
        this.userService = userService;
        this.userActivityMessageService = userActivityMessageService;
    }

    @Override
    public void run() {
        JPA.withTransaction(() -> {
                try {
                    List<UserActivityMessage> userActivities = userActivityMessageService.getUserActivityMessages();
                    Map<String, List<UserActivityMessage>> activityLogMap = new HashMap<>();

                    for (UserActivityMessage activityLog : userActivities) {
                        if (activityLogMap.containsKey(activityLog.getUserJid())) {
                            activityLogMap.get(activityLog.getUserJid()).add(activityLog);
                        } else {
                            activityLogMap.put(activityLog.getUserJid(), Lists.newArrayList(activityLog));
                        }
                    }

                    for (String userJid : activityLogMap.keySet()) {
                        // TODO check if access token is valid, if not should use refresh token
                        String accessToken = userService.getUserTokensByUserJid(userJid).getAccessToken();
                        if ((accessToken != null) && !jophiel.sendUserActivityMessages(accessToken, activityLogMap.get(userJid))) {
                            userActivityMessageService.addUserActivityMessages(activityLogMap.get(userJid));
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
    }
}
