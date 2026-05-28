package com.example.wms.dto;

/**

 * 令牌对响应对象，用于返回访问令牌和刷新令牌。

 */
public record TokenPairResponse(
    /**
     * 表示访问令牌。
     */
    String token,
    /**
     * 表示刷新令牌。
     */
    String refreshToken,
    /**
     * 表示当前登录态对应的鉴权上下文。
     */
    AuthPayload authPayload
) {
}
