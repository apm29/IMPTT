package com.imptt.apm29.api;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BaseResponse<T> {
    public String status;
    public String text;
    public String token;
    public T data;

    public boolean success() {
        return status != null && status.equals("1");
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "status='" + status + '\'' +
                ", text='" + text + '\'' +
                ", token='" + token + '\'' +
                ", data=" + data +
                '}';
    }

    public BaseResponse(String status, String text, String token, T data) {
        this.status = status;
        this.text = text;
        this.token = token;
        this.data = data;
    }

    public static <T> BaseResponse<List<T>> emptyList() {
        return new BaseResponse<List<T>>(
                "1", "请求失败", "", new ArrayList<T>()
        );
    }

    public static <T> BaseResponse<T> error() {
        return new BaseResponse<>(
                "0", "请求失败", "", null
        );
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(
                "0", "成功", "", data
        );
    }

    @NotNull
    public static BaseResponse<Object> fail(@NotNull Exception e) {
        return new BaseResponse<>(
                "0", "请求失败:" + e.getMessage(), "", null
        );
    }
}
