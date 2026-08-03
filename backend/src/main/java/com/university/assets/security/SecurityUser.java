package com.university.assets.security;

import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class SecurityUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final AccountStatus accountStatus;
    private final Instant lockedUntil;
    private final Set<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.accountStatus = user.getAccountStatus();
        this.lockedUntil = user.getLockedUntil();
        this.authorities = new LinkedHashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getCode())));
        });
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.LOCKED
                && (lockedUntil == null || lockedUntil.isBefore(Instant.now()));
    }

    @Override
    public boolean isEnabled() {
        return accountStatus != AccountStatus.DISABLED;
    }
}
