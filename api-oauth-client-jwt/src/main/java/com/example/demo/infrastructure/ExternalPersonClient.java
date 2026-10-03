package com.example.demo.infrastructure;

import com.example.demo.domain.Person;
import com.example.demo.domain.PersonGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalPersonClient implements PersonGateway {

    private final RestClient restClient;

    public ExternalPersonClient(OAuth2ClientHttpRequestInterceptor oauth2Interceptor,
                                @Value("${external-person.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(oauth2Interceptor)
                .build();
    }

    @Override
    public Person getPerson() {
        return restClient.get()
                .uri("/person")
                .retrieve()
                .body(Person.class);
    }
}
