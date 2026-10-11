package com.b2b.warehouse.gateway.payment;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Cryptographic HMAC utility for VNPay (HMAC-SHA512) and MoMo/VietQR (HMAC-SHA256)
 * with constant-time signature comparison to prevent timing side-channel attacks.
 */
public final class HmacSignUtil {

    private HmacSignUtil() {
    }

    public static String hmacSha512Hex(String secret, String data) {
        return computeHmacHex("HmacSHA512", secret, data);
    }

    public static String hmacSha256Hex(String secret, String data) {
        return computeHmacHex("HmacSHA256", secret, data);
    }

    public static boolean constantTimeEquals(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) {
            return false;
        }
        byte[] expectedBytes = expectedHex.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualHex.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private static String computeHmacHex(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.CRYPTO_ERROR, "Failed to compute " + algorithm + " signature", ex);
        }
    }
}
