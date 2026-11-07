package io.github.tomaszfranczak.batchprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KafkaBatchProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaBatchProcessorApplication.class, args);
    }
}
