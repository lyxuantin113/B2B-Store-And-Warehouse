package com.b2b.warehouse.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts column values before DB write
 * and decrypts them upon entity read using {@link AesGcmCryptoUtil}.
 */
@Component
@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesGcmCryptoUtil aesGcmCryptoUtil;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return aesGcmCryptoUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return aesGcmCryptoUtil.decrypt(dbData);
    }
}
