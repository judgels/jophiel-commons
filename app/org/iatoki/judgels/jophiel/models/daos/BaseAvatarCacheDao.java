package org.iatoki.judgels.jophiel.models.daos;

import org.iatoki.judgels.play.models.daos.interfaces.Dao;
import org.iatoki.judgels.jophiel.models.entities.AbstractAvatarCacheModel;

import java.util.List;

public interface BaseAvatarCacheDao<M extends AbstractAvatarCacheModel> extends Dao<String, M> {

    M createAvatarCacheModel();

    boolean existsByUserJid(String userJid);

    M findByUserJid(String userJid);

    List<M> findByUserJids(List<String> userJids);
}
