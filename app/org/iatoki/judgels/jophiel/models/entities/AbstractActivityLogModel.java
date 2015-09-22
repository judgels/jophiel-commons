package org.iatoki.judgels.jophiel.models.entities;

import org.iatoki.judgels.play.models.entities.AbstractModel;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractActivityLogModel extends AbstractModel {

    @Id
    @GeneratedValue
    public long id;

    public String username;

    public String keyAction;

    public String parameters;
}
