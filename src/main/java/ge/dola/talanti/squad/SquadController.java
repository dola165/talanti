package ge.dola.talanti.squad;

import ge.dola.talanti.squad.dto.AddSquadPlayerDto;
import ge.dola.talanti.squad.dto.CreateSquadDto;
import ge.dola.talanti.squad.dto.SquadDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs/{clubId}/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;

    @GetMapping
    public ResponseEntity<List<SquadDto>> getSquads(@PathVariable Long clubId) {
        return ResponseEntity.ok(squadService.getSquadsForClub(clubId));
    }

    @PostMapping
    public ResponseEntity<SquadDto> createSquad(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateSquadDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(squadService.createSquad(clubId, dto));
    }

    @PostMapping("/{squadId}/players")
    public ResponseEntity<Map<String, String>> addPlayer(
            @PathVariable Long clubId,
            @PathVariable Long squadId,
            @Valid @RequestBody AddSquadPlayerDto dto) {
        squadService.addPlayerToSquad(clubId, squadId, dto);
        return ResponseEntity.ok(Map.of("message", "Player added to squad successfully."));
    }

    @DeleteMapping("/{squadId}/players/{userId}")
    public ResponseEntity<Map<String, String>> removePlayer(
            @PathVariable Long clubId,
            @PathVariable Long squadId,
            @PathVariable Long userId) {
        squadService.removePlayerFromSquad(clubId, squadId, userId);
        return ResponseEntity.ok(Map.of("message", "Player removed from squad successfully."));
    }
}