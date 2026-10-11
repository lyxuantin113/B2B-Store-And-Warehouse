package com.b2b.warehouse.common.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Defense-in-depth XSS Sanitizer backed by OWASP Java HTML Sanitizer.
 * Sanitizes rich-text HTML submitted for Admin Broadcast Announcements and B2B Policy pages
 * before persisting to PostgreSQL.
 */
@Component
public class HtmlSanitizerUtil {

    private static final PolicyFactory SAFE_HTML_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.LINKS)
            .and(new HtmlPolicyBuilder()
                    .allowElements("h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "hr", "span", "div")
                    .allowAttributes("class").onElements("span", "div", "p", "table", "th", "td")
                    .toFactory());

    /**
     * Strips dangerous tags ({@code <script>}, {@code <iframe>}, {@code <object>}), inline event handlers
     * ({@code onerror}, {@code onload}), and {@code javascript:} URLs while preserving safe rich-text formatting.
     */
    public String sanitizeRichText(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        return SAFE_HTML_POLICY.sanitize(rawHtml);
    }
}
