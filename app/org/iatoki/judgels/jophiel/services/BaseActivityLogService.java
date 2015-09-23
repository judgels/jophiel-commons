package org.iatoki.judgels.jophiel.services;

import org.iatoki.judgels.jophiel.ActivityKey;

public interface BaseActivityLogService {

    void addActivityLog(ActivityKey activityKey, String username, long time, String userJid, String userIpAddress);
}
