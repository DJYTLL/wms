package com.example.wms.dto;

/**

 * 刷新令牌请求参数，用于通过刷新令牌换取新的访问令牌。

 */
public record RefreshTokenRequest(
    /**
     * 表示刷新令牌。
     */
    String refreshToken
) {
}
