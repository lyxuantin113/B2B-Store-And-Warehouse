package com.b2b.warehouse.auth.security;

import com.b2b.warehouse.auth.entity.AccountStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Stateless security principal populated from validated JWT Access Token claims.
 */
@Getter
@Builder
public class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String roleCode;
    private final AccountStatus accountStatus;
    private final int tokenVersion;
    private final List<String> permissions;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roleCode != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }
        if (permissions != null) {
            for (String perm : permissions) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.SUSPENDED;
    }
}
