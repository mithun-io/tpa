package com.tpa.security;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtKeyGenerator {
    static void main(String[] args) {
        byte[] key = new byte[64];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        String BASE64 = Base64.getEncoder().encodeToString(key);
        System.out.printf(BASE64);
    }
}
