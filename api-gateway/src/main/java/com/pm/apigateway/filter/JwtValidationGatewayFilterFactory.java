package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class JwtValidationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<JwtValidationGatewayFilterFactory.Config> {

    private final WebClient webClient;

    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
                                             @Value("${auth.service.url}") String authServiceUrl) {
        super(Config.class);
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 1. Missing or invalid Authorization header -> Return 401 immediately
            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // 2. Send request to auth-service /auth/validate
            return webClient.get()
                    .uri("/validate") // Appended to auth.service.url (e.g. http://auth-service:4005/auth)
                    .header(HttpHeaders.AUTHORIZATION, token) // Passing actual token string
                    .retrieve()
                    .toBodilessEntity()
                    .then(chain.filter(exchange)) // Token valid -> Forward request to downstream service
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        // Catches 401/403 responses from auth-service and passes them to client (Prevents 500 Error)
                        exchange.getResponse().setStatusCode(ex.getStatusCode());
                        return exchange.getResponse().setComplete();
                    })
                    .onErrorResume(Exception.class, ex -> {
                        // Handles network timeouts or when auth-service is completely down
                        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    public static class Config {
        // Required by Spring Cloud Gateway factory pattern
    }
}