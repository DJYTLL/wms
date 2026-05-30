package com.example.wms;

import com.example.wms.dto.ApiResponse;
import com.example.wms.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void maxUploadSizeExceededReturnsPayloadTooLargeWithConfiguredLimitMessage() throws Exception {
        ExceptionHandlerMethodResolver resolver =
            new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);
        Method method = resolver.resolveMethod(new MaxUploadSizeExceededException(1048576));

        assertThat(method).isNotNull();

        GlobalExceptionHandler handler = new GlobalExceptionHandler("30MB");
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<Void>> response =
            (ResponseEntity<ApiResponse<Void>>) method.invoke(handler, new MaxUploadSizeExceededException(1048576));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(413);
        assertThat(response.getBody().message()).contains("文件过大").contains("30MB");
    }
}
