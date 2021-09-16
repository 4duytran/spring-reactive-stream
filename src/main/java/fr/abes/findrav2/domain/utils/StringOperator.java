package fr.abes.findrav2.domain.utils;


import fr.abes.findrav2.config.PropertiesLoader;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class StringOperator {

    private String replaceDiacritic(final String str)
    {
        return  str.replace("�?", "A").replace("Æ", "AE").replace("�?", "I").replace("�?", "I").replace("Ĳ", "IJ").replace("�?", "D").replace("Ø", "O").replace("Œ", "OE")
                .replace("œ", "oe").replace("Þ", "TH").replace("�?", "Y").replace("æ", "ae").replace("ĳ", "ij").replace("ð", "d")
                .replace("ø", "o").replace("ß", "ss").replace("þ", "th").replace("ﬀ", "ff").replace("�?", "fi").replace("ﬂ", "fl")
                .replace("ﬃ", "ffi").replace("ﬄ", "ffl").replace("ﬅ", "ft").replace("ﬆ", "st");
    }

    private String deleteApostrophe(final String str) {
        return str.contains("'") ? str.replace("'", "") : str;
    }

    private Map<Integer, String> initMapWithName(String firstName, String lastName) {

        Map<Integer, String> initMap = new LinkedHashMap<>();

        // Init HashMap avec 9 cases et les valeurs sont null par défaut
        IntStream.range(1, 10).forEach( x -> initMap.put(x, null));

        // Traitement le Prénom (firstName) et le Nom (lastName)
        List<String> fName = processName(firstName);
        List<String> lName = processName(lastName);


        // Cases 1 - 4 sont pour le Nom
        IntStream.range(0,lName.size()).forEach(x -> initMap.put(x+1, lName.get(x) ));


        // Cases 5 - 9 sont pour le Prénom
        if (firstName.toLowerCase().matches("^[\\p{L}]+[-][\\p{L}]+")) {
            initMap.put(5, fName.get(0) );
            initMap.put(6, fName.get(1) );
            initMap.put(7, fName.get(0).substring(0, 1) );

        }

        if (firstName.toLowerCase().matches("^[\\p{L} ]{2,}|[\\p{L}]{2,}")) {
            initMap.put(5, fName.get(0) );
            initMap.put(7, fName.get(0).substring(0, 1) );
        }

        if (firstName.toLowerCase().matches("^[a-z].-[a-z].")) {
            initMap.put(8, fName.get(0).substring(0, 1) );
            initMap.put(9, fName.get(2).substring(0, 1) );

        }

        if (firstName.toLowerCase().matches("^[\\p{L}]+[ ][a-z].|[a-z].[ ][\\p{L}]+|[a-z].|[a-z]")) {
            fName.stream()
                    .filter(x -> x.matches("^[a-z].|[a-z]"))
                    .findFirst()
                    .ifPresent(x -> initMap.put(8, x.substring(0, 1)));
        }

        // Print le HashMap avec le nom et le prénom dans la console
        initMap.forEach( (k,v) -> System.out.printf("Key = %s : Value = %s%n", k,v));
        return initMap;
    }

    public List<String> listOfSolrRequestFromPropertieFile(String fileName, String firstName, String lastName) {

        Map<Integer, String> mapWithName = initMapWithName(firstName, lastName);
        // Load le fichier properties
        PropertiesLoader propertiesLoader = new PropertiesLoader(fileName);

        List<String> allSolrRequest = new ArrayList<>();

        // Ajouter toutes les valeurs dans ce fichier properties dans une liste pour les traitements

        propertiesLoader.getConfigProp().forEach((key, value) -> {
            allSolrRequest.add(value.toString());
        });

        // Ajouter seulement les clés avec une valeur NON null dans un nouveau HahMap
        Map<String, String> mapWithNameNotNull = mapWithName.entrySet().stream().filter(v -> v.getValue() != null)
                .collect(Collectors.toMap(k -> "${"+k.getKey()+"}", Map.Entry::getValue, (prev, next) -> next, LinkedHashMap::new));

        // Ajouter une liste avec seulement les clés NON null
        /*List<String> requestNumbersMatch = mapWithName.entrySet().stream().filter(v -> v.getValue() != null)
                .map(Map.Entry::getKey)
                .map(x -> "${"+x+"}")
                .collect(Collectors.toList());*/

        // Ajouter une liste avec seulement les clés null
        List<String> requestNumbersNotMatch = mapWithName.entrySet().stream().filter(v -> v.getValue() == null)
                .map(Map.Entry::getKey)
                .map(x -> "${"+x+"}")
                .collect(Collectors.toList());

        // Check tous les éléments dans la liste que l'on obtient dans le fichier properties
        // on prend seulement les valeurs qui ne contiennent pas les clés qui sont avec les valeurs null dans le HashMap mapWithName
        // Ensuite, on injecte les noms et les prénoms dans chaque élément (String) de la liste avec les memes numéros de la clé dans initMap

        return allSolrRequest.stream()
                .filter(v -> requestNumbersNotMatch.stream().noneMatch(v::contains))
                .map(x -> mapWithNameNotNull.entrySet()
                        .stream()
                        .reduce(x,(s, e) -> s.replace( e.getKey(), e.getValue() ),(s1, s2) ->  null))
                .collect(Collectors.toList());

    }

    public List<String> processName(String name) {

        String str = deleteApostrophe(replaceDiacritic(name.toLowerCase()));
        String nameRegex = "-|\\.| ";
        return new ArrayList<>(Arrays.asList(str.split(nameRegex)));

    }

}
