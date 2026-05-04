package br.com.delivere.acai.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Configura o MongoDB (Atlas) apenas quando app.mongodb.uri estiver definida.
 * Usado para sincronizar dados de gestão para o app mobile.
 */
@Configuration
@ConditionalOnProperty(name = "app.mongodb.uri", matchIfMissing = false)
public class MongoAtlasConfig {

    @Bean
    public MongoClient mongoClient(Environment env) {
        String uri = env.getProperty("app.mongodb.uri");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("app.mongodb.uri está vazia.");
        }
        return MongoClients.create(uri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient, Environment env) {
        String database = env.getProperty("app.mongodb.database", "acaigestao");
        return new MongoTemplate(mongoClient, database);
    }
}
