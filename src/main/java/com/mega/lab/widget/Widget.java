package com.mega.lab.widget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA 实体 —— 映射 widget 表。
 *
 * 相比你 2.x 时代的几个关键代差：
 *  1. 包名是 jakarta.persistence.*，不再是 javax.persistence.*（Boot 3 = Jakarta EE 9+）。
 *     这是从 2.x 升 3.x 时编译报错最多的地方，全局把 javax. 换成 jakarta. 即可（除了 javax.sql 等少数仍在 JDK 里的）。
 *  2. @Version 乐观锁：高并发写场景（你游戏后台很需要）防丢更新，比手写 version 字段判断省心。
 *  3. 实体不要用 record（JPA 要求无参构造 + 可变字段做代理），所以这里仍用类 + Lombok。
 *     —— 这是“能不能用 record”的一条重要边界：DTO 用 record，JPA 实体不用。
 */
@Entity
@Table(name = "widget")
@Getter
@Setter
@NoArgsConstructor
public class Widget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)              // 存字符串而非序号，枚举顺序变了也不会错乱
    @Column(nullable = false, length = 16)
    private WidgetStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Widget(String name, String sku, Integer quantity, WidgetStatus status) {
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = Instant.now();
    }
}
