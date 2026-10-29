package br.com.infracheck.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da API do InfraCheck.
 *
 * <p>A API é única e centralizada (Confluence: "Documentação oficial"): atende o app do inspetor
 * (Flutter mobile) e o painel de gestão (Flutter web) pelos mesmos contratos REST.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class InfraCheckServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraCheckServerApplication.class, args);
    }
}
