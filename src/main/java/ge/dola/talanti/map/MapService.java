package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public List<MapMarkerDto> getNearbyEntities(Double lat, Double lng, Double radiusKm, String type,
                                                List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries) {

        if ("CLUB".equalsIgnoreCase(type)) {
            return mapRepository.findNearbyClubs(lat, lng, radiusKm, cities, countries);
        } else if ("TRYOUT".equalsIgnoreCase(type)) {
            return mapRepository.findNearbyTryouts(lat, lng, radiusKm, gender, ageGroups, cities, countries);
        } else if ("MATCH".equalsIgnoreCase(type) || "FRIENDLY".equalsIgnoreCase(type)) {
            return mapRepository.findNearbyMatches(lat, lng, radiusKm, gender, ageGroups, cities, countries);
        }

        return List.of();
    }
}