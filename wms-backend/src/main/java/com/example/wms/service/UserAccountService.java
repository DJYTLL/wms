package com.example.wms.service;

import com.example.wms.dto.AuthPayload;
import com.example.wms.entity.UserAccount;
import org.springframework.security.core.userdetails.UserDetailsService;

// 用户服务接口：统一用户加载能力
public interface UserAccountService extends UserDetailsService {
    // 加载 JWT 需要的用户信息与权限
    AuthPayload loadAuthPayload(String username);

    // 读取用户权限版本
    long loadAuthVersion(String username);

    // 读取用户基础信息
    UserAccount loadUserAccount(String username);
}
