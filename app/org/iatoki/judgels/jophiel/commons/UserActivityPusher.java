package org.iatoki.judgels.jophiel.commons;

import com.google.common.collect.Lists;
import play.db.jpa.JPA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserActivityPusher implements Runnable {

    private final BaseUserService userService;
    private final UserActivityService userActivityService;

    public UserActivityPusher(BaseUserService userService, UserActivityService userActivityService) {
        this.userService = userService;
        this.userActivityService = userActivityService;
    }

    @Override
    public void run() {
        JPA.withTransaction(() -> {
            try {
                List<UserActivity> userActivities = userActivityService.getUserActivities();
                Map<String, List<UserActivity>> activityLogMap = new HashMap<>();

                for (UserActivity activityLog : userActivities) {
                    if (activityLogMap.containsKey(activityLog.getUserJid())) {
                        activityLogMap.get(activityLog.getUserJid()).add(activityLog);
                    } else {
                        activityLogMap.put(activityLog.getUserJid(), Lists.newArrayList(activityLog));
                    }
                }

                for (String userJid : activityLogMap.keySet()) {
                    // TODO check if access token is valid, if not should use refresh token
                    String accessToken = userService.getUserTokensByUserJid(userJid).getAccessToken();
                    if ((accessToken != null) && (!JophielUtils.sendUserActivities(accessToken, activityLogMap.get(userJid)))) {
                        userActivityService.addUserActivities(activityLogMap.get(userJid));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
