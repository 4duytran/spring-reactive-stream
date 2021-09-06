package fr.abes.findrav2.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceAutoriteDto {

    private String ppn;
    private String firstName;
    private String lastName;

}
