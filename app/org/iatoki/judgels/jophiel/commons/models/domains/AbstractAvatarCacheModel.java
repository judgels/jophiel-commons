package org.iatoki.judgels.jophiel.commons.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractModel;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractAvatarCacheModel extends AbstractModel {
    @Id
    @GeneratedValue
    public long id;

    public String userJid;

    public String avatarUrl;
}
