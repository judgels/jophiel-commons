package org.iatoki.judgels.jophiel.models.daos.hibernate;

import org.iatoki.judgels.jophiel.models.daos.BaseActivityLogDao;
import org.iatoki.judgels.jophiel.models.entities.AbstractActivityLogModel;
import org.iatoki.judgels.play.models.daos.impls.AbstractHibernateDao;

public abstract class AbstractActivityLogHibernateDao<M extends AbstractActivityLogModel> extends AbstractHibernateDao<Long, M> implements BaseActivityLogDao<M> {

    protected AbstractActivityLogHibernateDao(Class<M> modelClass) {
        super(modelClass);
    }
}
