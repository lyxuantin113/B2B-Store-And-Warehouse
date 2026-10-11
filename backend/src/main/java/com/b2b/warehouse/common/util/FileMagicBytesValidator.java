package com.b2b.warehouse.common.util;

import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Validates uploaded B2B business registration documents and product images
 * by inspecting file header magic bytes via Apache Tika rather than trusting
 * the client-supplied {@code Content-Type} header or file extension.
 */
@Component
public class FileMagicBytesValidator {

    private static final Set<String> ALLOWED_LICENSE_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Tika tika = new Tika();

    /**
     * Detects the actual MIME type from the stream's magic bytes and verifies it is in the allowlist.
     *
     * @param inputStream InputStream supporting mark/reset or fresh byte stream
     * @return Verified MIME type string
     * @throws BusinessException if the detected MIME type is not allowed
     */
    public String validateBusinessDocument(InputStream inputStream) {
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "Uploaded file stream cannot be null");
        }
        try {
            String detectedMimeType = tika.detect(inputStream);
            if (!ALLOWED_LICENSE_MIME_TYPES.contains(detectedMimeType)) {
                throw new BusinessException(
                        ErrorCode.INVALID_FILE_TYPE,
                        "Disallowed file content detected (" + detectedMimeType + "). Allowed types: PDF, JPEG, PNG, WEBP"
                );
            }
            return detectedMimeType;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "Unable to inspect file magic bytes", ex);
        }
    }
}
