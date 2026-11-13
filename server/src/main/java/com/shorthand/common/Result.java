package com.shorthand.common;

import lombok.Data;

@Data
public class Result {
    private int code;
    private String message;
    private Object data;

    public static Result ok() {
        return ok(null);
    }

    public static Result ok(Object data) {
        Result r = new Result();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static Result fail(int code, String message) {
        Result r = new Result();
        r.code = code;
        r.message = message;
        return r;
    }
}
