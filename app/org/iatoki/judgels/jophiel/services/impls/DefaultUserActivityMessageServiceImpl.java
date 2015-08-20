package org.iatoki.judgels.jophiel.services.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserActivityMessage;
import org.iatoki.judgels.jophiel.services.UserActivityMessageService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DefaultUserActivityMessageServiceImpl implements UserActivityMessageService {

    private static DefaultUserActivityMessageServiceImpl INSTANCE;
    private final Jophiel jophiel;
    private List<UserActivityMessage> userActivities;

    private DefaultUserActivityMessageServiceImpl(Jophiel jophiel) {
        this.jophiel = jophiel;
        this.userActivities = Lists.newArrayList();
    }

    @Override
    public void addUserActivityMessage(UserActivityMessage userActivityMessage) throws InterruptedException {
        if (!jophiel.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            throw new InterruptedException();
        }

        userActivities.add(userActivityMessage);
        jophiel.getActivityLock().unlock();
    }

    @Override
    public void addUserActivityMessages(List<UserActivityMessage> userActivityMessages) throws InterruptedException {
        if (!jophiel.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            throw new InterruptedException();
        }

        this.userActivities.addAll(userActivityMessages);
        jophiel.getActivityLock().unlock();
    }

    @Override
    public List<UserActivityMessage> getUserActivityMessages() throws InterruptedException {
        if (!jophiel.getActivityLock().tryLock(10, TimeUnit.SECONDS)) {
            throw new InterruptedException();
        }

        List<UserActivityMessage> activityLogs = Lists.newArrayList(this.userActivities);
        this.userActivities.clear();
        jophiel.getActivityLock().unlock();

        return activityLogs;
    }

    public static synchronized void buildInstance(Jophiel jophiel) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("DefaultUserActivityMessageServiceImpl instance has already been built");
        }
        INSTANCE = new DefaultUserActivityMessageServiceImpl(jophiel);
    }

    public static DefaultUserActivityMessageServiceImpl getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("DefaultUserActivityMessageServiceImpl instance has not been built");
        }
        return INSTANCE;
    }

}
