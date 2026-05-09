package com.example.wms.dto;

// 登录令牌对响应
public record TokenPairResponse(
    String token,
    String refreshToken
) {
}
