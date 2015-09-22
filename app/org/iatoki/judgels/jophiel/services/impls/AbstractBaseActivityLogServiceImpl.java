package org.iatoki.judgels.jophiel.services.impls;

import org.iatoki.judgels.jophiel.ActivityKey;
import org.iatoki.judgels.jophiel.models.daos.BaseActivityLogDao;
import org.iatoki.judgels.jophiel.models.entities.AbstractActivityLogModel;
import org.iatoki.judgels.jophiel.services.BaseActivityLogService;

public abstract class AbstractBaseActivityLogServiceImpl<M extends AbstractActivityLogModel> implements BaseActivityLogService {

    private final BaseActivityLogDao<M> activityLogDao;

    public AbstractBaseActivityLogServiceImpl(BaseActivityLogDao<M> activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    @Override
    public void addActivityLog(ActivityKey activityKey, String username, String userJid, String userIpAddress) {
        M activityLogModel = activityLogDao.createActivityLogModel();

        activityLogModel.username = username;
        activityLogModel.keyAction = activityKey.getKeyAction();
        activityLogModel.parameters = activityKey.toJsonString();

        activityLogDao.persist(activityLogModel, userJid, userIpAddress);
    }
}
