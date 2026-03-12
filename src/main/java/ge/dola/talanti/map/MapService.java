package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MapService {

    private final MapRepository mapRepository;

    public MapService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    public void addLocation(SaveLocationDto dto) {
        mapRepository.saveLocation(dto);
    }

    public List<MapMarkerDto> getNearbyEntities(Double lat, Double lng, Double radiusKm, String type) {
        if ("CLUB".equalsIgnoreCase(type)) {
            return mapRepository.findNearbyClubs(lat, lng, radiusKm);
        } else if ("TRYOUT".equalsIgnoreCase(type)) {
            return mapRepository.findNearbyTryouts(lat, lng, radiusKm);
        } else if ("MATCH_REQUEST".equalsIgnoreCase(type)) {
            // NEW: Route Matches to the Map!
            return mapRepository.findNearbyMatches(lat, lng, radiusKm);
        }

        return List.of();
    }


}