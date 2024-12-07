package com.dtc.impeller.flow;

import java.util.Map;

public class Result<T, U extends Throwable> {
    private T success;

    private U error;

    public Result(T s, U e){
        this.success = s;
        this.error = e;
    }

    public boolean isOk(){
        return error == null;
    }

    public static <T,U extends Throwable>Result<T,U> ok(T success){
        return new Result<T, U>(success, null);
    }

    public static <T,U extends Throwable>Result<T,U> error(U error){
        return new Result<T, U>(null, error);
    }

    public Map<String,Object> ok(){
        return Map.of("output",success);
    }

    public U error(){
        return error;
    }
}
