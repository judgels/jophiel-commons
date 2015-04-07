package org.iatoki.judgels.jophiel.commons;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DefaultUserActivityServiceImpl implements UserActivityService {

    private static final DefaultUserActivityServiceImpl INSTANCE = new DefaultUserActivityServiceImpl();
    private List<UserActivity> userActivities;

    private DefaultUserActivityServiceImpl() {
        userActivities = Lists.newArrayList();
    }

    @Override
    public void addUserActivity(UserActivity userActivity) throws InterruptedException{
        if (JophielUtils.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            userActivities.add(userActivity);
            JophielUtils.getActivityLock().unlock();
        } else {
            throw new InterruptedException();
        }
    }

    @Override
    public void addUserActivities(List<UserActivity> userActivities) throws InterruptedException {
        if (JophielUtils.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            this.userActivities.addAll(userActivities);
            JophielUtils.getActivityLock().unlock();
        } else {
            throw new InterruptedException();
        }
    }

    @Override
    public List<UserActivity> getUserActivities() throws InterruptedException {
        if (JophielUtils.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            List<UserActivity> activityLogs = Lists.newArrayList(this.userActivities);
            this.userActivities.clear();
            JophielUtils.getActivityLock().unlock();

            return activityLogs;
        } else {
            throw new InterruptedException();
        }
    }

    public static DefaultUserActivityServiceImpl getInstance() {
        return INSTANCE;
    }

}
