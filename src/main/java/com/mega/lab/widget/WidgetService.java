package com.mega.lab.widget;

import com.mega.lab.common.DuplicateResourceException;
import com.mega.lab.common.ResourceNotFoundException;
import com.mega.lab.widget.dto.CreateWidgetRequest;
import com.mega.lab.widget.dto.WidgetResponse;
import com.mega.lab.widget.dto.WidgetSummary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class WidgetService {

    private final WidgetRepository repository;

    // 构造器注入 —— 你已经在用，跳过讲解。唯一强调：不需要 @Autowired 了（单构造器自动注入）。
    public WidgetService(WidgetRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WidgetResponse> list() {
        return repository.findAll().stream()
                .map(WidgetResponse::from)
                .toList();                          // Java 16+ 的 Stream.toList()，替代 collect(Collectors.toList())
    }

    @Transactional(readOnly = true)
    public WidgetResponse get(Long id) {
        Widget w = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("widget %d 不存在".formatted(id)));
        return WidgetResponse.from(w);
    }

    // ===== ③ Specification：条件运行期才确定（搜索表单任意组合）=====
    // 它就是 JPA 版的 LambdaQueryWrapper：null 的条件不加进列表，自然不进 where。
    // 注意：老教程里的 Specification.where(null) 在 3.5 已废弃待删，2026 用 allOf(列表)。
    @Transactional(readOnly = true)
    public List<WidgetResponse> search(String sku, WidgetStatus status, Integer minQty) {
        List<Specification<Widget>> specs = new ArrayList<>();
        if (sku != null && !sku.isBlank()) {
            specs.add((root, query, cb) -> cb.equal(root.get("sku"), sku));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (minQty != null) {
            specs.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.<Integer>get("quantity"), minQty));
        }
        // allOf 把所有条件 AND 起来；列表为空时即「无限制」，等价查全部。
        return repository.findAll(Specification.allOf(specs)).stream()
                .map(WidgetResponse::from)
                .toList();
    }

    // ===== ① 派生查询示例：最新一条 =====
    @Transactional(readOnly = true)
    public WidgetResponse latest() {
        Widget w = repository.findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("还没有任何 widget"));
        return WidgetResponse.from(w);
    }

    // ===== ② @Query 示例：某状态且库存≥min =====
    @Transactional(readOnly = true)
    public List<WidgetResponse> inStock(WidgetStatus status, int min) {
        return repository.findInStock(status, min).stream()
                .map(WidgetResponse::from)
                .toList();
    }

    // ===== 接口投影示例：只返回 sku+quantity =====
    @Transactional(readOnly = true)
    public List<WidgetSummary> summaries(WidgetStatus status) {
        return repository.findSummaryByStatus(status);
    }

    @Transactional
    public WidgetResponse create(CreateWidgetRequest req) {
        if (repository.existsBySku(req.sku())) {
            throw new DuplicateResourceException("sku %s 已存在".formatted(req.sku()));
        }
        Widget saved = repository.save(
                new Widget(req.name(), req.sku(), req.quantity(), WidgetStatus.ACTIVE)
        );
        return WidgetResponse.from(saved);
    }
}
