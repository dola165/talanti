package ge.dola.talanti.club.mapper;

import ge.dola.talanti.club.dto.ClubDto;
import ge.dola.talanti.club.dto.CreateClubDto;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import org.springframework.stereotype.Component;

@Component
public class ClubMapper {

    // Reads from DB: Translate String "VERIFIED" to boolean true
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

    // Writes to DB: Translate boolean true to String "VERIFIED"
    public ClubsRecord toRecord(CreateClubDto dto) {
        if (dto == null) return null;

        ClubsRecord record = new ClubsRecord();
        record.setName(dto.name());
        record.setDescription(dto.description());
        record.setLocationId(dto.locationId());
        record.setType(dto.type());
        record.setStatus(dto.isOfficial() != null && dto.isOfficial() ? "VERIFIED" : "UNVERIFIED");

        return record;
    }
}