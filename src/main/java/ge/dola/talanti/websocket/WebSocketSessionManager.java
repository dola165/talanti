package ge.dola.talanti.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {
    private final Set<String> activeUsernames = new HashSet<>();
    private final SimpMessagingTemplate messagingTemplate;

    public void addUsername(String username) {
        activeUsernames.add(username);
        broadcastActiveUsernames();
    }

    public void removeUsername(String username) {
        activeUsernames.remove(username);
        broadcastActiveUsernames();
    }

    public void broadcastActiveUsernames() {
        messagingTemplate.convertAndSend("/topic/users", activeUsernames);
        System.out.println("Broadcasting active users to /topic/users: " + activeUsernames);
    }
}