package br.com.infracheck.server.service;

import br.com.infracheck.server.config.AppProperties;
import br.com.infracheck.server.domain.AppUser;
import br.com.infracheck.server.repository.UserRepository;
import br.com.infracheck.server.web.ApiException;
import br.com.infracheck.server.web.dto.Dtos.LoginResponse;
import br.com.infracheck.server.web.dto.Dtos.UserDto;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login por e-mail/senha e emissão do JWT (claims: {@code sub}=id do usuário, {@code role}). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties props;
    private final Clock clock;

    /**
     * Autentica o usuário. A mensagem de falha é a mesma para e-mail inexistente, senha errada e usuário
     * inativo, para não revelar quais e-mails existem.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        AppUser user = users.findByEmailIgnoreCase(email.trim())
                .filter(AppUser::isActive)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha incorretos."));
        Instant now = clock.instant();
        Instant expires = now.plus(props.security().tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expires)
                .claim("role", user.getRole().name())
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new LoginResponse(token, expires, toDto(user));
    }

    /** Usuário dono do token; falha se ele foi removido ou desativado depois da emissão. */
    @Transactional(readOnly = true)
    public AppUser currentUser(String subject) {
        return users.findById(UUID.fromString(subject))
                .filter(AppUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNKNOWN_USER", "Sessão inválida."));
    }

    public static UserDto toDto(AppUser u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getRegistration());
    }
}
