package ge.dola.talanti.map;

import ge.dola.talanti.map.dto.MapMarkerDto;
import ge.dola.talanti.map.dto.SaveLocationDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    @Cacheable(cacheNames = "map-nearby", keyGenerator = "mapNearbyKeyGenerator")
    public List<MapMarkerDto> getNearbyEntities(Double lat, Double lng, Double radiusKm, String type,
                                                List<String> gender, List<String> ageGroups, List<String> cities, List<String> countries,
                                                String query) {
        String normalizedType = normalizeType(type);
        List<String> normalizedGender = normalizeFilters(gender);
        List<String> normalizedAgeGroups = normalizeFilters(ageGroups);
        List<String> normalizedCities = normalizeFilters(cities);
        List<String> normalizedCountries = normalizeFilters(countries);
        String normalizedQuery = normalizeQuery(query);

        if ("CLUB".equals(normalizedType)) {
            return mapRepository.findNearbyClubs(lat, lng, radiusKm, normalizedCities, normalizedCountries, normalizedQuery);
        } else if ("TRYOUT".equals(normalizedType)) {
            return mapRepository.findNearbyTryouts(lat, lng, radiusKm, normalizedGender, normalizedAgeGroups, normalizedCities, normalizedCountries, normalizedQuery);
        } else if ("MATCH".equals(normalizedType) || "FRIENDLY".equals(normalizedType)) {
            return mapRepository.findNearbyMatches(
                    lat,
                    lng,
                    radiusKm,
                    normalizedGender,
                    normalizedAgeGroups,
                    normalizedCities,
                    normalizedCountries,
                    normalizedQuery,
                    normalizedType
            );
        }
        throw new IllegalArgumentException("Unsupported map entity type.");
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "CLUB";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeFilters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
