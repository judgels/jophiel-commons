package org.iatoki.judgels.jophiel;

import java.net.URL;

public final class PublicUser {

    private String jid;
    private String username;
    private String name;
    private URL profilePictureUrl;

    public PublicUser(String jid, String username, URL profilePictureUrl) {
        this.jid = jid;
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
    }

    public PublicUser(String jid, String username, String name, URL profilePictureUrl) {
        this.jid = jid;
        this.username = username;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getJid() {
        return jid;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public URL getProfilePictureUrl() {
        return profilePictureUrl;
    }
}
