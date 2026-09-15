package ryerson.ca.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for hashing and verifying passwords using SHA-256 with a random salt.
 *
 * Stored format: base64(salt) + ":" + base64(sha256(salt + password))
 *
 * New user registrations will use hashed passwords. Existing users with plaintext
 * passwords stored in the database will continue to authenticate via the plaintext
 * fallback in Business.authenticate() until their passwords are migrated.
 */
public class PasswordUtil {

    private static final int SALT_BYTES = 16;
    private static final String SEPARATOR = ":";

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = computeHash(salt, password);
        return saltB64 + SEPARATOR + hashB64;
    }

    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        String[] parts = stored.split(SEPARATOR, 2);
        if (parts.length != 2) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String expected = parts[1];
            return expected.equals(computeHash(salt, password));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String computeHash(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
