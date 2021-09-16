package fr.abes.findrav2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import fr.abes.findrav2.config.PropertiesLoader;
import fr.abes.findrav2.domain.dto.ReferenceAutoriteDto;
import fr.abes.findrav2.domain.entity.ReferenceAutorite;
import fr.abes.findrav2.domain.utils.MapStructMapper;
import fr.abes.findrav2.domain.utils.StringOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
class FindraV2ApplicationTests {

    @Autowired
    StringOperator stringOperator;

    @Autowired
    WebClient.Builder webClientBuilder;

    @Autowired
    MapStructMapper mapStructMapper;


    @Test
    void contextLoads() {
    }

    @Test
    void initMapWithName() {

        Map<Integer, String> initMap = new LinkedHashMap<>();
        IntStream.range(1, 10).forEach( x -> initMap.put(x, null));


        String firstName = "Jean-Luc";
        String lastName = "Robert";

        List<String> fName = stringOperator.processName(firstName);
        List<String> lName = stringOperator.processName(lastName);


        IntStream.range(0,lName.size()).forEach(x -> initMap.put(x+1, lName.get(x) ));

        /**
         * If (Jean-Luc)
         * - 5=Jean
         * - 6=Luc
         * - 7=J
         * If (Jean Luc Patrick)
         * - 5=Jean
         * - 7=J
         * If (Jean)
         * - 5=Jean
         * - 7=J
         * If (J.-L.)
         * - 8=J
         * - 9=L
         * If (J. Luc)
         * If (J.)
         * If (Delarue J.)
         * - 8=J
         */
        /*Predicate<String> filterNameCase5And6And7 = e -> e.matches("^[a-z]+\\-[a-z]+.$");
        Predicate<String> filterNameCase8And9 = e -> e.matches("^[a-z]{1}.\\-[a-z]{1}.$");
        Predicate<String> filterNameCase5And7 = e -> e.matches("^[a-z ]+");
        Predicate<String> filterNameCase8 = e -> e.matches("^[a-z]+[ ][a-z]{1}.");
        Predicate<String> filterNameCase8Bis = e -> e.matches("^[a-z]{1}.[ ][a-z]+");*/


        if (firstName.toLowerCase().matches("^[\\p{L}]+\\-[\\p{L}]+.$")) {
                initMap.put(5, fName.get(0) );
                initMap.put(6, fName.get(1) );
                initMap.put(7, fName.get(0).substring(0, 1) );

        }

        if (firstName.toLowerCase().matches("^[\\p{L} ]+|[\\p{L}]+$")) {
            System.out.println( "Matched");
            System.out.println(fName.get(0));
                initMap.put(5, fName.get(0) );
                initMap.put(7, fName.get(0).substring(0, 1) );
        }

        if (firstName.toLowerCase().matches("^[\\p{L}]{1}.\\-[\\p{L}]{1}.$")) {
                initMap.put(8, fName.get(0).substring(0, 1) );
                initMap.put(9, fName.get(2).substring(0, 1) );

        }

        if (firstName.toLowerCase().matches("^[\\p{L}]+[ ][\\p{L}]{1}.|[\\p{L}]{1}.[ ][\\p{L}]+")) {
            fName.stream()
                    .filter(x -> x.matches("^[\\p{L}]{1}"))
                    .findFirst()
                    .ifPresent(x -> initMap.put(8, x.substring(0, 1)));
        }


        initMap.forEach( (k, v) -> System.out.printf("key = %s , value = %s%n", k, v));



    }

    @Test
    void testRegex() {

        System.out.println(("l.-l.").matches("^[a-z]{1}.\\-[a-z]{1}.$"));
        System.out.println(("jean-luc").matches("^[a-z]+\\-[a-z]+.$"));
        System.out.println(("jean luc patrick").matches("^[a-z ]+"));
        System.out.println(("jean l.").matches("^[a-z]+[ ][a-z]{1}."));
    }

