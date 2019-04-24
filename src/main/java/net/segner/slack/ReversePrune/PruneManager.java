package net.segner.slack.ReversePrune;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackChannelLeft;
import com.ullink.slack.simpleslackapi.listeners.SlackChannelLeftListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.segner.slack.ReversePrune.manager.ChannelManager;
import net.segner.slack.ReversePrune.manager.PRUNE_MODE;
import net.segner.slack.ReversePrune.manager.SessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class PruneManager {
    private final SessionManager sessionManager;
    private final ChannelManager channelManager;

    /**
     * INCLUDED add selected users to listed channels only minus the excluded channels
     * STANDARD add selected users to all public channels minus the excluded channels (channels_include is ignored)
     */
    @Value("${mode}")
    private PRUNE_MODE mode;


    public PruneManager(SessionManager sessionManager, ChannelManager channelManager) {
        this.sessionManager = sessionManager;
        this.channelManager = channelManager;
    }

    void run() {
        try {
            performAction();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            sessionManager.disconnect();
        }
    }


    private void performAction() throws IOException {
        sessionManager.getSession();
        List<SlackUser> usersOfInterest = sessionManager.getUsers();
        channelManager.filterChannels(sessionManager.getChannels(), mode);
        channelManager.inviteToChannels(usersOfInterest);
    }

}
