package ge.dola.talanti.club.mapper;

import ge.dola.talanti.club.dto.ClubDto;
import ge.dola.talanti.club.dto.CreateClubDto;
import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClubMapper {

    // Map JOOQ record to Output DTO
    ClubDto toDto(ClubsRecord record);

    // Map incoming Create DTO to JOOQ record (ignoring DB-generated fields)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true) // Handled in Service layer
    @Mapping(target = "createdAt", ignore = true)
    ClubsRecord toRecord(CreateClubDto dto);

    // Update existing record with new data
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateRecordFromDto(CreateClubDto dto, @MappingTarget ClubsRecord record);
}