    @Test
    void getListFromRequets() throws InterruptedException {

        WebClient webClient = webClientBuilder.baseUrl("http://solrtotal.v102.abes.fr:8081").build();
        ObjectMapper mapper = new ObjectMapper();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        List<String> requests = new ArrayList<>(
                List.of(
                        "(AA900.A900Sa:robert AND AA900.A900Sa:valerie) AND NOT(A008_AS:Tp8)",
                        "(AA901.A901Sa:robert AND AA901.A901Sa:valerie) AND NOT(A008_AS:Tp8)",
                        "(AA902.A902Sa:robert AND AA902.A902Sa:valerie) AND NOT(A008_AS:Tp8)",
                        "(AA901.A901Sa:robert AND AA901.A901Sa:valerie) AND NOT(A008_AS:Tp8)"
                )
        );

        //String requestSolr = "(AA900.A900Sa:robert AND AA900.A900Sa:valerie) AND NOT(A008_AS:Tp8)";

        List<ReferenceAutoriteDto> referenceAutoriteList = Flux.fromIterable(requests)
                .parallel().runOn(Schedulers.boundedElastic())
                .flatMap(x -> webClient.get().uri(builder -> builder
                                .path("/solr/sudoc/select")
                                .queryParam("q", "{requestSolr}")
                                .queryParam("start", "0")
                                .queryParam("rows", "3000")
                                .queryParam("fl", "id,ppn_z,A200.A200Sa_AS,A200.A200Sb_AS")
                                .queryParam("wt", "json")
                                .build(x)
                        ).accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
                        .bodyToMono(JsonNode.class)
                        .log()
                        .doOnError(e -> System.out.println( "ERROR => " + e.getMessage() ))
                        .onErrorResume(e -> Mono.empty())
                        .map(jsonNode -> jsonNode.findValue("docs"))
                        .map(v -> {
                            ObjectReader reader = mapper.readerFor(new TypeReference<List<ReferenceAutorite>>() {
                            });
                            try {
                                return reader.<List<ReferenceAutorite>>readValue(v);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return new ArrayList<ReferenceAutorite>();
                            }
                        })
                        .flatMapMany(Flux::fromIterable))
                .flatMap(v -> {
                    return Mono.just(mapStructMapper.referenceAutoriteToreferenceAutoriteDto(v));
                })
                .sequential()
                .distinct(ReferenceAutoriteDto::getPpn)
                .collectList().block();

        assert referenceAutoriteList != null;
        referenceAutoriteList.forEach(System.out::println);
    }

