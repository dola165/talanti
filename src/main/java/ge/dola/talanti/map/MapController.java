package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @PostMapping("/location")
    public ResponseEntity<Map<String, Long>> addLocation(@RequestBody SaveLocationDto dto) {
        // STRICT ENFORCEMENT: Return the ID so the frontend can link it to Clubs/Matches!
        Long locationId = mapService.addLocation(dto);
        return ResponseEntity.ok(Map.of("locationId", locationId));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<MapMarkerDto>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "15.0") Double radius,
            @RequestParam(defaultValue = "CLUB") String type,
            @RequestParam(required = false) List<String> gender,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> countries
    ) {
        return ResponseEntity.ok(mapService.getNearbyEntities(lat, lng, radius, type, gender, ageGroups, cities, countries));
    }
}