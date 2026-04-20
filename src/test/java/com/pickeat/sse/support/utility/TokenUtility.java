package com.pickeat.sse.support.utility;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class TokenUtility {

    private final String secretKey;

    public TokenUtility(@Value("${jwt.secretKey}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String createToken(String participantCode, String pickeatCode) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(participantCode)
                .claim("pickeatCode", pickeatCode)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }
}
