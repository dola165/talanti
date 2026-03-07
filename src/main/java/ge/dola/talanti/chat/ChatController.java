package ge.dola.talanti.chat;

import ge.dola.talanti.dto.ChatDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    @Transactional
    public void sendMessage(@Payload ChatDTOs.ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {

        // 1. In a real secure app, you'd get the user ID from the JWT principal in the headerAccessor.
        // For your ngrok MVP bypass, we'll hardcode the react_dev ID (assuming ID 1 or whatever it is in your DB).
        // Let's assume senderId = 1L for now.
        Long senderId = 1L;

        // 2. Save the message to the database
        ChatDTOs.ChatMessageResponse savedMessage = chatRepository.saveMessage(
                request.conversationId(),
                senderId,
                request.content()
        );

        // 3. Find out who is actually in this conversation
        List<Long> participantIds = chatRepository.getParticipantIds(request.conversationId());

        // 4. Route the message ONLY to those participants
        for (Long participantId : participantIds) {
            // They will subscribe to: /topic/user.{participantId}.chat
            messagingTemplate.convertAndSend(
                    "/topic/user." + participantId + ".chat",
                    savedMessage
            );
        }
    }
}
