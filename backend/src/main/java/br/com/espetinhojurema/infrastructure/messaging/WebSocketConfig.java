package br.com.espetinhojurema.infrastructure.messaging;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOriginPatterns;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(
            @Value("#{'${app.cors.allowed-origins}'.split(',')}") List<String> allowedOriginPatterns,
            JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.allowedOriginPatterns = allowedOriginPatterns.stream().map(String::trim).toList();
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] patterns = allowedOriginPatterns.toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(patterns)
                .addInterceptors(jwtHandshakeInterceptor);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(patterns)
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }
}
