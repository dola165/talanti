package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MapService {

    private final MapRepository mapRepository;

    public MapService(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    @Transactional
    @PreAuthorize("isAuthenticated()") // STRICT ENFORCEMENT: Stop bot location spam
    public Long addLocation(SaveLocationDto dto) {
        return mapRepository.saveLocation(dto);
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