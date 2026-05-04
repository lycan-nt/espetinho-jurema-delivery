package br.com.espetinhojurema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EspetinhoJuremaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EspetinhoJuremaApplication.class, args);
    }
}
