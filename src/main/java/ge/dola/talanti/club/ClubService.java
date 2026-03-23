package ge.dola.talanti.club;

import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.club.dto.*;
import ge.dola.talanti.club.event.CalendarEventCreatedEvent;
import ge.dola.talanti.club.event.ClubFollowedEvent;
import ge.dola.talanti.club.mapper.ClubMapper;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import ge.dola.talanti.notification.NotificationService;
import ge.dola.talanti.schedule.ScheduleService;
import ge.dola.talanti.schedule.dto.ScheduleItemDto;
import ge.dola.talanti.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMembershipRepository clubMembershipRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClubMapper clubMapper;
    private final DSLContext dsl;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ClubDto createClub(CreateClubDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User ID: {} is creating a new club: {}", safe(currentUserId), safe(dto.name()));

        if (clubMembershipRepository.isUserInAnyClub(currentUserId)) {
            throw new IllegalArgumentException("You are already a member of a club.");
        }
        if (clubRepository.existsByNormalizedName(dto.name())) {
            throw new IllegalArgumentException("A club with this name already exists.");
        }

        ClubContactDetails contactDetails = validateClubContactDetails(dto);

        ClubsRecord record = clubMapper.toRecord(dto);
        record.setCreatedBy(currentUserId);
        record.setCreatedAt(java.time.LocalDateTime.now());
        record.setStatus("NEW_CLUB");

        ClubsRecord savedClub = clubRepository.save(
                record,
                contactDetails.contactEmail(),
                contactDetails.whatsappNumber(),
                contactDetails.facebookMessengerUrl(),
                contactDetails.preferredCommunicationMethod()
        );

        clubMembershipRepository.createMembership(savedClub.getId(), currentUserId, ClubRole.OWNER);

        return clubMapper.toDto(savedClub);
    }

    @Transactional(readOnly = true)
    public List<ClubProfileDto> getAllClubs() {
        Long currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        return clubProfileRepository.getAllClubs(currentUserId);
    }

    @Transactional(readOnly = true)
    public ClubProfileDto getClubProfile(Long clubId) {
        Long currentUserId = SecurityUtils.getCurrentUser().map(u -> u.getUserId()).orElse(null);
        log.debug("User ID: {} fetching profile for Club ID: {}", safe(currentUserId), safe(clubId));
        return clubProfileRepository.getClubProfile(clubId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));
    }

    @Transactional(readOnly = true)
    public List<ClubRosterDto> getClubRoster(Long clubId) {
        return clubProfileRepository.getClubRoster(clubId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public List<ClubStaffDto> getClubStaff(Long clubId) {
        return clubProfileRepository.getClubStaff(clubId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public List<CalendarEventDto> getInternalClubSchedule(Long clubId) {
        return clubProfileRepository.getClubSchedule(clubId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleItemDto> getClubCalendar(Long clubId) {
        return scheduleService.getClubCalendar(clubId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Optional<MyClubResponseDto> getMyPrimaryClub() {
        return clubProfileRepository.getMyPrimaryClub(SecurityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ClubMembershipContextDto getMyMembershipContext() {
        return clubMembershipRepository.findMembershipContext(SecurityUtils.getCurrentUserId())
                .orElse(new ClubMembershipContextDto(false, true, null, null, null));
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void updateClubImages(Long clubId, ClubUpdateDto updateDto) {
        log.info("User ID: {} updating images for Club ID: {}", safe(SecurityUtils.getCurrentUserId()), safe(clubId));
        clubRepository.updateClubImages(clubId, updateDto);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void createCalendarEvent(Long clubId, CalendarRequestDto request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User ID: {} creating calendar event for Club ID: {}", safe(currentUserId), safe(clubId));
        clubProfileRepository.createCalendarEvent(clubId, currentUserId, request);
        eventPublisher.publishEvent(new CalendarEventCreatedEvent(clubId, request.type(), request.title()));
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void deleteCalendarEvent(Long clubId, String eventId) {
        log.info("User ID: {} deleting event {} from Club ID: {}", safe(SecurityUtils.getCurrentUserId()), safe(eventId), safe(clubId));
        clubProfileRepository.deleteCalendarEvent(clubId, eventId);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void createChallenge(Long targetClubId, CreateChallengeDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (dto.targetClubId() != null && !dto.targetClubId().equals(targetClubId)) {
            throw new IllegalArgumentException("Target club ID must match the request path.");
        }
        if (!dsl.fetchExists(
                dsl.selectOne()
                        .from(ge.dola.talanti.jooq.Tables.CLUBS)
                        .where(ge.dola.talanti.jooq.Tables.CLUBS.ID.eq(targetClubId))
        )) {
            throw new ResourceNotFoundException("Target club not found.");
        }

        Long sourceClubId = clubProfileRepository.getMyPrimaryClub(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("You must manage a club to issue a challenge."))
                .clubId();

        if (sourceClubId.equals(targetClubId)) {
            log.warn("User ID: {} attempted self-challenge for Club ID: {}", safe(currentUserId), safe(sourceClubId));
            throw new IllegalArgumentException("You cannot challenge your own club.");
        }

        log.info("Club ID: {} challenging Club ID: {}", safe(sourceClubId), safe(targetClubId));
        Long matchId = clubProfileRepository.issueMatchChallenge(sourceClubId, targetClubId, dto);
        String sourceClubName = clubRepository.findById(sourceClubId)
                .map(ClubsRecord::getName)
                .orElse("Another club");
        notificationService.notifyClubChallengeReceived(
                targetClubId,
                matchId,
                sourceClubName,
                dto.matchType() != null ? dto.matchType() : "FRIENDLY"
        );
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public boolean toggleClubFollow(Long clubId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!dsl.fetchExists(
                dsl.selectOne()
                        .from(ge.dola.talanti.jooq.Tables.CLUBS)
                        .where(ge.dola.talanti.jooq.Tables.CLUBS.ID.eq(clubId))
        )) {
            throw new ResourceNotFoundException("Club not found.");
        }

        boolean isFollowing = clubRepository.isUserFollowingClub(currentUserId, clubId);

        if (isFollowing) {
            log.info("User ID: {} unfollowing Club ID: {}", safe(currentUserId), safe(clubId));
            clubRepository.unfollowClub(currentUserId, clubId);
            return false;
        } else {
            log.info("User ID: {} following Club ID: {}", safe(currentUserId), safe(clubId));
            clubRepository.followClub(currentUserId, clubId);
            eventPublisher.publishEvent(new ClubFollowedEvent(currentUserId, clubId));
            return true;
        }
    }

    private ClubContactDetails validateClubContactDetails(CreateClubDto dto) {
        String contactEmail = normalizeNullable(dto.contactEmail());
        String whatsappNumber = normalizeWhatsapp(dto.whatsappNumber());
        String facebookMessengerUrl = normalizeMessengerUrl(dto.facebookMessengerUrl());

        if (whatsappNumber == null && facebookMessengerUrl == null) {
            throw new IllegalArgumentException("Add WhatsApp or Facebook/Messenger contact info to create a club.");
        }

        ClubCommunicationMethod preferredMethod = dto.preferredCommunicationMethod();
        if (preferredMethod == ClubCommunicationMethod.WHATSAPP && whatsappNumber == null) {
            throw new IllegalArgumentException("Preferred communication method must match an available contact option.");
        }
        if (preferredMethod == ClubCommunicationMethod.FACEBOOK_MESSENGER && facebookMessengerUrl == null) {
            throw new IllegalArgumentException("Preferred communication method must match an available contact option.");
        }

        return new ClubContactDetails(
                contactEmail,
                whatsappNumber,
                facebookMessengerUrl,
                preferredMethod.name()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeWhatsapp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replaceAll("[\\s()\\-]", "");
        if (!Pattern.matches("\\+?\\d{7,15}", normalized)) {
            throw new IllegalArgumentException("WhatsApp number must contain a valid international phone number.");
        }

        return normalized.startsWith("+") ? normalized : "+" + normalized;
    }

    private String normalizeMessengerUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (!Pattern.matches("(?i)^https?://(www\\.)?(facebook\\.com|m\\.me)/.+$", normalized)) {
            throw new IllegalArgumentException("Facebook/Messenger contact must be a valid facebook.com or m.me URL.");
        }

        return normalized;
    }

    private record ClubContactDetails(
            String contactEmail,
            String whatsappNumber,
            String facebookMessengerUrl,
            String preferredCommunicationMethod
    ) {
    }
}
