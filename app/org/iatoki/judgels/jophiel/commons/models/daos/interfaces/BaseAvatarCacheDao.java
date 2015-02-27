package org.iatoki.judgels.jophiel.commons.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.Dao;
import org.iatoki.judgels.jophiel.commons.models.domains.AbstractAvatarCacheModel;

import java.util.List;

public interface BaseAvatarCacheDao<M extends AbstractAvatarCacheModel> extends Dao<String, M> {

    M createAvatarCacheModel();

    boolean existsByUserJid(String userJid);

    M findByUserJid(String userJid);

    List<M> findByUserJids(List<String> userJids);
}
