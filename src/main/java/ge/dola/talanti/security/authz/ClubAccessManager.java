package ge.dola.talanti.security.authz;

import ge.dola.talanti.club.ClubMembershipRepository;
import ge.dola.talanti.club.ClubRole;
import ge.dola.talanti.security.CustomUserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("clubAccessManager")
public class ClubAccessManager implements DomainAccessManager<Long> {

    private final ClubMembershipRepository clubMembershipRepository;

    public ClubAccessManager(ClubMembershipRepository clubMembershipRepository) {
        this.clubMembershipRepository = clubMembershipRepository;
    }

    @Override
    public boolean decide(CustomUserDetails user, Long clubId) {
        return clubMembershipRepository.hasAnyRole(
                clubId,
                user.getUserId(),
                List.of(ClubRole.OWNER, ClubRole.CLUB_ADMIN)
        );
    }
}
