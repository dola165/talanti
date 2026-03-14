package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @PostMapping("/location")
    public ResponseEntity<Void> addLocation(@RequestBody SaveLocationDto dto) {
        mapService.addLocation(dto);
        return ResponseEntity.ok().build();
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