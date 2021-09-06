package fr.abes.findrav2.domain.utils;

import fr.abes.findrav2.domain.dto.ReferenceAutoriteDto;
import fr.abes.findrav2.domain.entity.ReferenceAutorite;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring"
)
public interface MapStructMapper {

    ReferenceAutoriteDto referenceAutoriteToreferenceAutoriteDto (ReferenceAutorite referenceAutorite);

}
