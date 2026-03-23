package ge.dola.talanti.notification;

import ge.dola.talanti.club.ClubMembershipRepository;
import ge.dola.talanti.club.ClubRepository;
import ge.dola.talanti.club.ClubRole;
import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.notification.dto.NotificationBulkReadResultDto;
import ge.dola.talanti.notification.dto.NotificationDto;
import ge.dola.talanti.notification.dto.NotificationReadStateDto;
import ge.dola.talanti.notification.dto.NotificationUnreadCountDto;
import ge.dola.talanti.squad.SquadRepository;
import ge.dola.talanti.user.repository.UserRepository;
import ge.dola.talanti.util.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final ClubMembershipRepository clubMembershipRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final SquadRepository squadRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PageResult<NotificationDto> getMyNotifications(Long userId, String scope, Long clubId, int page, int size) {
        ResolvedNotificationFilter filter = resolveFilter(userId, scope, clubId);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 50));
        return notificationRepo.getUserNotifications(userId, filter.scope(), filter.clubId(), normalizedPage, normalizedSize);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public NotificationUnreadCountDto getUnreadCount(Long userId, String scope, Long clubId) {
        ResolvedNotificationFilter filter = resolveFilter(userId, scope, clubId);
        return new NotificationUnreadCountDto(notificationRepo.countUnread(userId, filter.scope(), filter.clubId()));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public NotificationReadStateDto markAsRead(Long notificationId, Long userId) {
        NotificationDto accessibleNotification = notificationRepo.findAccessibleNotification(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        notificationRepo.markAsRead(notificationId, userId);
        return new NotificationReadStateDto(accessibleNotification.id(), true);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public NotificationBulkReadResultDto markAllAsRead(Long userId, String scope, Long clubId) {
        ResolvedNotificationFilter filter = resolveFilter(userId, scope, clubId);
        return new NotificationBulkReadResultDto(notificationRepo.markAllAsRead(userId, filter.scope(), filter.clubId()));
    }

    @Transactional
    public void notifyClubChallengeReceived(Long targetClubId, Long matchId, String sourceClubName, String matchType) {
        List<Long> recipients = clubMembershipRepository.findLeadershipUserIds(targetClubId);
        createNotifications(
                recipients,
                NotificationType.CLUB_CHALLENGE_RECEIVED.name(),
                "match",
                matchId,
                "Challenge received",
                sourceClubName + " challenged your club to a " + sentenceValue(matchType) + " match.",
                NotificationScope.CLUB,
                targetClubId,
                "/clubs/" + targetClubId
        );
    }

    @Transactional
    public void notifyTryoutApplicationReceived(Long clubId, Long applicationId, String tryoutTitle, Long applicantUserId) {
        List<Long> recipients = clubMembershipRepository.findTryoutStaffUserIds(clubId);
        String applicantName = userRepository.findDisplayNameById(applicantUserId).orElse("A player");
        createNotifications(
                recipients,
                NotificationType.TRYOUT_APPLICATION_RECEIVED.name(),
                "tryout_application",
                applicationId,
                "New tryout application",
                applicantName + " applied to " + tryoutTitle + ".",
                NotificationScope.CLUB,
                clubId,
                "/clubs/" + clubId
        );
    }

    @Transactional
    public void notifyTryoutDecision(Long applicationId, Long applicantUserId, Long clubId, String tryoutTitle, String clubName, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String type = switch (normalizedStatus) {
            case "ACCEPTED" -> NotificationType.TRYOUT_APPLICATION_ACCEPTED.name();
            case "REJECTED" -> NotificationType.TRYOUT_APPLICATION_REJECTED.name();
            case "SHORTLISTED" -> NotificationType.TRYOUT_APPLICATION_SHORTLISTED.name();
            default -> null;
        };

        if (type == null) {
            return;
        }

        createNotifications(
                List.of(applicantUserId),
                type,
                "tryout_application",
                applicationId,
                "Tryout update",
                "Your application to " + tryoutTitle + " was " + sentenceValue(normalizedStatus) + " by " + clubName + ".",
                NotificationScope.PERSONAL,
                clubId,
                "/clubs/" + clubId
        );
    }

    @Transactional
    public void notifyClubInvitationReceived(Long inviteeUserId, Long inviteId, Long clubId, ClubRole role) {
        String clubName = clubRepository.findById(clubId)
                .map(record -> record.getName())
                .orElse("A club");
        createNotifications(
                List.of(inviteeUserId),
                NotificationType.CLUB_INVITATION_RECEIVED.name(),
                "club_membership_invite",
                inviteId,
                "Club invitation",
                clubName + " invited you to join as " + humanizeEnumValue(role.name()) + ".",
                NotificationScope.PERSONAL,
                clubId,
                "/clubs/" + clubId
        );
    }

    @Transactional
    public void notifyClubRoleChanged(Long targetUserId, Long clubId, ClubRole newRole) {
        String clubName = clubRepository.findById(clubId)
                .map(record -> record.getName())
                .orElse("Your club");
        createNotifications(
                List.of(targetUserId),
                NotificationType.CLUB_ROLE_CHANGED.name(),
                "club_membership",
                clubId,
                "Club role updated",
                "Your role at " + clubName + " is now " + humanizeEnumValue(newRole.name()) + ".",
                NotificationScope.PERSONAL,
                clubId,
                "/clubs/" + clubId
        );
    }

    @Transactional
    public void notifySquadAssignment(Long targetUserId, Long clubId, Long squadId) {
        SquadRepository.SquadSummary squadSummary = squadRepository.findSquadSummary(clubId, squadId)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found for club."));

        createNotifications(
                List.of(targetUserId),
                NotificationType.SQUAD_ASSIGNMENT.name(),
                "squad",
                squadId,
                "Squad assignment",
                "You were added to " + squadSummary.squadName() + " at " + squadSummary.clubName() + ".",
                NotificationScope.PERSONAL,
                clubId,
                "/clubs/" + clubId + "/squads?squad=" + squadId
        );
    }

    @Transactional
    public void createDirectNotification(Long userId,
                                         String type,
                                         String entityType,
                                         Long entityId,
                                         String title,
                                         String body,
                                         String linkPath) {
        createNotifications(List.of(userId), type, entityType, entityId, title, body, NotificationScope.PERSONAL, null, linkPath);
    }

    @Transactional
    public void createClubAnnouncementNotifications(Long clubId, Long postId, String title, String body, String linkPath) {
        notificationRepo.createClubAnnouncementNotifications(clubId, postId, title, body, linkPath);
    }

    private void createNotifications(Collection<Long> userIds,
                                     String type,
                                     String entityType,
                                     Long entityId,
                                     String title,
                                     String body,
                                     NotificationScope scope,
                                     Long clubId,
                                     String linkPath) {
        notificationRepo.createNotifications(userIds, type, entityType, entityId, title, body, scope, clubId, linkPath);
    }

    private static String humanizeEnumValue(String value) {
        if (value == null || value.isBlank()) {
            return "updated";
        }

        return List.of(value.trim().toLowerCase(Locale.ROOT).split("_"))
                .stream()
                .map(part -> part.isBlank() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(value);
    }

    private static String sentenceValue(String value) {
        return humanizeEnumValue(value).toLowerCase(Locale.ROOT);
    }

    private ResolvedNotificationFilter resolveFilter(Long userId, String scope, Long clubId) {
        NotificationQueryScope normalizedScope = NotificationQueryScope.from(scope);

        if (clubId != null && normalizedScope == NotificationQueryScope.ALL) {
            normalizedScope = NotificationQueryScope.CLUB;
        }

        if (clubId != null && normalizedScope == NotificationQueryScope.PERSONAL) {
            throw new IllegalArgumentException("Club filter is only supported for club notifications.");
        }

        if (clubId != null && !clubMembershipRepository.hasNotificationAccess(clubId, userId)) {
            throw new AccessDeniedException("You do not have access to this club notification context.");
        }

        return new ResolvedNotificationFilter(normalizedScope, clubId);
    }

    private record ResolvedNotificationFilter(NotificationQueryScope scope, Long clubId) {
    }
}
