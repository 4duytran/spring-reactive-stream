package fr.abes.findrav2.router;

import fr.abes.findrav2.handler.ReferenceAutoriteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;


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
