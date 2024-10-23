package ru.paskal.laba1.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.paskal.laba1.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class Requester {

    private final WebClient webClient;
    private final Handler handler;
    private final ObjectMapper objectMapper;


    public void fetchAndLogJson(String url) {
        if (Utils.isResourceUrl(url)) {
            handleResourceUrl(url);
        } else {
            handleInternetUrl(url);
        }
    }

    private void handleResourceUrl(String url) {
        log.info("Fetching data from local");
        String resourcePath = Utils.getResourcePath(url);
        try (InputStream inputStream = Utils.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.error("Resource not found: {}", resourcePath);
                return;
            }
            JsonNode jsonNode = objectMapper.readTree(inputStream);
            handler.handleResponse(Utils.toPlainUrl(url), jsonNode.toPrettyString());
        } catch (IOException e) {
            log.error("Error reading JSON from resource \"{}\": {}", resourcePath, e.getMessage());
        }
    }

    private void handleInternetUrl(String url) {
        log.info("Fetching data from web");
        webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> handler.handleResponse(Utils.toPlainUrl(url), response));
    }
//    public void fetchAndLogJson(String url) {
//        webClient.get()
//                .uri(url)
//                .retrieve()
//                .bodyToMono(String.class)
//                .subscribe(response -> {
//                    handler.handleResponse(Utils.toPlainUrl(url), response);
//                });
//    }
}