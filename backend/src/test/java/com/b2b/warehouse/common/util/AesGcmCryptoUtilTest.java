package com.b2b.warehouse.common.util;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmCryptoUtilTest {

    private static final String TEST_BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="; // 32 bytes
    private AesGcmCryptoUtil cryptoUtil;

    @BeforeEach
    void setUp() {
        cryptoUtil = new AesGcmCryptoUtil(TEST_BASE64_KEY);
    }

    @Test
    @DisplayName("Should encrypt and decrypt VNPay/MoMo secret keys accurately")
    void shouldEncryptAndDecryptRoundTrip() {
        String originalSecret = "VNPAY_HASH_SECRET_998877665544332211_PRODUCTION";

        String encrypted = cryptoUtil.encrypt(originalSecret);
        String decrypted = cryptoUtil.decrypt(encrypted);

        assertThat(encrypted).isNotBlank().isNotEqualTo(originalSecret);
        assertThat(decrypted).isEqualTo(originalSecret);
    }

    @Test
    @DisplayName("Should generate distinct ciphertexts for identical plaintexts using random 96-bit IV")
    void shouldProduceDifferentCiphertextsForSamePlaintext() {
        String secret = "GHN_SHOP_API_TOKEN_2026";

        String ciphertext1 = cryptoUtil.encrypt(secret);
        String ciphertext2 = cryptoUtil.encrypt(secret);

        assertThat(ciphertext1).isNotEqualTo(ciphertext2);
        assertThat(cryptoUtil.decrypt(ciphertext1)).isEqualTo(secret);
        assertThat(cryptoUtil.decrypt(ciphertext2)).isEqualTo(secret);
    }

    @Test
    @DisplayName("Should detect ciphertext tampering via GCM 128-bit authentication tag and throw CRYPTO_ERROR")
    void shouldThrowExceptionWhenCiphertextIsTampered() {
        String encrypted = cryptoUtil.encrypt("MOMO_SECRET_KEY");
        byte[] rawBytes = Base64.getDecoder().decode(encrypted);
        // Flip the last bit of the authentication tag
        rawBytes[rawBytes.length - 1] ^= 0x01;
        String tamperedBase64 = Base64.getEncoder().encodeToString(rawBytes);

        assertThatThrownBy(() -> cryptoUtil.decrypt(tamperedBase64))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRYPTO_ERROR);
    }

    @Test
    @DisplayName("Should reject master keys that are not 256-bit (32 bytes)")
    void shouldRejectInvalidKeyLength() {
        String shortKeyBase64 = Base64.getEncoder().encodeToString("short-16byte-key".getBytes());

        assertThatThrownBy(() -> new AesGcmCryptoUtil(shortKeyBase64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }
}
