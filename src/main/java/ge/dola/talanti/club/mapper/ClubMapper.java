package ge.dola.talanti.club.mapper;

import ge.dola.talanti.club.dto.ClubDto;
import ge.dola.talanti.club.dto.CreateClubDto;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ClubMapper {

    public ClubDto toDto(ClubsRecord record) {
        if (record == null) return null;

        return new ClubDto(
                record.getId(),
                record.getName(),
                record.getDescription(),
                record.getLocationId(),
                record.getType(),
                "VERIFIED".equals(record.getStatus()),
                record.getCreatedAt()
        );
    }

    public ClubsRecord toRecord(CreateClubDto dto) {
        if (dto == null) return null;

        ClubsRecord record = new ClubsRecord();
        record.setName(dto.name().trim().replaceAll("\\s+", " "));
        record.setDescription(dto.description() == null || dto.description().isBlank() ? null : dto.description().trim());
        record.setLocationId(dto.locationId());
        record.setType(dto.type().trim().toUpperCase(Locale.ROOT));

        return record;
    }
}
