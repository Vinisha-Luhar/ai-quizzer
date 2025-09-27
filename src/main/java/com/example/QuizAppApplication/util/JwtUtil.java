package com.example.QuizAppApplication.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
  Simple JWT creation and validation (HS256) WITHOUT using external JWT libraries.
  Note: This is educational + mock — for production use a well-tested library like jjwt or Nimbus.
*/
public class JwtUtil {
    private static final String HMAC_ALGO = "HmacSHA256";
    // Use a secret key. In real apps keep it safe (env var, not source code).
    private static final String SECRET = "change_this_secret_to_something_strong";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createToken(String username, long ttlSeconds) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long iat = Instant.now().getEpochSecond();
            long exp = iat + ttlSeconds;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", username);
            payload.put("iat", iat);
            payload.put("exp", exp);

            String headerJson = mapper.writeValueAsString(header);
            String payloadJson = mapper.writeValueAsString(payload);

            String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerB64 + "." + payloadB64;
            byte[] sig = hmacSha256(SECRET.getBytes(StandardCharsets.UTF_8), signingInput);
            String signatureB64 = base64UrlEncode(sig);

            return signingInput + "." + signatureB64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create token", e);
        }
    }

    // verifies token: structure, signature, expiration. Returns subject (username) if valid, otherwise null.
    public static String validateTokenAndGetSubject(String token) {
        try {
            if (token == null) return null;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String sigB64 = parts[2];

            String signingInput = headerB64 + "." + payloadB64;
            byte[] expectedSig = hmacSha256(SECRET.getBytes(StandardCharsets.UTF_8), signingInput);
            String expectedSigB64 = base64UrlEncode(expectedSig);

            if (!constantTimeEquals(expectedSigB64, sigB64)) {
                return null;
            }

            // decode payload
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            Map<?,?> payload = mapper.readValue(payloadBytes, Map.class);

            // check expiration
            Number expNum = (Number) payload.get("exp");
            if (expNum == null) return null;
            long exp = expNum.longValue();
            long now = Instant.now().getEpochSecond();
            if (now > exp) return null;

            Object sub = payload.get("sub");
            return sub == null ? null : sub.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // simple constant-time string compare to avoid timing attacks
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
