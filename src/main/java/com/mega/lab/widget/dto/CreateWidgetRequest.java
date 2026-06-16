package com.mega.lab.widget.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 请求 DTO 用 record —— 替代你过去的 `@Data class XxxPO`。
 *
 * record 的好处：天然不可变、自带构造/getter/equals/hashCode/toString，一行声明。
 * 校验注解直接写在 record component 上，Spring 的 @Valid 完全支持。
 *
 * 注意：record 的“getter”是 name() 而非 getName()，Jackson 反序列化用规范构造器，都能正常工作。
 */
public record CreateWidgetRequest(

        @NotBlank(message = "name 不能为空")
        @Size(max = 64)
        String name,

        @NotBlank
        @Pattern(regexp = "[A-Z0-9-]{3,64}", message = "sku 必须是大写字母/数字/横杠，长度 3-64")
        String sku,

        @Min(value = 0, message = "quantity 不能为负")
        int quantity
) {
}
