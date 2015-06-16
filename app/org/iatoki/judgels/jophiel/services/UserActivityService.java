package org.iatoki.judgels.jophiel.services;

import org.iatoki.judgels.jophiel.UserActivity;

import java.util.List;

public interface UserActivityService {

    void addUserActivity(UserActivity userActivity) throws InterruptedException;

    void addUserActivities(List<UserActivity> userActivities) throws InterruptedException;

    List<UserActivity> getUserActivities() throws InterruptedException;
}
