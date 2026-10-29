package br.com.infracheck.server.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduz exceções em um corpo de erro único: {@code {"code","message","details":[...]}}.
 * Erros inesperados nunca expõem stack trace ao cliente.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Corpo de erro devolvido em qualquer falha. */
    public record ErrorBody(String code, String message, List<String> details) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorBody(e.getCode(), e.getMessage(), e.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorBody("VALIDATION_ERROR", "Dados inválidos.", details));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ErrorBody> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", "Requisição malformada.", List.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorBody> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorBody("FILE_TOO_LARGE", "Arquivo acima do limite permitido.", List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> handleUnexpected(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL_ERROR", "Erro interno. Tente novamente.", List.of()));
    }
}
