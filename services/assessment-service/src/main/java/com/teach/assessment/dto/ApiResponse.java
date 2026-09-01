package com.teach.assessment.dto;

public class ApiResponse<T> {
    private int code; private String message; private T data;
    public static <T> ApiResponse<T> ok(T data) { return ok("success", data); }
    public static <T> ApiResponse<T> ok(String message, T data) { ApiResponse<T> r=new ApiResponse<>(); r.code=200; r.message=message; r.data=data; return r; }
    public static <T> ApiResponse<T> fail(int code,String message) { ApiResponse<T> r=new ApiResponse<>(); r.code=code; r.message=message; return r; }
    public static <T> ApiResponse<T> fail(String message) { return fail(400,message); }
    public int getCode(){return code;} public String getMessage(){return message;} public T getData(){return data;}
    public void setCode(int v){code=v;} public void setMessage(String v){message=v;} public void setData(T v){data=v;}
}
