package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.*;
import ge.dola.talanti.club.event.CalendarEventCreatedEvent;
import ge.dola.talanti.club.event.ClubFollowedEvent;
import ge.dola.talanti.club.mapper.ClubMapper;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
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

import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ClubMapper clubMapper;
    private final DSLContext dsl;

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ClubDto createClub(CreateClubDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User ID: {} is creating a new club: {}", safe(currentUserId), safe(dto.name()));

        ClubsRecord record = clubMapper.toRecord(dto);
        record.setCreatedBy(currentUserId);
        record.setCreatedAt(java.time.LocalDateTime.now());
        record.setStatus("UNVERIFIED");

        ClubsRecord savedClub = clubRepository.save(record);

        dsl.insertInto(ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS)
                .set(ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS.CLUB_ID, savedClub.getId())
                .set(ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS.USER_ID, currentUserId)
                .set(ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS.ROLE, ClubRole.OWNER.name())
                .set(ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS.JOINED_AT, java.time.LocalDateTime.now())
                .execute();

        return clubMapper.toDto(savedClub);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ClubProfileDto> getAllClubs() {
        return clubProfileRepository.getAllClubs(SecurityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ClubProfileDto getClubProfile(Long clubId) {
        log.debug("User ID: {} fetching profile for Club ID: {}", safe(SecurityUtils.getCurrentUserId()), safe(clubId));
        return clubProfileRepository.getClubProfile(clubId, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ClubRosterDto> getClubRoster(Long clubId) {
        return clubProfileRepository.getClubRoster(clubId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ClubStaffDto> getClubStaff(Long clubId) {
        return clubProfileRepository.getClubStaff(clubId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public List<CalendarEventDto> getInternalClubSchedule(Long clubId) {
        return clubProfileRepository.getClubSchedule(clubId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Optional<MyClubResponseDto> getMyPrimaryClub() {
        return clubProfileRepository.getMyPrimaryClub(SecurityUtils.getCurrentUserId());
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

        Long sourceClubId = clubProfileRepository.getMyPrimaryClub(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("You must manage a club to issue a challenge."))
                .clubId();

        if (sourceClubId.equals(targetClubId)) {
            log.warn("User ID: {} attempted self-challenge for Club ID: {}", safe(currentUserId), safe(sourceClubId));
            throw new IllegalArgumentException("You cannot challenge your own club.");
        }

        log.info("Club ID: {} challenging Club ID: {}", safe(sourceClubId), safe(targetClubId));
        clubProfileRepository.issueMatchChallenge(sourceClubId, targetClubId, dto);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public boolean toggleClubFollow(Long clubId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
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
}