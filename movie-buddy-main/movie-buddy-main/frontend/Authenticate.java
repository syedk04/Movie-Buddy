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
import javax.crypto.SecretKey;

/**
 *
 * @author student
 */
public class Authenticate {
    private static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private static final Key signingKey;
    
    // Static initialization block to set up the signing key once
    static {
        // Use a fixed, secure key - at least 32 bytes (256 bits) for HS256
        // In production, this should be loaded from a secure configuration, not hardcoded
        String secretKeyString = "thisIsASecureKeyWithAtLeast32Bytes123456"; // 38 characters = 304 bits
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }
    
    
    
    public Authenticate() {
    }
    
    public String createJWT(String issuer, String subject, long ttlMillis) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        
        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setSubject(subject)
                .setIssuer(issuer)
                .signWith(signingKey);
                
        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }
        
        return builder.compact();
    }
    
    public Entry<Boolean, String> verify(String jwt) throws UnsupportedEncodingException {
        Jws<Claims> jws = null;
        String username = "";
        
        try {
            jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(jwt);
                    
            username = jws.getBody().getSubject();
            
        } catch (JwtException ex) {
            System.out.println("JWT verification failed: " + ex.getMessage());
            return new AbstractMap.SimpleEntry<>(false, "");
        }
        
        // Check if token is expired
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (jws.getBody().getExpiration().before(now)) {
            return new AbstractMap.SimpleEntry<>(false, "");
        }
        
        return new AbstractMap.SimpleEntry<>(true, username);
    }
}