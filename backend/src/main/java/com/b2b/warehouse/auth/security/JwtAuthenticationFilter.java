package com.b2b.warehouse.auth.security;

import com.b2b.warehouse.auth.entity.AccountStatus;
import com.b2b.warehouse.common.dto.ApiResponse;
import com.b2b.warehouse.common.exception.BusinessException;
import com.b2b.warehouse.common.exception.ErrorCode;
import com.b2b.warehouse.rbac.entity.RoleCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Stateless JWT authentication filter &amp; B2B 3-Stage Onboarding Gatekeeper.
 *
 * <p>If a user has role {@code B2B_PARTNER} and their {@code accountStatus != ACTIVE},
 * they are restricted strictly to {@code /api/v1/b2b/onboarding/**} and {@code /api/v1/auth/**}
 * endpoints until they complete Stage 3 confirmation.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            AuthenticatedUser principal = jwtTokenProvider.extractAuthenticatedUser(jwt);

            if (principal.getAccountStatus() == AccountStatus.SUSPENDED) {
                writeErrorResponse(response, ErrorCode.FORBIDDEN, "Your account has been suspended by an administrator.");
                return;
            }

            // B2B Partner 3-Stage Onboarding Gatekeeper
            String path = request.getRequestURI();
            if (RoleCode.B2B_PARTNER.name().equals(principal.getRoleCode())
                    && principal.getAccountStatus() != AccountStatus.ACTIVE
                    && isRestrictedB2bPortalPath(path)) {
                writeErrorResponse(response, ErrorCode.PARTNER_NOT_ACTIVE, ErrorCode.PARTNER_NOT_ACTIVE.getDefaultMessage());
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, ex.getErrorCode(), ex.getMessage());
        }
    }

    private boolean isRestrictedB2bPortalPath(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("/api/v1/b2b/onboarding")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/public")) {
            return false;
        }
        return path.startsWith("/api/v1/b2b/");
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.error(errorCode.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