    @Test
    void loadPropertieFileAndReplaceWithName() {

        // Init HashMap avec 9 cases 1 - 9
        Map<Integer, String> initMap = new LinkedHashMap<>();
        IntStream.range(1, 10).forEach( x -> initMap.put(x, null));

        String firstName = "Deluca J.";
        String lastName = "Valérie";

        // Traitement les noms
        List<String> fName = stringOperator.processName(firstName);
        List<String> lName = stringOperator.processName(lastName);

        // Ajouter le Nom dans les 1 - 4 cases
        IntStream.range(0,lName.size()).forEach(x -> initMap.put(x+1, lName.get(x) ));

        /**
         * If (Jean-Luc)
         * - 5=Jean
         * - 6=Luc
         * - 7=J
         * If (Jean Luc Patrick)
         * - 5=Jean
         * - 7=J
         * If (Jean)
         * - 5=Jean
         * - 7=J
         * If (Delarue J.)
         * - 8=J
         * If (J.-L.)
         * - 8=J
         * - 9=L
         * If (J. Luc)
         * - 8=J
         */

        // Init les cases 5,6,7 avec le prénom
        if (firstName.toLowerCase().matches("^[a-z]+\\-[a-z]+.$")) {
            initMap.put(5, fName.get(0) );
            initMap.put(6, fName.get(1) );
            initMap.put(7, fName.get(0).substring(0, 1) );

        }

        // Init les cases 5,7 avec le prénom
        if (firstName.toLowerCase().matches("^[a-z ]+|[a-z]+")) {
            initMap.put(5, fName.get(0) );
            initMap.put(7, fName.get(0).substring(0, 1) );
        }

        // Init les cases 8,9 avec le prénom
        if (firstName.toLowerCase().matches("^[a-z]{1}.\\-[a-z]{1}.$")) {
            initMap.put(8, fName.get(0).substring(0, 1) );
            initMap.put(9, fName.get(2).substring(0, 1) );

        }

        // Init les cases 8 avec le prénom
        if (firstName.toLowerCase().matches("^[a-z]+[ ][a-z]{1}.|[a-z]{1}.[ ][a-z]+")) {
            // on cherche a savoir la positon de la letrre avec le .
            // (J. Luc) ou (Delarue J.)
            fName.stream()
                    .filter(x -> x.matches("^[a-z]{1}"))
                    .findFirst()
                    .ifPresent(x -> initMap.put(8, x.substring(0, 1)));
        }

        System.out.println("La Map avec le nom et le prénom");
        initMap.forEach( (k,v) -> System.out.printf(" Key = %s :  value = %s%n",k,v));


        List<String> allSolrRequest = new ArrayList<>();

        // Load le fichier propertie
        PropertiesLoader propertiesLoader = new PropertiesLoader("default");

        // Ajouter tous les valeurs dans ce fichier propertie dans une liste pour les traitements
        propertiesLoader.getConfigProp().forEach((key, value) -> {
            allSolrRequest.add(value.toString());
        });


        // Ajouter seulement les clés avec une valeur non null dans initmap
        Map<String, String> mapWithName = initMap.entrySet().stream().filter(v -> v.getValue() != null)
                .collect(Collectors.toMap( k -> "${"+k.getKey()+"}", Map.Entry::getValue));


        System.out.println("Map avec le nom et le prénom déja traité");
        mapWithName.forEach((key, value) -> System.out.println(key + " : " + value));


        // Ajouter une liste avec seulement les clés non null
        List<String> requestNumbersMatch = initMap.entrySet().stream().filter(v -> v.getValue() != null)
                .map(Map.Entry::getKey)
                .map(x -> "${"+x+"}")
                .collect(Collectors.toList());

        System.out.println("la liste contient seulement les cles non null");
        requestNumbersMatch.forEach(System.out::println);


        // Ajouter une liste avec seulement les clés null
        List<String> requestNumbersNotMatch = initMap.entrySet().stream().filter(v -> v.getValue() == null)
                .map(Map.Entry::getKey)
                .map(x -> "${"+x+"}")
                .collect(Collectors.toList());

        System.out.println("la liste contient seulement les cles null");
        requestNumbersNotMatch.forEach(System.out::println);


        // Check tous les valeurs dans la liste obtenir dans la fichier propertie
        System.out.println("La liste du fichier propertie contient les éléments qui n'ont pas les clés null");
        allSolrRequest.stream()
                .filter(v -> requestNumbersNotMatch.stream().noneMatch(v::contains))
                .forEach(System.out::println);


        // Check tous les éléments dans la liste qu'on obtient dans le fichier propertie
        // on prends seulement les valeurs qui ne contiennent pas les clés avec les valeurs null dans le HashMap initMap
        // Ensuite, on injecte les noms et les prénoms dans chaque élément ( String ) de la liste avec les memes numéros de la clé dans initMap
        System.out.println("La liste du fichier propertie contient les éléments qui n'ont pas les clés null et on injecte les noms et les prénom");
        allSolrRequest.stream()
                .filter(v -> requestNumbersNotMatch.stream().noneMatch(v::contains))
                .map(x -> {
                    return mapWithName.entrySet()
                            .stream()
                            .reduce(x,(s, e) -> s.replace( e.getKey(), e.getValue() ),(s1, s2) -> null);
                })
                .forEach(System.out::println);


    }

}
