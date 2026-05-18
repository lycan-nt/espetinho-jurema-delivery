package br.com.espetinhojurema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EspetinhoJuremaApplication {

    public static void main(String[] args) {
        // Antes de qualquer classe AWT — senão isHeadless() pode ficar true e o seletor de pasta não abre.
        System.setProperty("java.awt.headless", "false");
        SpringApplication app = new SpringApplication(EspetinhoJuremaApplication.class);
        app.setHeadless(false);
        app.run(args);
    }
}
