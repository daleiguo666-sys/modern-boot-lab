package com.mega.lab.widget;

import com.mega.lab.widget.dto.WidgetSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 查询的三个层次都在这里示范。配套接口/Service：
 *   - WidgetService.search(...)  用 Specification（动态条件）
 *   - WidgetController 暴露 /search /latest /in-stock /summaries 供 curl 体验
 *
 * 多继承了 JpaSpecificationExecutor<Widget> —— 这是启用 Specification 动态查询的开关。
 */
public interface WidgetRepository
        extends JpaRepository<Widget, Long>, JpaSpecificationExecutor<Widget> {

    // ========== ① 派生查询：方法名即 SQL（启动时解析方法名生成实现） ==========

    Optional<Widget> findBySku(String sku);                       // where sku = ?
    boolean existsBySku(String sku);                              // select count(*)>0 ... where sku = ?

    List<Widget> findByStatus(WidgetStatus status);              // where status = ?
    List<Widget> findByNameContainingIgnoreCase(String keyword); // where lower(name) like %?%
    List<Widget> findByQuantityGreaterThan(int min);            // where quantity > ?

    Optional<Widget> findFirstByOrderByCreatedAtDesc();          // order by created_at desc limit 1（最新一条）

    // ========== ② @Query：方法名撑不住时自己写（JPQL，面向实体/字段，非表/列） ==========

    @Query("select w from Widget w where w.status = :status and w.quantity >= :min")
    List<Widget> findInStock(@Param("status") WidgetStatus status, @Param("min") int min);

    // ========== ③ 接口投影：只 SELECT sku、quantity 两列 ==========
    // 方法名里 find 和 By 之间的 "Summary" 只是描述、会被忽略；真正决定投影的是返回类型。
    List<WidgetSummary> findSummaryByStatus(WidgetStatus status);
}
