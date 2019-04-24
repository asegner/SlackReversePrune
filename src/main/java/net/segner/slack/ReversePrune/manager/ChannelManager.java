package net.segner.slack.ReversePrune.manager;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChannelManager {

    private SessionManager sessionManager;

    /**
     * Defines channels that will always be excluded in either mode
     */
    @Value("${channels_excluded}")
    private String channels_excluded;
    private List<String> channels_excluded_list;

    /**
     * Defines channels that will always be included in either mode
     */
    @Value("${channels_included}")
    private String channels_included;
    private List<String> channels_included_list;

    @Getter
    private Collection<SlackChannel> unFilteredChannels = null, filteredChannels, nonMemberChannels;

    public ChannelManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @PostConstruct
    public void postConstruct() {
        channels_excluded_list = CollectionUtils.arrayToList(channels_excluded.split(","));
        channels_included_list = CollectionUtils.arrayToList(channels_included.split(","));
    }

    /**
     * Invites a SlackUser list to the channels
     */
    public void inviteToChannels(List<SlackUser> channelusers) throws IOException {
        if (unFilteredChannels == null) {
            log.error("No channels available for invite");
            return;
        }
        SlackSession session = sessionManager.getSession();

        filteredChannels.forEach(channel -> channelusers.forEach(channeluser -> {
            boolean tempJoin = false;
            if (nonMemberChannels.contains(channel)) {
                tempJoin = true;
                session.joinChannel(channel.getName());
            }
            log.info("Inviting " + channeluser.getUserName() + " to " + channel.getName());
            session.inviteToChannel(channel, channeluser);
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            if (tempJoin) {
                session.leaveChannel(channel);
            }
        }));
    }

    public void filterChannels(Collection<SlackChannel> channels, PRUNE_MODE mode) {
        unFilteredChannels = channels;
        filteredChannels = new ArrayList<>(unFilteredChannels);
        filteredChannels.removeIf(SlackChannel::isArchived);

        nonMemberChannels = new ArrayList<>(unFilteredChannels);
        nonMemberChannels.removeIf(SlackChannel::isArchived);
        nonMemberChannels.removeIf(SlackChannel::isMember);

        switch (mode) {
            case INCLUDED:
                filteredChannels.removeIf(channel ->
                        !channels_included_list.contains(channel.getName()));
        }
        filteredChannels.removeIf(channel ->
                !channel.getType().equals(SlackChannel.SlackChannelType.PUBLIC_CHANNEL)
                        || channels_excluded_list.contains(channel.getName()));
    }

}
