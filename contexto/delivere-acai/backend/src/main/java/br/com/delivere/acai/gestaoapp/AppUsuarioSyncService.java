package br.com.delivere.acai.gestaoapp;

import br.com.delivere.acai.auth.User;
import br.com.delivere.acai.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Sincroniza usuários do sistema (H2) para a collection app_usuarios no MongoDB,
 * para o app mobile fazer login direto via Atlas Data API (sem depender do backend).
 */
@Service
@ConditionalOnBean(MongoTemplate.class)
public class AppUsuarioSyncService {

    private static final Logger log = LoggerFactory.getLogger(AppUsuarioSyncService.class);
    private static final String COLLECTION = "app_usuarios";

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public AppUsuarioSyncService(UserRepository userRepository, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Sincroniza todos os usuários para o MongoDB (username como id do documento para upsert simples).
     */
    public void sync() {
        try {
            for (User user : userRepository.findAll()) {
                String username = user.getUsername();
                if (username == null || username.isBlank()) continue;
                AppUsuarioDocument doc = new AppUsuarioDocument(
                        username.trim().toLowerCase(),
                        user.getPassword(),
                        user.getSetor() != null ? user.getSetor() : ""
                );
                doc.setId(username.trim().toLowerCase());
                mongoTemplate.save(doc, COLLECTION);
            }
            log.debug("App usuários sincronizados para MongoDB ({} usuários)", userRepository.count());
        } catch (Exception e) {
            log.warn("Falha ao sincronizar app usuários para MongoDB: {}", e.getMessage());
        }
    }
}
