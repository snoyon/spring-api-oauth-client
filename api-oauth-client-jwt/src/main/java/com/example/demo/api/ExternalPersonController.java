package com.example.demo.api;

import com.example.demo.application.GetExternalPerson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExternalPersonController {

    private final GetExternalPerson getExternalPerson;

    public ExternalPersonController(GetExternalPerson getExternalPerson) {
        this.getExternalPerson = getExternalPerson;
    }

    @GetMapping("/external/person")
    public PersonResponse getPerson() {
        var person = getExternalPerson.execute();
        return new PersonResponse(person.firstName(), person.lastName());
    }
}
