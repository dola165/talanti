package ge.dola.talanti.squad;

import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.notification.NotificationService;
import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.squad.dto.AddSquadPlayerDto;
import ge.dola.talanti.squad.dto.CreateSquadDto;
import ge.dola.talanti.squad.dto.SquadRosterGroupDto;
import ge.dola.talanti.squad.dto.SquadRosterPlayerDto;
import ge.dola.talanti.squad.dto.SquadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;
    private final NotificationService notificationService;

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    @CacheEvict(cacheNames = "club-squads", key = "#clubId")
    public SquadDto createSquad(Long clubId, CreateSquadDto dto) {
        log.info("Admin ID [{}] creating squad [{}] for Club ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(dto.name()), safe(clubId));

        if (squadRepository.existsByNormalizedName(clubId, dto.name())) {
            throw new IllegalArgumentException("A squad with this name already exists for this club.");
        }

        Long squadId = squadRepository.createSquad(clubId, dto);
        return new SquadDto(squadId, clubId, dto.name(), dto.category(), dto.gender(), null);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "club-squads", key = "#clubId")
    public List<SquadDto> getSquadsForClub(Long clubId) {
        return squadRepository.getSquadsForClub(clubId);
    }

    @Transactional(readOnly = true)
    public List<SquadRosterGroupDto> getSquadRoster(Long clubId, Long squadId) {
        requireSquadBelongsToClub(clubId, squadId);

        Map<String, List<SquadRosterPlayerDto>> grouped = squadRepository.getSquadRoster(clubId, squadId)
                .stream()
                .sorted(
                        Comparator.comparing((SquadRosterPlayerDto player) -> positionGroupSortOrder(player.position()))
                                .thenComparing(player -> player.number(), Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(player -> player.name() == null ? "" : player.name(), String.CASE_INSENSITIVE_ORDER)
                )
                .collect(Collectors.groupingBy(
                        player -> positionGroupLabel(player.position()),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        return List.of("Goalkeepers", "Defenders", "Midfielders", "Forwards")
                .stream()
                .filter(grouped::containsKey)
                .map(label -> new SquadRosterGroupDto(label, grouped.get(label)))
                .toList();
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void addPlayerToSquad(Long clubId, Long squadId, AddSquadPlayerDto dto) {
        verifySquadBelongsToClub(clubId, squadId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Admin ID [{}] adding User ID [{}] to Squad ID [{}]", safe(currentUserId), safe(dto.userId()), safe(squadId));
        squadRepository.addPlayerToSquad(squadId, dto);
        if (!dto.userId().equals(currentUserId)) {
            notificationService.notifySquadAssignment(dto.userId(), clubId, squadId);
        }
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

    private void requireSquadBelongsToClub(Long clubId, Long squadId) {
        if (!squadRepository.doesSquadBelongToClub(clubId, squadId)) {
            throw new ResourceNotFoundException("Squad not found for club.");
        }
    }

    private static int positionGroupSortOrder(String position) {
        return switch (positionGroupLabel(position)) {
            case "Goalkeepers" -> 0;
            case "Defenders" -> 1;
            case "Midfielders" -> 2;
            default -> 3;
        };
    }

    private static String positionGroupLabel(String position) {
        String normalized = position == null ? "" : position.trim().toUpperCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return "Forwards";
        }
        if (normalized.contains("GK") || normalized.contains("GOALKEEP")) {
            return "Goalkeepers";
        }
        if (normalized.contains("DEF") || normalized.contains("BACK") || normalized.contains("FULLBACK") || normalized.contains("WINGBACK") || normalized.contains("CENTER BACK") || normalized.contains("CENTRE BACK")) {
            return "Defenders";
        }
        if (normalized.contains("MID") || normalized.equals("DM") || normalized.equals("CM") || normalized.equals("AM")) {
            return "Midfielders";
        }

        return "Forwards";
    }
}
