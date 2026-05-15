package com.example.wms.dto;

/**

 * 统一接口响应体，用于包装接口返回的状态码、消息和业务数据。

 */
public record ApiResponse<T>(
    /**
     * 表示业务编码。
     */
    int code,
    /**
     * 表示接口返回消息。
     */
    String message,
    /**
     * 表示接口返回的业务数据。
     */
    T data
) {
    // 成功响应快捷方法
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "ok", data);
    }

    // 错误响应快捷方法
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
