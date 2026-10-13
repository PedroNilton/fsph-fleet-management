package br.com.gestaofrota.auth;

import br.com.gestaofrota.auth.dto.LoginRequest;
import br.com.gestaofrota.auth.dto.LoginResponse;
import br.com.gestaofrota.security.JwtService;
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

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizedEmail,
                        request.password()
                )
        );

        UserDetails user = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(user);

        return new LoginResponse(accessToken, "Bearer", jwtService.getExpirationSeconds());
    }
}
