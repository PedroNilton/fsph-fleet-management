package br.com.gestaofrota.infra.security.jwt;

import br.com.gestaofrota.infra.security.services.CustomUserDetailsService;
import br.com.gestaofrota.infra.security.services.RevokedTokenService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final RevokedTokenService revokedTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            RevokedTokenService revokedTokenService,
            CustomUserDetailsService userDetailsService,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService = jwtService;
        this.revokedTokenService = revokedTokenService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);

            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        try {
            String email = jwtService.extractSubject(token);

            if (revokedTokenService.isRevoked(token)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, new BadCredentialsException("Token revogado"));
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(email);

                if (!jwtService.isTokenValid(token, user)) {
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new BadCredentialsException("Token inválido")
                    );
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                user,
                                null,
                                user.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JWTVerificationException | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Token inválido ou expirado", exception)
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
