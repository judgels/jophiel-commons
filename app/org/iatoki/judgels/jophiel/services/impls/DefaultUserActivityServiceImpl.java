package org.iatoki.judgels.jophiel.services.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserActivity;
import org.iatoki.judgels.jophiel.services.UserActivityService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DefaultUserActivityServiceImpl implements UserActivityService {

    private static DefaultUserActivityServiceImpl INSTANCE;
    private final Jophiel jophiel;
    private List<UserActivity> userActivities;

    private DefaultUserActivityServiceImpl(Jophiel jophiel) {
        this.jophiel = jophiel;
        this.userActivities = Lists.newArrayList();
    }

    @Override
    public void addUserActivity(UserActivity userActivity) throws InterruptedException{
        if (jophiel.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            userActivities.add(userActivity);
            jophiel.getActivityLock().unlock();
        } else {
            throw new InterruptedException();
        }
    }

    @Override
    public void addUserActivities(List<UserActivity> userActivities) throws InterruptedException {
        if (jophiel.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            this.userActivities.addAll(userActivities);
            jophiel.getActivityLock().unlock();
        } else {
            throw new InterruptedException();
        }
    }

    @Override
    public List<UserActivity> getUserActivities() throws InterruptedException {
        if (jophiel.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            List<UserActivity> activityLogs = Lists.newArrayList(this.userActivities);
            this.userActivities.clear();
            jophiel.getActivityLock().unlock();

            return activityLogs;
        } else {
            throw new InterruptedException();
        }
    }

    public static synchronized void buildInstance(Jophiel jophiel) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("DefaultUserActivityServiceImpl instance has already been built");
        }
        INSTANCE = new DefaultUserActivityServiceImpl(jophiel);
    }

    public static DefaultUserActivityServiceImpl getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("DefaultUserActivityServiceImpl instance has not been built");
        }
        return INSTANCE;
    }

}
