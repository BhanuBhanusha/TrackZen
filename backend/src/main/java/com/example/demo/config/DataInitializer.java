package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // No predefined dummy names or dummy accounts.
        // Users sign in / register with their own actual names.
    }
}
