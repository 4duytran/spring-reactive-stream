package fr.abes.findrav2.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@Slf4j
public class BeanConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {

        log.info("Initializing WebClient Bean");

        return WebClient.builder()
            .clientConnector(
            new ReactorClientHttpConnector(HttpClient.create()  // <== Create new reactor client for the connection
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // <== Wait server response
                    .responseTimeout(Duration.ofSeconds(5))  // <== Wait server response after send packet
                    .doOnConnected(connection -> connection
                            .addHandler(new ReadTimeoutHandler(5))
                            .addHandler(new WriteTimeoutHandler(5))
                    )
            )
        );
    }

}
