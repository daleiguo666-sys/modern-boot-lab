package com.mega.lab.widget.dto;

import com.mega.lab.widget.Widget;
import com.mega.lab.widget.WidgetStatus;

import java.time.Instant;

/**
 * 响应 DTO，也用 record。
 *
 * 用静态工厂 from() 做“实体 → DTO”的映射，把映射逻辑收在 DTO 自己身上。
 * 真实项目里更多用 MapStruct（编译期生成映射代码，零反射），但小项目手写 from() 更直观，先不引入。
 */
public record WidgetResponse(
        Long id,
        String name,
        String sku,
        int quantity,
        WidgetStatus status,
        Instant createdAt
) {
    public static WidgetResponse from(Widget w) {
        return new WidgetResponse(
                w.getId(), w.getName(), w.getSku(),
                w.getQuantity(), w.getStatus(), w.getCreatedAt()
        );
    }
}
