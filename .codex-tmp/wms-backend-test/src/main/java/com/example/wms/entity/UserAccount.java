package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// 登录用户实体：用于 JWT 与权限体系
@TableName("app_user")
public class UserAccount extends TenantAuditableSoftDeleteEntity {
    // 登录名
    @TableField("username")
    private String username;

    // 密码哈希（BCrypt）
    @TableField("password_hash")
    private String passwordHash;

    // 显示名
    @TableField("display_name")
    private String displayName;

    // 邮箱
    @TableField("email")
    private String email;

    // 电话
    @TableField("phone")
    private String phone;

    // 头像地址
    @TableField("avatar_url")
    private String avatarUrl;

    // 是否启用
    @TableField("is_enabled")
    private boolean enabled = true;

    // 账号未过期
    @TableField("account_non_expired")
    private boolean accountNonExpired = true;

    // 账号未锁定
    @TableField("account_non_locked")
    private boolean accountNonLocked = true;

    // 凭证未过期
    @TableField("credentials_non_expired")
    private boolean credentialsNonExpired = true;

    // 权限版本（权限变更时递增）
    @TableField("auth_version")
    private long authVersion;

    // 最近登录时间
    @TableField("last_login_at")
    private Instant lastLoginAt;

    // 备注
    @TableField("remark")
    private String remark;

    // 用户拥有的角色（非表字段）
    @TableField(exist = false)
    private Set<Role> roles = new HashSet<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(long authVersion) {
        this.authVersion = authVersion;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
