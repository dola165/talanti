package ge.dola.talanti.tryout;

import ge.dola.talanti.notification.NotificationService;
import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.tryout.dto.TryoutApplicantDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TryoutAdminService {

    private final TryoutRepository tryoutRepository;
    private final NotificationService notificationService;

    public TryoutAdminService(TryoutRepository tryoutRepository, NotificationService notificationService) {
        this.tryoutRepository = tryoutRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@clubTryoutAccessManager.decide(principal, #clubId)")
    public List<TryoutApplicantDto> getApplicationsForClub(Long clubId) {
        return tryoutRepository.getApplicationsForClub(clubId);
    }

    @Transactional
    @PreAuthorize("@clubTryoutAccessManager.decide(principal, #clubId)")
    public void updateApplicationStatus(Long clubId, Long applicationId, String status) {
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        TryoutRepository.TryoutApplicationContext application = tryoutRepository.findApplicationContext(clubId, applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found or does not belong to this club."));

        int updatedRows = tryoutRepository.updateApplicationStatus(
                clubId,
                applicationId,
                normalizedStatus,
                SecurityUtils.getCurrentUserId(),
                LocalDateTime.now()
        );

        if (updatedRows == 0) {
            throw new IllegalArgumentException("Application not found or does not belong to this club.");
        }

        if (!normalizedStatus.equals(application.status())) {
            notificationService.notifyTryoutDecision(
                    application.applicationId(),
                    application.applicantUserId(),
                    application.clubId(),
                    application.tryoutTitle(),
                    application.clubName(),
                    normalizedStatus
            );
        }
    }
}
