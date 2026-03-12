package ge.dola.talanti.chat;

import ge.dola.talanti.chat.dto.ChatMessageRequest;
import ge.dola.talanti.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // React will send messages to: /app/chat.send
    @MessageMapping("/chat.send")
    @Transactional
    public void sendMessage(@Payload ChatMessageRequest request) {

        // 1. Hardcoded user ID for MVP testing (User 1)
        Long senderId = 1L;

        // 2. Save to database
        ChatMessageResponse savedMessage = chatRepository.saveMessage(
                request.conversationId(),
                senderId,
                request.content()
        );

        // 3. Broadcast to the Room's specific radio frequency
        // E.g., /topic/chat.1
        messagingTemplate.convertAndSend(
                "/topic/chat." + request.conversationId(),
                savedMessage
        );
    }
}