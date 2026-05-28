package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;

/**

 * 删除请求参数，用于接收执行删除操作时填写的原因说明。

 */
public record DeleteRequest(
    /**
     * 表示执行操作时填写的原因。
     */
    @NotBlank(message = "删除原因不能为空")
    String reason
) {
}
