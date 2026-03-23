package ge.dola.talanti.chat;

import ge.dola.talanti.chat.dto.ChatMessageRequest;
import ge.dola.talanti.chat.dto.ChatMessageResponse;
import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    public void sendMessage(@Payload ChatMessageRequest request, Authentication authentication) {
        if (request == null || request.conversationId() == null || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Conversation and content are required.");
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AccessDeniedException("Authentication required for chat messaging.");
        }

        Long senderId = user.getUserId();
        if (!chatRepository.isConversationParticipant(request.conversationId(), senderId)) {
            throw new AccessDeniedException("You are not a participant in this conversation.");
        }

        ChatMessageResponse savedMessage = chatRepository.saveMessage(
                request.conversationId(),
                senderId,
                request.content()
        );

        messagingTemplate.convertAndSend(
                "/topic/chat." + request.conversationId(),
                savedMessage
        );
    }
}
