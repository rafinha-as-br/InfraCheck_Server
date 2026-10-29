package br.com.infracheck.server.web;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Erro de negócio com código estável para os clientes (campo {@code code} do JSON de erro) e,
 * opcionalmente, uma lista de detalhes legíveis (ex.: itens sem resposta).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;

    public ApiException(HttpStatus status, String code, String message, List<String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException unprocessable(String code, String message, List<String> details) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message, details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
