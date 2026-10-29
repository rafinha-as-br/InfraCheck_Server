package br.com.infracheck.server.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações próprias do InfraCheck (prefixo {@code infracheck} em {@code application.yml}).
 *
 * @param timezone  fuso usado para decidir o "hoje" (atrasos, carga diária)
 * @param security  parâmetros do JWT
 * @param storage   onde ficam fotos e assinaturas
 * @param cors      origens autorizadas a chamar a API pelo navegador (painel web)
 * @param seed      se {@code true}, popula dados de demonstração quando o banco está vazio
 */
@ConfigurationProperties(prefix = "infracheck")
public record AppProperties(String timezone, Security security, Storage storage, Cors cors, boolean seed) {

    /** @param jwtSecret segredo HS256 com pelo menos 32 caracteres; vem do ambiente em produção */
    public record Security(String jwtSecret, Duration tokenTtl) {}

    public record Storage(String path) {}

    public record Cors(List<String> allowedOriginPatterns) {}
}
