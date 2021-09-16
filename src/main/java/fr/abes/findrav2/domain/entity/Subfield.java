package fr.abes.findrav2.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@Getter
@NoArgsConstructor
public class Subfield {

    @XmlValue
    private String subfield;

    @XmlAttribute(name="code")
    private String code;

}
