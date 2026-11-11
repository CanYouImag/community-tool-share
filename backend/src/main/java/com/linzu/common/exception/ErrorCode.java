package com.linzu.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok", HttpStatus.OK),

    PARAM_ERROR(1001, "参数校验失败", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTS(1002, "用户名已存在", HttpStatus.BAD_REQUEST),
    PHONE_EXISTS(1003, "手机号已被使用", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(1004, "两次密码不一致", HttpStatus.BAD_REQUEST),
    PASSWORD_WEAK(1005, "密码长度至少 6 位", HttpStatus.BAD_REQUEST),
    OLD_PASSWORD_ERROR(1006, "原密码不正确", HttpStatus.BAD_REQUEST),
    REAL_NAME_FORMAT_ERROR(1007, "身份证号格式不正确", HttpStatus.BAD_REQUEST),
    REAL_NAME_EXISTS(1008, "已提交过实名认证", HttpStatus.BAD_REQUEST),
    WALLET_BALANCE_NOT_ENOUGH(1009, "可用余额不足", HttpStatus.BAD_REQUEST),
    WALLET_FROZEN_NOT_ENOUGH(1010, "冻结金额不足", HttpStatus.BAD_REQUEST),

    // ===== 社区与治理域（钟姝伶）=====
    COMMUNITY_NAME_EXISTS(1011, "社区名称已存在", HttpStatus.BAD_REQUEST),
    ALREADY_IN_COMMUNITY(1012, "已是该社区成员", HttpStatus.BAD_REQUEST),
    JOIN_APPLY_EXISTS(1013, "已有待处理的加入申请", HttpStatus.BAD_REQUEST),
    COMMUNITY_HAS_ACTIVE_ORDER(1014, "该社区下存在进行中订单，暂不能操作", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(2001, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(2002, "无权限执行该操作", HttpStatus.FORBIDDEN),
    ACCOUNT_OR_PASSWORD_ERROR(2003, "账号或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_BANNED(2004, "账号已被封禁", HttpStatus.FORBIDDEN),
    NOT_COMMUNITY_ADMIN(2005, "不是社区管理员", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND(3001, "用户不存在", HttpStatus.NOT_FOUND),
    WALLET_NOT_FOUND(3002, "钱包不存在", HttpStatus.NOT_FOUND),
    STATE_CONFLICT(3003, "数据状态已变化，请重试", HttpStatus.CONFLICT),
    COMMUNITY_NOT_FOUND(3010, "社区不存在", HttpStatus.NOT_FOUND),
    JOIN_APPLY_NOT_FOUND(3011, "加入申请不存在", HttpStatus.NOT_FOUND),
    COMMUNITY_MEMBER_NOT_FOUND(3012, "社区成员不存在", HttpStatus.NOT_FOUND),
    COMMUNITY_ANNOUNCEMENT_NOT_FOUND(3013, "公告不存在", HttpStatus.NOT_FOUND),

    SYSTEM_ERROR(5000, "系统错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
