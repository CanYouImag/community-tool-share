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
    REAL_NAME_FORMAT_ERROR(1007, "身份证号格式或校验位不正确", HttpStatus.BAD_REQUEST),
    REAL_NAME_EXISTS(1008, "已提交过实名认证", HttpStatus.BAD_REQUEST),
    WALLET_BALANCE_NOT_ENOUGH(1009, "可用余额不足", HttpStatus.BAD_REQUEST),
    WALLET_FROZEN_NOT_ENOUGH(1010, "冻结金额不足", HttpStatus.BAD_REQUEST),
    CAPTCHA_INVALID(1011, "验证码不正确或已过期", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1012, "图片大小超出限制", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_SUPPORTED(1013, "仅支持 JPG、PNG、GIF、WebP 图片", HttpStatus.BAD_REQUEST),
    RENT_PERIOD_INVALID(1014, "租期不合法：结束日期不能早于开始日期，且不能早于今天", HttpStatus.BAD_REQUEST),
    ITEM_IMAGE_TOO_MANY(1015, "物品图片最多 6 张", HttpStatus.BAD_REQUEST),
    ORDER_MESSAGE_EMPTY(1016, "留言内容不能为空", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(2001, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(2002, "无权限执行该操作", HttpStatus.FORBIDDEN),
    ACCOUNT_OR_PASSWORD_ERROR(2003, "账号或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_BANNED(2004, "账号已被封禁", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND(3001, "用户不存在", HttpStatus.NOT_FOUND),
    WALLET_NOT_FOUND(3002, "钱包不存在", HttpStatus.NOT_FOUND),
    STATE_CONFLICT(3003, "数据状态已变化，请重试", HttpStatus.CONFLICT),
    ITEM_NOT_FOUND(3004, "物品不存在", HttpStatus.NOT_FOUND),
    ITEM_NOT_AVAILABLE(3005, "物品当前不可租或不在架", HttpStatus.CONFLICT),
    CATEGORY_NOT_FOUND(3006, "分类不存在", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(3007, "订单不存在", HttpStatus.NOT_FOUND),
    ORDER_STATE_NOT_ALLOWED(3008, "订单当前状态不允许该操作", HttpStatus.CONFLICT),
    PICKUP_CODE_ERROR(3009, "取物码不正确", HttpStatus.BAD_REQUEST),
    DISPUTE_STATE_NOT_ALLOWED(3010, "订单当前不在纠纷中", HttpStatus.CONFLICT),
    CATEGORY_NAME_EXISTS(3011, "分类名称已存在", HttpStatus.BAD_REQUEST),

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
