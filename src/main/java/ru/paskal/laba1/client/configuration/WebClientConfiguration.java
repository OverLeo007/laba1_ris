package ru.paskal.laba1.client.configuration;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Configuration
@ComponentScan("ru.paskal.laba1")
@Slf4j
@PropertySource("classpath:application.properties")
public class WebClientConfiguration {

    private final Integer TIMEOUT;


    public WebClientConfiguration(Environment env) {
        this.TIMEOUT = Integer.valueOf(Objects.requireNonNull(env.getProperty("client.timeout")));
    }

    @Bean
    public WebClient webClientWithTimeout() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(TIMEOUT, TimeUnit.MILLISECONDS));
                });
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                        .build()
                )
                .build();
    }

    @PostConstruct
    public void setup() {
        Hooks.onErrorDropped(e -> {
            // Custom error handling logic
            log.error(e.getMessage());
        });
    }

}
