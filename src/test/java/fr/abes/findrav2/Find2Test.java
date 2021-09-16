package fr.abes.findrav2;

import fr.abes.findrav2.domain.entity.XmlRootRecord;
import fr.abes.findrav2.domain.service.ReferenceContextuelService;
import fr.abes.findrav2.domain.utils.LuceneSearch;
import fr.abes.findrav2.domain.utils.MapStructMapper;
import fr.abes.findrav2.domain.utils.StringOperator;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootTest
public class Find2Test {

    @Autowired
    StringOperator stringOperator;

    @Autowired
    WebClient.Builder webClientBuilder;

    @Autowired
    MapStructMapper mapStructMapper;

    @Autowired
    ReferenceContextuelService referenceContextuelService;

    @Test
    void readSudocXml() {

        WebClient webClient = webClientBuilder.baseUrl("https://www.sudoc.fr/130405302.abes")
                .build();

        XmlRootRecord xmlRootRecord = webClient.get()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .retrieve()
                .bodyToMono(XmlRootRecord.class)
                .block();

        AtomicInteger counter = new AtomicInteger(0);
        assert xmlRootRecord != null;
        xmlRootRecord.getDatafieldList().stream()
                .filter(e -> e.getTag().startsWith("70"))
                .peek(u -> counter.getAndIncrement())
                .filter(e -> e.getSubfieldList()
                        .stream()
                        .noneMatch(t ->  t.getCode().equals("3"))
                )
                .collect(Collectors.toList())
                .forEach(e -> {
                    System.out.println(e.getTag());
                    e.getSubfieldList().forEach(x -> {
                        System.out.println(x.getCode() + " : " + x.getSubfield());
                    });
                });

        System.out.println(counter.get());

    }

    @Test
    void getFindRC() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        referenceContextuelService
                .findAllRC("default-req-rc","Valérie", "Robert")
                .subscribe(e -> {
                    System.out.println(e.getPpnCounter());
                    e.getReferenceAutorite().forEach(System.out::println);
                }, System.out::println, countDownLatch::countDown);
        countDownLatch.await();
    }

    @Test
    void luceneSearch() throws ParseException {
        System.out.println(
                LuceneSearch.Search("Jules" , "Paul~0.8")
        );

    }
}
