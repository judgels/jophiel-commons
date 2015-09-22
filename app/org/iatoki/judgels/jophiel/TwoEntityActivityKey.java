package org.iatoki.judgels.jophiel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public abstract class TwoEntityActivityKey implements ActivityKey {

    private String entity;

    private String entityJid;

    private String entityName;

    private String refEntity;

    private String refEntityJid;

    private String refEntityName;

    public final ActivityKey construct(String entity, String entityJid, String entityName, String refEntity, String refEntityJid, String refEntityName) {
        this.entity = entity;
        this.entityJid = entityJid;
        this.entityName = entityName;
        this.refEntity = refEntity;
        this.refEntityJid = refEntityJid;
        this.refEntityName = refEntityName;

        return this;
    }

    public String getEntity() {
        return entity;
    }

    public String getEntityJid() {
        return entityJid;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getRefEntity() {
        return refEntity;
    }

    public String getRefEntityJid() {
        return refEntityJid;
    }

    public String getRefEntityName() {
        return refEntityName;
    }

    @Override
    public String toJsonString() {
        return new Gson().toJson(this, TwoEntityActivityKey.class);
    }

    @Override
    public ActivityKey fromJson(String json) {
        Map<String, String> fields = new Gson().fromJson(json, new TypeToken<Map<String, String>>() { }.getType());

        return this.construct(fields.get("entity"), fields.get("entityJid"), fields.get("entityName"), fields.get("refEntity"), fields.get("refEntityJid"), fields.get("refEntityName"));
    }
}
