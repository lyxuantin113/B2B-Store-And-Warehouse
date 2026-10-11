package com.b2b.warehouse.common.util;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Authenticated Encryption with Associated Data (AEAD) utility using AES-256-GCM.
 * Used to encrypt sensitive 3rd-party API keys (VNPay HashSecret, MoMo SecretKey, GHN Token)
 * stored in the {@code system_configs} table.
 *
 * <p>Ciphertext layout: {@code Base64( [12-byte Random IV] || [Ciphertext + 16-byte Auth Tag] )}</p>
 */
@Component
public class AesGcmCryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12; // 96-bit IV recommended by NIST SP 800-38D
    private static final int TAG_LENGTH_BITS = 128; // 128-bit authentication tag

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCryptoUtil(@Value("${app.security.aes-master-key}") String base64MasterKey) {
        byte[] decodedKey = Base64.getDecoder().decode(base64MasterKey);
        if (decodedKey.length != 32) {
            throw new IllegalArgumentException("AES-256 master key must decode to exactly 32 bytes (256 bits)");
        }
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.CRYPTO_ERROR, "Failed to encrypt sensitive data", ex);
        }
    }

    public String decrypt(String base64Payload) {
        if (base64Payload == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Payload);
            if (decoded.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid AES-GCM payload length");
            }

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintextBytes = cipher.doFinal(ciphertext);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.CRYPTO_ERROR, "Failed to decrypt or verify integrity of sensitive data", ex);
        }
    }
}
