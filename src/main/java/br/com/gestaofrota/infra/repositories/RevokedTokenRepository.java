package br.com.gestaofrota.infra.repositories;

import br.com.gestaofrota.infra.security.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query(value = """
            INSERT INTO revoked_tokens (token_hash, expires_at)
            VALUES (:tokenHash, :expiresAt)
            ON CONFLICT (token_hash) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("tokenHash") String tokenHash, @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Query("delete from RevokedToken token where token.expiresAt <= :now")
    int deleteExpired(Instant now);
}
