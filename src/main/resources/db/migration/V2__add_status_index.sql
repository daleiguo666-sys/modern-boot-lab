-- V2：给 status 加索引。
--
-- 真实场景：GET /api/jpa/widgets/search 常按 status 过滤，量大时全表扫会慢，加索引避免。
--
-- 这是一次「纯 schema 优化」—— 不碰任何 Java 代码、不改实体。正是 Flyway 独立管 schema 的价值：
-- 这种变更可以单独作为一个迁移脚本上线，不需要改实体、不需要重新设计应用逻辑。
--
-- 它也是本节课用来演示「增量」的脚本：V1 跑过之后，重启应用 Flyway 只会执行这个 V2。
create index idx_widget_status on widget (status);
