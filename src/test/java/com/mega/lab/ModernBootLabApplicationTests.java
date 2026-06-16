package com.mega.lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基本的「上下文能否启动」冒烟测试。
 * 用默认 profile（H2 内存库），不需要 Docker，所以归在普通单元测试里、随 ./gradlew build 一起跑。
 * 别小看它：很多配置/依赖装配的错误，这一个测试就能拦住。
 */
@SpringBootTest
class ModernBootLabApplicationTests {

    @Test
    void contextLoads() {
    }
}
