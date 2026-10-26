package com.javed.java_pgvector.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfiguration {

    @Bean
    RestClientCustomizer ollamaTimeouts() {
        return builder -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(httpClient);
            requests.setReadTimeout(Duration.ofSeconds(120));
            builder.requestFactory(requests);
        };
    }
}
