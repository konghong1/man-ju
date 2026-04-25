package org.kh.manju;

import org.kh.manju.config.ManJuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ManJuProperties.class)
public class ManJuApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManJuApplication.class, args);
    }
}
