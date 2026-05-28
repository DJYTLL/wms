package com.example.wms.service;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.entity.UserAccount;

// 刷新令牌服务接口
public interface RefreshTokenService {
    // 生成登录令牌对
    TokenPairResponse issueTokens(UserAccount user, AuthPayload payload);

    // 使用刷新令牌获取新令牌对
    TokenPairResponse refresh(String refreshToken);

    // 撤销指定刷新令牌
    void revoke(String refreshToken);

    // 撤销用户的全部刷新令牌
    void revokeByUserId(Long userId);
}
