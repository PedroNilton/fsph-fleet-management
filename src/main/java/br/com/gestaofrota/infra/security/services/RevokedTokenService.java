package br.com.gestaofrota.infra.security.services;

import br.com.gestaofrota.infra.repositories.RevokedTokenRepository;
import br.com.gestaofrota.infra.security.jwt.JwtService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RevokedTokenService {

    private final RevokedTokenRepository repository;
    private final JwtService jwtService;

    public RevokedTokenService(RevokedTokenRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void revoke(String token) {
        repository.insertIfAbsent(hash(token), jwtService.getExpiration(token));
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        return repository.existsByTokenHash(hash(token));
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void deleteExpired() {
        repository.deleteExpired(Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
