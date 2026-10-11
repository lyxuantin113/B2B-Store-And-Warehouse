package com.b2b.warehouse.auth.entity;

/**
 * 3-Stage B2B Onboarding & Account Lifecycle statuses.
 * <ul>
 *   <li>{@code PENDING_ADMIN}: Stage 1 - Application submitted, awaiting Admin/Manager review.</li>
 *   <li>{@code ADMIN_APPROVED}: Stage 2 - Admin approved with notice; awaiting B2B Partner final confirmation.</li>
 *   <li>{@code ACTIVE}: Stage 3 - Partner confirmed; full access to 8-Tab B2B Wholesale &amp; Sample Portal.</li>
 *   <li>{@code REJECTED}: Application rejected by Admin; partner can edit &amp; Re-apply in-place.</li>
 *   <li>{@code SUSPENDED}: Account suspended by Admin; all active tokens immediately revoked.</li>
 * </ul>
 */
public enum AccountStatus {
    PENDING_ADMIN,
    ADMIN_APPROVED,
    ACTIVE,
    REJECTED,
    SUSPENDED
}
