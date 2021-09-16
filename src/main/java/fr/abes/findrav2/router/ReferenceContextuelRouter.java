package fr.abes.findrav2.router;

import fr.abes.findrav2.handler.ReferenceContextuelHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;


@Configuration
public class ReferenceContextuelRouter {

    @Bean
    public RouterFunction<ServerResponse> routeReferenceContextuel(ReferenceContextuelHandler handler) {

        return RouterFunctions.route(
            GET("/v2/findrc")
                    .and(RequestPredicates.queryParam("file", t -> true))
                    .and(RequestPredicates.queryParam("nom", t -> true))
                    .and(RequestPredicates.queryParam("prenom", t -> true)),
            handler::getAll
        ).andRoute(
            GET("/v2/findrc")
                    .and(RequestPredicates.queryParam("nom", t -> true))
                    .and(RequestPredicates.queryParam("prenom", t -> true)),
            handler::getAll
        );
    }


}
