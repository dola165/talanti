package ge.dola.talanti.tryout;

import ge.dola.talanti.tryout.dto.TryoutMapDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tryouts")
@RequiredArgsConstructor
public class TryoutController {

    private final TryoutRepository tryoutRepository;

    @GetMapping("/map")
    public ResponseEntity<List<TryoutMapDto>> getMapTryouts() {
        return ResponseEntity.ok(tryoutRepository.getUpcomingTryoutsForMap());
    }
}