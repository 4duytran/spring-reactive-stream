package fr.abes.findrav2.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import fr.abes.findrav2.domain.dto.ReferenceAutoriteDto;
import fr.abes.findrav2.domain.dto.ReferenceAutoriteGetDto;
import fr.abes.findrav2.domain.entity.ReferenceAutorite;
import fr.abes.findrav2.domain.entity.XmlRootRecord;
import fr.abes.findrav2.domain.utils.LuceneSearch;
import fr.abes.findrav2.domain.utils.MapStructMapper;
import fr.abes.findrav2.domain.utils.StringOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReferenceContextuelService {


    private final WebClient.Builder webClientBuilder;
    private final MapStructMapper mapStructMapper;
    private final StringOperator stringOperator;


    @Value("${solr.base-url}")
    private String solrBaseUrl;

    // This method returns filter function which will log request data
    // Using this for DEBUG mod
    private static ExchangeFilterFunction logRequestWebclient() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    public Mono<ReferenceAutoriteGetDto> findAllRC(String fileName, String firstName, String lastName) {

        List<String> requests = stringOperator.listOfSolrRequestFromPropertieFile(fileName, firstName, lastName);
        WebClient webClientRA = webClientBuilder.baseUrl(solrBaseUrl)
                //.filter(logRequestWebclient()) // <== Use this to see WebClient requests
                .build();

        ObjectMapper mapper = new ObjectMapper();

        List<ReferenceAutoriteDto> referenceAutoriteDtoList = new ArrayList<>();
        ReferenceAutoriteGetDto referenceAutoriteGetDto = new ReferenceAutoriteGetDto();
        AtomicInteger ppnCount = new AtomicInteger();

        return Flux.fromIterable(requests)
            .parallel().runOn(Schedulers.boundedElastic())
            .flatMap(x -> webClientRA.get().uri(builder -> builder
                            .path("/solr/sudoc/select")
                            .queryParam("q", "{requestSolr}")
                            .queryParam("start", "0")
                            .queryParam("rows", "3000")
                            .queryParam("fl", "id,ppn_z,B700.B700Sa_BS,B700.B700Sb_BS")
                            .queryParam("wt", "json")
                            .build(x)
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError,
                        response -> Mono.error(
                            new IllegalStateException("Failed to get from the site!")
                        )
                )
                .bodyToMono(JsonNode.class)
                //.timeout(Duration.ofSeconds(30))
                .doOnError(e -> log.error( "ERROR => {}", e.getMessage() ))
                .onErrorResume(e -> Mono.empty())
                .map(jsonNode -> jsonNode.findValue("docs"))
                .map(v -> {
                    ObjectReader reader = mapper.readerFor(new TypeReference<List<ReferenceAutorite>>(){});
                    try {
                        return reader.<List<ReferenceAutorite>>readValue(v);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ArrayList<ReferenceAutorite>();
                    }
                })
                .flatMapMany(Flux::fromIterable))
                .map(mapStructMapper::referenceAutoriteToreferenceAutoriteDto)
                .sequential()
                .distinct(ReferenceAutoriteDto::getPpn)
                .flatMap(x -> referenceAutoriteDtoMono(x.getPpn()))
                .filter(x -> {
                    try {
                        return
                            (!Strings.isNullOrEmpty(x.getFirstName()) && !Strings.isNullOrEmpty(x.getLastName()))
                            &&
                            (LuceneSearch.Search(x.getLastName(), lastName.replace("-"," ") + "~0.8") >0)
                            &&
                            (
                            LuceneSearch.Search(x.getFirstName(), firstName.replace("-", " ") + "~0.8") > 0
                            || LuceneSearch.Search(x.getFirstName(), firstName.charAt(0) + "*" + "~0.8") > 0
                            || (firstName.replace(".", "").length() == 1 && x.getFirstName().equals(firstName))
                            );
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .onErrorResume(v -> Mono.empty())
                .map(e -> {
                    //System.out.println(e.getPpn() + ":" + e.getFirstName() + ":" + e.getLastName());
                    referenceAutoriteDtoList.add(e);
                    referenceAutoriteGetDto.setReferenceAutorite(referenceAutoriteDtoList);
                    referenceAutoriteGetDto.setPpnCounter(ppnCount.getAndIncrement()+1);

                    return referenceAutoriteGetDto;
                })
                .last()
                .doOnError(e -> log.warn( "Name not found or failed to parsing JSON" ))
                .onErrorResume(x -> Mono.just(new ReferenceAutoriteGetDto(0, new ArrayList<>())));
    }

    private Flux<ReferenceAutoriteDto> referenceAutoriteDtoMono(String ppn) {

        AtomicInteger counter = new AtomicInteger(0);
        WebClient webClient = webClientBuilder.baseUrl("https://www.sudoc.fr/").build();

        return webClient.get().uri(uriBuilder -> uriBuilder.path(ppn + ".abes").build())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
            .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
            .retrieve()
            .bodyToMono(XmlRootRecord.class)
            .doOnError(v -> log.error("ERROR => {}", v.getMessage()))
            .onErrorResume(v -> Mono.empty())
            .flatMapIterable(XmlRootRecord::getDatafieldList)
            .filter(v -> v.getTag().startsWith("70"))
            .doOnEach(v -> counter.getAndIncrement())
            .filter(v -> v.getSubfieldList()
                    .stream()
                    .noneMatch(t -> t.getCode().equals("3"))
            )
            .map(v -> {
                /*System.out.println("TAG = " + v.getTag());
                System.out.println("PPN = " + ppn);
                System.out.println("POS = " + counter.get());*/
                ReferenceAutoriteDto referenceAutoriteDto = new ReferenceAutoriteDto();
                v.getSubfieldList().forEach(t -> {
                        //System.out.println(t.getCode() + " : " + t.getSubfield());
                        referenceAutoriteDto.setPpn(ppn + "-" + counter.get());
                        if (t.getCode().equals("a")) {
                           referenceAutoriteDto.setLastName(t.getSubfield());
                        }
                        if (t.getCode().equals("b")) {
                            referenceAutoriteDto.setFirstName(t.getSubfield());
                        }
                    }
                );
                //System.out.println("==================================");
                return referenceAutoriteDto;
            })
            .onErrorResume(v -> Flux.just(new ReferenceAutoriteDto()));
    }

    //Utility function ( Check doublon with key of Java Stream() )
    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor)
    {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;

    }
}
