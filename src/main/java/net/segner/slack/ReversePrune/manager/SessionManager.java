package net.segner.slack.ReversePrune.manager;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SessionManager {

    @Value("${api_token}")
    private String apiToken;
    @Value("${user_emails}")
    private String usernameList;

    private SlackSession session = null;
    private List<SlackUser> users = null;

    @PostConstruct
    public void postConstruct() {
        session = SlackSessionFactory
                .getSlackSessionBuilder(apiToken)
                .withAutoreconnectOnDisconnection(true)
                .build();
    }

    /**
     * @return SlackSession connected and ready to use
     */
    public SlackSession getSession() throws IOException {
        if (session == null) postConstruct();

        if (!session.isConnected()) {
            session.connect();
            parseUsers();
        }
        return session;
    }

    public void disconnect() {
        if (session != null) {
            try {
                session.disconnect();
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    public void parseUsers() throws IOException {
        Collection<SlackUser> userList = getSession().getUsers();
        userList.removeIf(SlackUser::isDeleted);
        userList.removeIf(SlackUser::isBot);
        userList.removeIf(user -> StringUtils.isEmpty(user.getUserMail()));
        Map<String, SlackUser> allUsers = userList.stream().collect(Collectors.toMap(SlackUser::getUserMail, (SlackUser u) -> u));

        // parse users of interest from user list
        users = new ArrayList<>();
        CollectionUtils.arrayToList(usernameList.split(",")).forEach(slackuser -> {
            users.add(allUsers.get(slackuser));
        });
    }

    /**
     * @return List SlackUser objects for the user emails
     */
    public List<SlackUser> getUsers() throws IOException {
        return users;
    }

    public Collection<SlackChannel> getChannels() {
        return session.getChannels();
    }

}
