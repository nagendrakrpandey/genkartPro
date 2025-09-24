package Tech_Nagendra.Certificates_genration.Utility;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecretBase64;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretBase64);
        signingKey = Keys.hmacShaKeyFor(keyBytes); // will throw if key too short
    }



    public String generateToken(String username, String email) {
        return Jwts.builder()
                .setSubject(username) // main subject = username
                .claim("email", email) // extra claim for email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7)) // 7 days
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) throws JwtException {
        // ✅ Clean token: remove spaces and non-ASCII
        token = token.trim().replaceAll("[^\\x20-\\x7E]", "");
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token, String username) {
        try {
            String sub = extractUsername(token);
            return sub.equals(username) && !isTokenExpired(token);
        } catch (JwtException e) {
            // invalid, expired, or malformed token
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true; // treat malformed token as expired
        }
    }
}
