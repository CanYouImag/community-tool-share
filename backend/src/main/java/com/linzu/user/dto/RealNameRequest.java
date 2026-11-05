package com.linzu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RealNameRequest(
        @NotBlank(message = "真实姓名不能为空")
        @Size(min = 2, max = 20, message = "真实姓名长度需为 2-20 位")
        String realName,

        @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
        String idCardNo
) {
}
