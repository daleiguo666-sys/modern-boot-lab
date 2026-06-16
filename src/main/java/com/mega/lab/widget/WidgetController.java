package com.mega.lab.widget;

import com.mega.lab.widget.dto.CreateWidgetRequest;
import com.mega.lab.widget.dto.WidgetResponse;
import com.mega.lab.widget.dto.WidgetSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * JPA 版接口。用 Spring Data JPA 操作 widget 表。
 *
 * 注解代差：@Tag/@Operation 来自 io.swagger.core.v3（springdoc 用的 OpenAPI 3 注解），
 * 替代你老项目的 io.swagger.annotations.@Api/@ApiOperation（springfox 的 Swagger 2 注解）。
 *
 * 另一个值得注意的现代化点：这里用标准 REST 语义（GET 查、POST 建、201 + Location 头），
 * 不像你游戏后台习惯全用 POST。两种风格都能用，但教学项目按“最标准”来。
 */
@Tag(name = "Widget (JPA)", description = "用 Spring Data JPA 操作 widget 表")
@RestController
@RequestMapping("/api/jpa/widgets")
public class WidgetController {

    private final WidgetService service;

    public WidgetController(WidgetService service) {
        this.service = service;
    }

    @Operation(summary = "列出所有 widget")
    @GetMapping
    public List<WidgetResponse> list() {
        return service.list();
    }

    // ① Specification 动态搜索：三个参数任意组合，缺的不进 where
    //   curl 'localhost:8080/api/jpa/widgets/search?status=ACTIVE&minQty=5'
    @Operation(summary = "动态条件搜索（sku/status/minQty 任意组合）")
    @GetMapping("/search")
    public List<WidgetResponse> search(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) WidgetStatus status,
            @RequestParam(required = false) Integer minQty) {
        return service.search(sku, status, minQty);
    }

    // ② 派生查询：最新一条   curl localhost:8080/api/jpa/widgets/latest
    @Operation(summary = "最新创建的一个 widget")
    @GetMapping("/latest")
    public WidgetResponse latest() {
        return service.latest();
    }

    // ③ @Query：某状态且库存≥min  curl 'localhost:8080/api/jpa/widgets/in-stock?min=1'
    @Operation(summary = "某状态且库存≥min 的 widget")
    @GetMapping("/in-stock")
    public List<WidgetResponse> inStock(
            @RequestParam(defaultValue = "ACTIVE") WidgetStatus status,
            @RequestParam(defaultValue = "1") int min) {
        return service.inStock(status, min);
    }

    // ④ 接口投影：只返回 sku+quantity  curl 'localhost:8080/api/jpa/widgets/summaries?status=ACTIVE'
    @Operation(summary = "投影：只返回 sku 和 quantity")
    @GetMapping("/summaries")
    public List<WidgetSummary> summaries(
            @RequestParam(defaultValue = "ACTIVE") WidgetStatus status) {
        return service.summaries(status);
    }

    @Operation(summary = "按 id 查询 widget")
    @GetMapping("/{id}")
    public WidgetResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "创建 widget")
    @PostMapping
    public ResponseEntity<WidgetResponse> create(@Valid @RequestBody CreateWidgetRequest req) {
        WidgetResponse created = service.create(req);
        // 201 Created + Location 头指向新资源，是 REST 创建语义的标准做法
        return ResponseEntity
                .created(URI.create("/api/jpa/widgets/" + created.id()))
                .body(created);
    }
}
