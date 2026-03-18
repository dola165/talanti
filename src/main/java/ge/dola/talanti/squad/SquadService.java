package ge.dola.talanti.squad;

import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.squad.dto.AddSquadPlayerDto;
import ge.dola.talanti.squad.dto.CreateSquadDto;
import ge.dola.talanti.squad.dto.SquadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public SquadDto createSquad(Long clubId, CreateSquadDto dto) {
        log.info("Admin ID [{}] creating squad [{}] for Club ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(dto.name()), safe(clubId));

        Long squadId = squadRepository.createSquad(clubId, dto);
        return new SquadDto(squadId, clubId, dto.name(), dto.category(), dto.gender(), null);
    }

    @Transactional(readOnly = true)
    public List<SquadDto> getSquadsForClub(Long clubId) {
        return squadRepository.getSquadsForClub(clubId);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void addPlayerToSquad(Long clubId, Long squadId, AddSquadPlayerDto dto) {
        verifySquadBelongsToClub(clubId, squadId);

        log.info("Admin ID [{}] adding User ID [{}] to Squad ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(dto.userId()), safe(squadId));
        squadRepository.addPlayerToSquad(squadId, dto);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void removePlayerFromSquad(Long clubId, Long squadId, Long userId) {
        verifySquadBelongsToClub(clubId, squadId);

        log.info("Admin ID [{}] removing User ID [{}] from Squad ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(userId), safe(squadId));
        squadRepository.removePlayerFromSquad(squadId, userId);
    }

    // STRICT ENFORCEMENT: The Anti-Hijack Guard
    private void verifySquadBelongsToClub(Long clubId, Long squadId) {
        if (!squadRepository.doesSquadBelongToClub(clubId, squadId)) {
            log.warn("SECURITY ALERT: Admin of Club [{}] attempted to modify external Squad [{}]", safe(clubId), safe(squadId));
            throw new IllegalArgumentException("Squad does not belong to the specified club.");
        }
    }
}