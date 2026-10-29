package br.com.infracheck.server.web;

import br.com.infracheck.server.service.AuthService;
import br.com.infracheck.server.web.dto.Dtos.LoginRequest;
import br.com.infracheck.server.web.dto.Dtos.LoginResponse;
import br.com.infracheck.server.web.dto.Dtos.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Login e dados do usuário autenticado. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    /** Público. Devolve o JWT (Bearer) e os dados do usuário. */
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r.email(), r.password());
    }

    /** Usuário do token atual; os apps usam para validar a sessão salva. */
    @GetMapping("/me")
    UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return AuthService.toDto(auth.currentUser(jwt.getSubject()));
    }
}
