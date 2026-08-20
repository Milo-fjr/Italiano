package com.italiano.vocab.dto;

import lombok.Data;

/** 统一响应包装：code=0 成功 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setCode(0);
        r.setMessage("ok");
        r.setData(data);
        return r;
    }

    public static ApiResponse<Void> error(String message) {
        ApiResponse<Void> r = new ApiResponse<>();
        r.setCode(1);
        r.setMessage(message);
        return r;
    }
}
