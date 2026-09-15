package ryerson.ca.frontend;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Authenticate {

    private static final Logger logger = Logger.getLogger(Authenticate.class.getName());
    private static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private static final Key signingKey;

    static {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 32) {
            logger.warning("JWT_SECRET env var not set or too short; using default key. Set JWT_SECRET in production.");
            secret = "thisIsASecureKeyWithAtLeast32Bytes123456";
        }
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Authenticate() {}

    public String createJWT(String issuer, String subject, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setSubject(subject)
                .setIssuer(issuer)
                .signWith(signingKey);

        if (ttlMillis > 0) {
            builder.setExpiration(new Date(nowMillis + ttlMillis));
        }

        return builder.compact();
    }

    public Entry<Boolean, String> verify(String jwt) throws UnsupportedEncodingException {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(jwt);

            // parseClaimsJws already throws ExpiredJwtException if expired
            String username = jws.getBody().getSubject();
            return new AbstractMap.SimpleEntry<>(true, username);

        } catch (JwtException ex) {
            logger.log(Level.FINE, "JWT verification failed: {0}", ex.getMessage());
            return new AbstractMap.SimpleEntry<>(false, "");
        }
    }
}
