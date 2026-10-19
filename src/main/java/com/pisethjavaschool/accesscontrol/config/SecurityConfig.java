package com.pisethjavaschool.accesscontrol.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.pisethjavaschool.platform.security.ProvisioningJwtAuthoritiesConverter;
import com.pisethjavaschool.platform.security.ServiceClientAuthorizationManager;

import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            @Value("${security.provisioning.client-id}") String clientId,
            @Value("${security.provisioning.audience}") String audience) {
        var provisioning = new ServiceClientAuthorizationManager(clientId, audience, "ROLE_INTERNAL_PROVISIONING");
        var jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(new ProvisioningJwtAuthoritiesConverter().convert(jwt)));
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/access-control/internal/provisioning/users/*/roles/by-code")
                            .access(provisioning)
                        .pathMatchers(HttpMethod.POST, "/api/v1/access-control/users/*/roles/**").denyAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/access-control/users/*/roles").denyAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/access-control/users/*/roles/**").denyAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
                .build();
    }
}

