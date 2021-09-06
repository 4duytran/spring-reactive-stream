package fr.abes.findrav2.domain.dto;

import lombok.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceAutoriteGetDto {

    private int ppnCounter;
    private List<ReferenceAutoriteDto> referenceAutorite;
}
