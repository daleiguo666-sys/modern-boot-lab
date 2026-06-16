package com.mega.lab;

import com.mega.lab.config.LabProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 演示两件事：
 *  1. LabProperties（@ConfigurationProperties record）的注入与使用。
 *  2. 虚拟线程是否生效：开启 spring.threads.virtual.enabled=true 后，
 *     每个 HTTP 请求会跑在虚拟线程上，currentThread().isVirtual() 返回 true。
 *     对你这种 IO 密集的游戏后台，虚拟线程几乎零成本地提升并发吞吐 —— 这是 Java 21 最大的一块红利。
 */
@Tag(name = "Home", description = "首页 / 运行时自检")
@RestController
public class HomeController {

    private final LabProperties props;

    public HomeController(LabProperties props) {
        this.props = props;
    }

    @Operation(summary = "查看欢迎语与当前线程类型（验证虚拟线程）")
    @GetMapping("/")
    public Map<String, Object> home() {
        Thread current = Thread.currentThread();
        return Map.of(
                "message", props.welcomeMessage(),
                "cacheTtl", props.cacheTtl().toString(),
                "thread", current.toString(),
                "isVirtualThread", current.isVirtual()   // 虚拟线程开启时为 true
        );
    }
}
