package org.iatoki.judgels.jophiel.models.daos.jedishibernate;

import org.iatoki.judgels.jophiel.models.daos.BaseActivityLogDao;
import org.iatoki.judgels.jophiel.models.entities.AbstractActivityLogModel;
import org.iatoki.judgels.play.models.daos.impls.AbstractJedisHibernateDao;
import redis.clients.jedis.JedisPool;

public abstract class AbstractActivityLogJedisHibernateDao<M extends AbstractActivityLogModel> extends AbstractJedisHibernateDao<Long, M> implements BaseActivityLogDao<M> {

    protected AbstractActivityLogJedisHibernateDao(JedisPool jedisPool, Class<M> modelClass) {
        super(jedisPool, modelClass);
    }
}
