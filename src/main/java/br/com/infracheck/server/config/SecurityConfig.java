package br.com.infracheck.server.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Segurança da API: stateless, JWT HS256 no cabeçalho {@code Authorization: Bearer}, papéis vindos
 * da claim {@code role}.
 *
 * <p>A visibilidade por registro (o inspetor só enxerga as próprias OS) é aplicada nos serviços; aqui
 * ficam apenas as regras por rota e papel.
 */
@Configuration
public class SecurityConfig {

    private static final String MANAGER = "CONTRACT_MANAGER";
    private static final String ADMIN = "ADMIN";
    private static final String INSPECTOR = "INSPECTOR";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/auth/login", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Gestão
                        .requestMatchers("/api/v1/dashboard/**", "/api/v1/contracts", "/api/v1/checklists", "/api/v1/inspectors")
                        .hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/inspections").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(
                                "/api/v1/inspections/*/approve",
                                "/api/v1/inspections/*/reject",
                                "/api/v1/inspections/*/action-plans")
                        .hasAnyRole(ADMIN, MANAGER)
                        // Campo
                        .requestMatchers(
                                "/api/v1/inspections/*/start",
                                "/api/v1/inspections/*/submit",
                                "/api/v1/inspections/*/photos/*")
                        .hasRole(INSPECTOR)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Converte a claim {@code role} em authority {@code ROLE_<role>}. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey secretKey(AppProperties props) {
        String secret = props.security().jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "infracheck.security.jwt-secret (INFRACHECK_JWT_SECRET) deve ter pelo menos 32 caracteres");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(AppProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(props)));
    }

    @Bean
    JwtDecoder jwtDecoder(AppProperties props) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey(props)).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault()));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(props.cors().allowedOriginPatterns());
        cors.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(java.util.List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /** Relógio único da aplicação, no fuso configurado; facilita testes e a noção de "hoje". */
    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.timezone()));
    }
}
