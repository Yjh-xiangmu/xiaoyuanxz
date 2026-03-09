package com.example.backend.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 状态码：200代表成功，500代表失败
    private String msg;   // 提示信息
    private T data;       // 实际返回的数据

    // 成功时的快捷方法
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败时的快捷方法
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}