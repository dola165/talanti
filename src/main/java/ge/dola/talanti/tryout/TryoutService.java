package ge.dola.talanti.tryout;

import ge.dola.talanti.notification.NotificationService;
import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.tryout.dto.ApplyToTryoutDto;
import ge.dola.talanti.tryout.dto.TryoutApplicationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TryoutService {

    private final TryoutRepository tryoutRepository;
    private final NotificationService notificationService;

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public TryoutApplicationResponseDto applyToTryout(Long tryoutId, ApplyToTryoutDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        TryoutRepository.TryoutSummary tryout = tryoutRepository.findTryoutSummary(tryoutId)
                .orElseThrow(() -> new IllegalArgumentException("Tryout not found."));

        if (tryoutRepository.hasUserApplied(tryoutId, currentUserId)) {
            throw new IllegalArgumentException("You have already applied to this tryout.");
        }

        String message = dto.message() == null || dto.message().isBlank() ? null : dto.message().trim();
        Long applicationId = tryoutRepository.createApplication(tryoutId, currentUserId, message);
        notificationService.notifyTryoutApplicationReceived(
                tryout.clubId(),
                applicationId,
                tryout.title(),
                currentUserId
        );

        return new TryoutApplicationResponseDto(applicationId, "PENDING");
    }
}
