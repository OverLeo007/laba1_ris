package ru.paskal.laba1.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.paskal.laba1.utils.Utils;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
@RequiredArgsConstructor
public class Requester {

    private final WebClient webClient;
    private final Handler handler;


    public void fetchAndLogJson(String url) {
        webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> {
                    handler.handleResponse(Utils.toPlainUrl(url), response);
                });
    }
}