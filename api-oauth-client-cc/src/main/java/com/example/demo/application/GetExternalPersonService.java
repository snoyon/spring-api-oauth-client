package com.example.demo.application;

import com.example.demo.domain.Person;
import com.example.demo.domain.PersonGateway;
import org.springframework.stereotype.Service;

@Service
public class GetExternalPersonService implements GetExternalPerson {

    private final PersonGateway personGateway;

    public GetExternalPersonService(PersonGateway personGateway) {
        this.personGateway = personGateway;
    }

    @Override
    public Person execute() {
        return personGateway.getPerson();
    }
}
