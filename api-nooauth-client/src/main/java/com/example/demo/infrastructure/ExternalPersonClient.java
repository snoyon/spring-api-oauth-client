package com.example.demo.infrastructure;

import com.example.demo.domain.Person;
import com.example.demo.domain.PersonGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalPersonClient implements PersonGateway {

    private final RestClient restClient;

    public ExternalPersonClient(@Value("${external-person.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
