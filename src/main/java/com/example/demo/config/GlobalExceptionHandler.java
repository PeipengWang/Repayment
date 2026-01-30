//package com.example.demo.config;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Data;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 全局异常处理器
// */
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    /**
//     * 处理参数校验异常
//     */
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException e) {
//        Map<String, String> errors = new HashMap<>();
//        e.getBindingResult().getAllErrors().forEach(error -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMsg = error.getDefaultMessage();
//            errors.put(fieldName, errorMsg);
//        });
//        return ResponseEntity.badRequest().body(new ApiResponse("400", "参数校验失败", errors));
//    }
//
//    /**
//     * 处理业务异常
//     */
//    @ExceptionHandler(BusinessException.class)
//    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException e) {
//        return ResponseEntity.badRequest().body(new ApiResponse("400", e.getMessage(), null));
//    }
//
//    /**
//     * 处理系统异常
//     */
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse> handleSystemException(Exception e) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ApiResponse("500", "系统异常：" + e.getMessage(), null));
//    }
//
//    /**
//     * 统一响应格式
//     */
//    @Data
//    @Schema(name = "ApiResponse", description = "统一接口响应格式")
//    public static class ApiResponse {
//        @Schema(description = "响应码")
//        private String code;
//        @Schema(description = "响应信息")
//        private String msg;
//        @Schema(description = "响应数据")
//        private Object data;
//
//        public ApiResponse(String code, String msg, Object data) {
//            this.code = code;
//            this.msg = msg;
//            this.data = data;
//        }
//    }
//
//    /**
//     * 自定义业务异常
//     */
//    @Data
//    public static class BusinessException extends RuntimeException {
//        private String code;
//        private String msg;
//
//        public BusinessException(String msg) {
//            super(msg);
//            this.code = "400";
//            this.msg = msg;
//        }
//
//        public BusinessException(String code, String msg) {
//            super(msg);
//            this.code = code;
//            this.msg = msg;
//        }
//    }
//}