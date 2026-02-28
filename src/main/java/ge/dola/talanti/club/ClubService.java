package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {


    private final ClubRepository clubRepository;
    private final ClubProfileRepository clubProfileRepository;


    // Used for the UI display
    public ClubProfileDto getClubProfile(Long clubId, Long currentUserId) {
        return clubProfileRepository.getClubProfile(clubId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
    }

    // Used for backend logic / updates
    public void updateClubDescription(Long clubId, String newDescription) {
        ClubsRecord club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        club.setDescription(newDescription);
        clubRepository.save(club);
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

}