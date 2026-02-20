package ge.dola.talanti.club;

import ge.dola.talanti.club.dto.ClubProfileDto;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

}