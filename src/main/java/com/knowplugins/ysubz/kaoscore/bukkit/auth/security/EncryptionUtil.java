package com.knowplugins.ysubz.kaoscore.bukkit.auth.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class EncryptionUtil {

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA keys", e);
        }
    }

    public static byte[] generateVerifyToken() {
        byte[] token = new byte[4];
        new SecureRandom().nextBytes(token);
        return token;
    }

    public static SecretKey decryptSharedSecret(PrivateKey privateKey, byte[] encryptedSecret) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new SecretKeySpec(cipher.doFinal(encryptedSecret), "AES");
    }

    public static byte[] decryptVerifyToken(PrivateKey privateKey, byte[] encryptedToken) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedToken);
    }

    public static String getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
        digest.update(secretKey.getEncoded());
        digest.update(publicKey.getEncoded());
        return new BigInteger(digest.digest()).toString(16);
    }
}