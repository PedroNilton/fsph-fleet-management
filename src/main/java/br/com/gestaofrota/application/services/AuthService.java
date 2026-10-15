package br.com.gestaofrota.application.services;

import br.com.gestaofrota.api.v1.dtos.responses.LoginResponse;
import br.com.gestaofrota.application.results.LoginResult;
import br.com.gestaofrota.infra.security.jwt.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
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
        String accessToken = jwtUtil.generateToken(user);

        return new LoginResponse(accessToken, "Bearer", jwtUtil.getExpirationSeconds());
    }
}
