package com.example.wms.dto;

// 统一响应包装
public record ApiResponse<T>(int code, String message, T data) {
    // 成功响应快捷方法
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "ok", data);
    }

    // 错误响应快捷方法
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
