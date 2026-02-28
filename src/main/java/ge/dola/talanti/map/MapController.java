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

    /**
     * Endpoint to drop a location pin for a club or user.
     * POST /api/map/location
     */
    @PostMapping("/location")
    public ResponseEntity<Void> addLocation(@RequestBody SaveLocationDto dto) {
        mapService.addLocation(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * The main endpoint for the React map to fetch pins.
     * GET /api/map/nearby?lat=41.7151&lng=44.8271&radius=10.0&type=CLUB
     * (Default coordinates are Tbilisi city center)
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<MapMarkerDto>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "15.0") Double radius, // Default 15km radius
            @RequestParam(defaultValue = "CLUB") String type) {

        List<MapMarkerDto> markers = mapService.getNearbyEntities(lat, lng, radius, type);
        return ResponseEntity.ok(markers);
    }
}