package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.club.dto.ClubRosterDto;
import ge.dola.talanti.club.dto.ClubStaffDto;
import ge.dola.talanti.club.dto.MyClubResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubProfileRepository clubProfileRepository;
    // 🟢 REMOVED: PostRepository (FeedService handles this now)

    // Used for the UI display
    public ClubProfileDto getClubProfile(Long clubId, Long currentUserId) {
        return clubProfileRepository.getClubProfile(clubId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
    }

    public Optional<MyClubResponseDto> getMyPrimaryClub(Long userId) {
        return clubProfileRepository.getMyPrimaryClub(userId);
    }

    // Used for backend logic / updates
    public void updateCalendarEvent(Long clubId, String eventId, ge.dola.talanti.club.dto.CalendarRequestDto request) {
        clubProfileRepository.updateCalendarEvent(clubId, eventId, request);
    }

    public void deleteCalendarEvent(Long clubId, String eventId) {
        clubProfileRepository.deleteCalendarEvent(clubId, eventId);
    }

    @Transactional
    public void updateClubImages(Long clubId, ge.dola.talanti.club.dto.ClubUpdateDto updateDto) {
        clubRepository.updateClubImages(clubId, updateDto);
    }

    /**
     * Toggles the follow status. Returns true if the user is now following, false if unfollowed.
     */
    @Transactional
    public boolean toggleClubFollow(Long clubId, Long userId) {
        // 1. Ensure the club actually exists before trying to follow it
        boolean clubExists = clubRepository.findById(clubId).isPresent();
        if (!clubExists) {
            throw new RuntimeException("Club not found with id: " + clubId);
        }

        // 2. Check current status and toggle
        boolean isCurrentlyFollowing = clubRepository.isUserFollowingClub(userId, clubId);

        if (isCurrentlyFollowing) {
            clubRepository.unfollowClub(userId, clubId);
            return false; // They are no longer following
        } else {
            clubRepository.followClub(userId, clubId);
            return true;  // They are now following
        }
    }

    @Transactional(readOnly = true)
    public List<ClubProfileDto> getAllClubs(Long currentUserId) {
        return clubProfileRepository.getAllClubs(currentUserId);
    }

    public List<ClubRosterDto> getClubRoster(Long clubId) {
        return clubProfileRepository.getClubRoster(clubId);
    }

    public List<ClubStaffDto> getClubStaff(Long clubId) {
        return clubProfileRepository.getClubStaff(clubId);
    }

    public java.util.List<ge.dola.talanti.club.dto.CalendarEventDto> getClubSchedule(Long clubId) {
        return clubProfileRepository.getClubSchedule(clubId);
    }

    public void createCalendarEvent(Long clubId, Long userId, ge.dola.talanti.club.dto.CalendarRequestDto request) {
        clubProfileRepository.createCalendarEvent(clubId, userId, request);
    }

    // 🟢 REMOVED: getClubFeed() is now handled correctly by FeedService directly in the Controller
}