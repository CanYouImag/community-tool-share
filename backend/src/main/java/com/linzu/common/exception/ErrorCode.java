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

    // ===== 社区与治理域（钟姝伶）=====
    COMMUNITY_NAME_EXISTS(1101, "社区名称已存在", HttpStatus.BAD_REQUEST),
    ALREADY_IN_COMMUNITY(1102, "已是该社区成员", HttpStatus.BAD_REQUEST),
    JOIN_APPLY_EXISTS(1103, "已有待处理的加入申请", HttpStatus.BAD_REQUEST),
    COMMUNITY_HAS_ACTIVE_ORDER(1104, "该社区下存在进行中订单，暂不能操作", HttpStatus.BAD_REQUEST),
    ALREADY_DISPUTED(1105, "该订单已发起过纠纷，不能重复发起", HttpStatus.BAD_REQUEST),
    DISPUTE_STATE_INVALID(1106, "纠纷状态不允许该操作", HttpStatus.BAD_REQUEST),
    ARBITRATION_EXISTS(1107, "该纠纷已裁决", HttpStatus.BAD_REQUEST),
    DEDUCT_EXCEEDS_DEPOSIT(1108, "扣除金额不能超过押金", HttpStatus.BAD_REQUEST),
    REPORT_DUPLICATE(1109, "该目标已有待处理的举报", HttpStatus.BAD_REQUEST),
    COMMUNITY_ADMIN_EXISTS(1110, "该社区已有管理员", HttpStatus.BAD_REQUEST),
    USER_NOT_CERTIFIED(1111, "用户未通过实名认证", HttpStatus.BAD_REQUEST),
    USER_ALREADY_ADMIN(1112, "该用户已是其他社区的管理员", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(2001, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(2002, "无权限执行该操作", HttpStatus.FORBIDDEN),
    ACCOUNT_OR_PASSWORD_ERROR(2003, "账号或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_BANNED(2004, "账号已被封禁", HttpStatus.FORBIDDEN),
    NOT_COMMUNITY_ADMIN(2005, "不是社区管理员", HttpStatus.FORBIDDEN),

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
    COMMUNITY_NOT_FOUND(3012, "社区不存在", HttpStatus.NOT_FOUND),
    JOIN_APPLY_NOT_FOUND(3013, "加入申请不存在", HttpStatus.NOT_FOUND),
    COMMUNITY_MEMBER_NOT_FOUND(3014, "社区成员不存在", HttpStatus.NOT_FOUND),
    COMMUNITY_ANNOUNCEMENT_NOT_FOUND(3015, "公告不存在", HttpStatus.NOT_FOUND),
    DISPUTE_NOT_FOUND(3016, "纠纷不存在", HttpStatus.NOT_FOUND),
    REPORT_NOT_FOUND(3017, "举报不存在", HttpStatus.NOT_FOUND),

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
