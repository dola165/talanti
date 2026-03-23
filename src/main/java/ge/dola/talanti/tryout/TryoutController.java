package ge.dola.talanti.tryout;

import ge.dola.talanti.tryout.dto.ApplyToTryoutDto;
import ge.dola.talanti.tryout.dto.TryoutApplicationResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/tryouts")
@RequiredArgsConstructor
public class TryoutController {

    private final TryoutService tryoutService;

    @PostMapping("/{tryoutId}/apply")
    public ResponseEntity<TryoutApplicationResponseDto> applyToTryout(
            @PathVariable @Positive Long tryoutId,
            @Valid @RequestBody ApplyToTryoutDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tryoutService.applyToTryout(tryoutId, dto));
    }
}
