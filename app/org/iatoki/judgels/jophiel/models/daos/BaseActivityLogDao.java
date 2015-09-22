package org.iatoki.judgels.jophiel.models.daos;

import org.iatoki.judgels.jophiel.models.entities.AbstractActivityLogModel;
import org.iatoki.judgels.play.models.daos.Dao;

public interface BaseActivityLogDao<M extends AbstractActivityLogModel> extends Dao<Long, M> {

    M createActivityLogModel();
}
