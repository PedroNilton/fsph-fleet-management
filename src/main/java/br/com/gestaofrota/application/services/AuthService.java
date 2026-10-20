package br.com.gestaofrota.application.services;

import br.com.gestaofrota.api.v1.dtos.responses.LoginResponse;
import br.com.gestaofrota.infra.security.jwt.JwtService;
import br.com.gestaofrota.infra.security.services.RevokedTokenService;
import br.com.gestaofrota.infra.security.exceptions.InvalidTokenException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RevokedTokenService revokedTokenService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, RevokedTokenService revokedTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.revokedTokenService = revokedTokenService;
    }

    public LoginResponse login(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizedEmail,
                        password
                )
        );

        UserDetails user = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(user);

        return new LoginResponse(accessToken, "Bearer", jwtService.getExpirationSeconds());
    }

    public void logout(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")
                || authorization.length() == "Bearer ".length()) {
            throw new InvalidTokenException("Token ausente ou inválido");
        }
        try {
            revokedTokenService.revoke(authorization.substring("Bearer ".length()));
        } catch (JWTVerificationException exception) {
            throw new InvalidTokenException("Token inválido ou expirado", exception);
        }
    }
}
