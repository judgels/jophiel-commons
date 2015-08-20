package org.iatoki.judgels.jophiel.models.entities;

import org.iatoki.judgels.play.models.entities.AbstractModel;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractUserModel extends AbstractModel {

    @Id
    @GeneratedValue
    public long id;

    public String userJid;

    @Column(columnDefinition = "TEXT")
    public String accessToken;

    @Column(columnDefinition = "TEXT")
    public String refreshToken;

    @Column(columnDefinition = "TEXT")
    public String idToken;

    public long expirationTime;
}
