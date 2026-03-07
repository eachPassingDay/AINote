package com.ainote.common;

public enum ErrorCodeEnum {
    // 绝对成功
    SUCCESS("00000", "SUCCESS"),

    // A级别：用户端错误
    USER_ERROR("A0001", "用户端错误"),
    USER_NOT_LOGGED_IN("A0200", "用户未登录"),
    USER_UNAUTHORIZED("A0300", "权限不足"),
    PARAM_ERROR("A0400", "系统请求参数错误"),
    NOTE_NOT_FOUND("A0404", "笔记不存在"),
    NOTE_ALREADY_DELETED("A0405", "笔记已被删除"),

    // B级别：系统执行出错
    SYSTEM_ERROR("B0001", "系统执行出错"),
    SYSTEM_TIMEOUT("B0100", "系统执行超时"),
    FILE_UPLOAD_ERROR("B0500", "文件上传失败"),

    // C级别：调用第三方服务出错
    THIRD_PARTY_ERROR("C0001", "调用第三方服务出错"),
    LLM_TIMEOUT("C0100", "大模型调用超时"),
    LLM_ERROR("C0101", "大模型调用失败"),
    TIKA_PARSE_ERROR("C0200", "内容脱水解析失败");

    private final String code;
    private final String msg;

    ErrorCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
