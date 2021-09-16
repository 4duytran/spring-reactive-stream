package fr.abes.findrav2.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@NoArgsConstructor
@Getter
public class Datafield {

    @XmlAttribute(name="tag")
    private String tag;

    @XmlElement(name="subfield")
    private List<Subfield> subfieldList;
}
