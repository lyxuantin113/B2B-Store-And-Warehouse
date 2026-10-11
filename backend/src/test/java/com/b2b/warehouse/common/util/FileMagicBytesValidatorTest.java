package com.b2b.warehouse.common.util;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileMagicBytesValidatorTest {

    private final FileMagicBytesValidator validator = new FileMagicBytesValidator();

    @Test
    @DisplayName("Should accept genuine PDF magic bytes (%PDF-1.7)")
    void shouldAcceptGenuinePdfMagicBytes() {
        byte[] pdfBytes = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n".getBytes(StandardCharsets.US_ASCII);

        String mimeType = validator.validateBusinessDocument(new ByteArrayInputStream(pdfBytes));

        assertThat(mimeType).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should accept genuine PNG magic bytes")
    void shouldAcceptGenuinePngMagicBytes() {
        byte[] pngMagicBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        String mimeType = validator.validateBusinessDocument(new ByteArrayInputStream(pngMagicBytes));

        assertThat(mimeType).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Should reject executable shell script disguised as business_license.pdf")
    void shouldRejectDisguisedExecutableScript() {
        byte[] maliciousScript = "#!/bin/sh\nrm -rf /\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validateBusinessDocument(new ByteArrayInputStream(maliciousScript)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }
}
