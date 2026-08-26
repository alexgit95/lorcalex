package com.alexgit95;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LorcalexApplication {
    public static void main(String[] args) {
        SpringApplication.run(LorcalexApplication.class, args);
    }
}
