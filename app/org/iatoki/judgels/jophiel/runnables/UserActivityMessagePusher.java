package org.iatoki.judgels.jophiel.runnables;

import org.iatoki.judgels.api.JudgelsAPIClientException;
import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielUserActivityMessage;
import org.iatoki.judgels.jophiel.UserActivityMessage;
import org.iatoki.judgels.jophiel.services.UserActivityMessageService;
import play.db.jpa.JPA;

import java.util.List;
import java.util.stream.Collectors;

public final class UserActivityMessagePusher implements Runnable {

    private final JophielClientAPI jophielClientAPI;
    private final UserActivityMessageService userActivityMessageService;

    public UserActivityMessagePusher(JophielClientAPI jophielClientAPI, UserActivityMessageService userActivityMessageService) {
        this.jophielClientAPI = jophielClientAPI;
        this.userActivityMessageService = userActivityMessageService;
    }

    @Override
    public void run() {
        JPA.withTransaction(() -> {
                try {
                    List<UserActivityMessage> messages = userActivityMessageService.getUserActivityMessages();
                    List<JophielUserActivityMessage> jophielMessages = messages.stream()
                            .map(m -> new JophielUserActivityMessage(m.getTime(), m.getUserJid(), m.getLog(), m.getIpAddress()))
                            .collect(Collectors.toList());

                    try {
                        jophielClientAPI.sendUserActivityMessages(jophielMessages);
                    } catch (JudgelsAPIClientException e) {
                        userActivityMessageService.addUserActivityMessages(messages);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        );
    }
}
