package org.iatoki.judgels.jophiel;

public interface ActivityKey {

    ActivityKey fromJson(String json);

    String getKeyAction();

    String toJsonString();
}
