package com.example.wms.security;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.UserClaim;
import com.example.wms.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// 可复用的已认证用户主体，避免重复回库组装用户上下文。
public class AuthenticatedUser implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UserAccount userAccount;
    private final AuthPayload authPayload;

    private AuthenticatedUser(Long userId,
                              String username,
                              String password,
                              boolean enabled,
                              boolean accountNonExpired,
                              boolean accountNonLocked,
                              boolean credentialsNonExpired,
                              Collection<? extends GrantedAuthority> authorities,
                              UserAccount userAccount,
                              AuthPayload authPayload) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.authorities = authorities == null ? List.of() : List.copyOf(authorities);
        this.userAccount = userAccount;
        this.authPayload = authPayload;
    }

    public static AuthenticatedUser fromDatabase(UserAccount userAccount,
                                                 AuthPayload authPayload,
                                                 Collection<? extends GrantedAuthority> authorities) {
        return new AuthenticatedUser(
            userAccount == null ? null : userAccount.getId(),
            userAccount == null ? null : userAccount.getUsername(),
            userAccount == null ? "" : userAccount.getPasswordHash(),
            userAccount != null && userAccount.isEnabled(),
            userAccount == null || userAccount.isAccountNonExpired(),
            userAccount == null || userAccount.isAccountNonLocked(),
            userAccount == null || userAccount.isCredentialsNonExpired(),
            authorities,
            userAccount,
            authPayload
        );
    }

    public static AuthenticatedUser fromToken(Long userId,
                                              String username,
                                              AuthPayload authPayload,
                                              Collection<? extends GrantedAuthority> authorities) {
        return new AuthenticatedUser(
            userId,
            username,
            "",
            true,
            true,
            true,
            true,
            authorities,
            null,
            authPayload
        );
    }

    public Long getUserId() {
        return userId;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public AuthPayload getAuthPayload() {
        return authPayload;
    }

    public UserClaim getUserClaim() {
        return authPayload == null ? null : authPayload.user();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
