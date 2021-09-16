package fr.abes.findrav2.handler;

import com.google.common.base.Strings;
import fr.abes.findrav2.domain.dto.ReferenceAutoriteGetDto;
import fr.abes.findrav2.domain.service.ReferenceContextuelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReferenceContextuelHandler {

    private final ReferenceContextuelService referenceContextuelService;

    @NonNull
    public Mono<ServerResponse> getAll(ServerRequest serverRequest) {

        String filePropertie = serverRequest.queryParam("file").isPresent() ? serverRequest.queryParam("file")
                                            .get() : "default-req-rc";

        log.info("Load propertie file => {}", filePropertie);


        String firstName = serverRequest.queryParam("prenom").isPresent() ? serverRequest.queryParam("prenom")
                .get() : "";

        String lastName = serverRequest.queryParam("nom").isPresent() ? serverRequest.queryParam("nom")
                .get() : "";


        return ( Strings.isNullOrEmpty(firstName) || Strings.isNullOrEmpty(lastName) )
                ? ServerResponse.badRequest().build()
                : ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(referenceContextuelService.findAllRC(filePropertie,firstName,lastName), ReferenceAutoriteGetDto.class)
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(e -> ServerResponse.badRequest().build() );

    }


}
