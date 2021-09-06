package fr.abes.findrav2.router;

import fr.abes.findrav2.handler.ReferenceAutoriteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;


@Configuration
public class ReferenceAutoriteRouter {

    @Bean
    public RouterFunction<ServerResponse> routeReferenceAutorite(ReferenceAutoriteHandler handler) {

        return RouterFunctions.route(
            GET("/v2/findra")
                    .and(RequestPredicates.queryParam("file", t -> true))
                    .and(RequestPredicates.queryParam("nom", t -> true))
                    .and(RequestPredicates.queryParam("prenom", t -> true)),
            handler::getAllFromRequestSolr
        ).andRoute(
            GET("/v2/findra")
                    .and(RequestPredicates.queryParam("nom", t -> true))
                    .and(RequestPredicates.queryParam("prenom", t -> true)),
            handler::getAllFromRequestSolr
        );
    }


}
