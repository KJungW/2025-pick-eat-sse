package com.pickeat.sse.global.auth.participant;


import com.pickeat.sse.global.auth.JwtProvider;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ParticipantTokenProvider {

    private static final String PICKEAT_CODE_CLAIM_KEY = "pickeatCode";

    private final JwtProvider jwtProvider;
    private final long expirationMillis;

    public ParticipantTokenProvider(
            JwtProvider jwtProvider,
            @Value("${participant.jwt.expiration}") long expirationMillis
    ) {
        this.jwtProvider = jwtProvider;
        this.expirationMillis = expirationMillis;
    }

    public String getParticipantCode(String token) {
        Claims claims = jwtProvider.getClaims(token);
        return claims.getSubject();
    }

    public String getPickeatCode(String token) {
        Claims claims = jwtProvider.getClaims(token);
        return claims.get(PICKEAT_CODE_CLAIM_KEY, String.class);
    }
}
