package br.com.delivere.acai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class
})
@EnableScheduling
public class AcaiApplication {

    public static void main(String[] args) {
        Path dataDir = Paths.get("data").toAbsolutePath();
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (Exception e) {
            System.err.println("Aviso: não foi possível criar a pasta data: " + e.getMessage());
        }
        SpringApplication.run(AcaiApplication.class, args);
    }
}
