package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @PostMapping("/location")
    public ResponseEntity<Map<String, Long>> addLocation(@Valid @RequestBody SaveLocationDto dto) {
        // STRICT ENFORCEMENT: Return the ID so the frontend can link it to Clubs/Matches!
        Long locationId = mapService.addLocation(dto);
        return ResponseEntity.ok(Map.of("locationId", locationId));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<MapMarkerDto>> getNearby(
            @RequestParam @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90") @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90") Double lat,
            @RequestParam @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180") @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180") Double lng,
            @RequestParam(defaultValue = "15.0") @Positive(message = "Radius must be greater than zero") @DecimalMax(value = "250.0", message = "Radius cannot exceed 250km") Double radius,
            @RequestParam(defaultValue = "CLUB") @Pattern(regexp = "(?i)CLUB|TRYOUT|MATCH|FRIENDLY", message = "Type must be CLUB, TRYOUT, MATCH, or FRIENDLY") String type,
            @RequestParam(required = false) List<String> gender,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> countries,
            @RequestParam(required = false) @Size(max = 100, message = "Query cannot exceed 100 characters") String query
    ) {
        return ResponseEntity.ok(mapService.getNearbyEntities(lat, lng, radius, type, gender, ageGroups, cities, countries, query));
    }
}
