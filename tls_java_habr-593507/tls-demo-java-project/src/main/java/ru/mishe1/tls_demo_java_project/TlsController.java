package ru.mishe1.tls_demo_java_project;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TlsController {

    @GetMapping
    public String helloWorld() {
        return "Hello, world!";
    }
}