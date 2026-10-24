# 实际验证记录（2026-10-24）

## 本机已执行

环境：Windows、Microsoft OpenJDK17.0.18、MySQL8.0.44、隔离Redis16379、原生三个Java进程、Vue开发端口15173、Microsoft Edge / Playwright。原有MySQL3306与历史uploads保持原样。

| 检查 | 结果与证据 |
|---|---|
| mvn verify | 38项通过，0失败/错误/跳过；market33、notice3、gateway2；[test-results.json](test-results.json) |
| 认证 | 伪造身份头、缺失CSRF、会话轮换、改密/停用/角色修改失效、注册不能提权 |
| 交易 | 双买家并发只有一个活动意向；同买家同key20并发返回同ID、异参数409；确认/完成/取消约束及reserved_order_id保护 |
| 回滚 | 订单插入失败、条件预留失败、真实MySQL约束注入的Outbox写失败均无残留订单/预留；收藏计数失败关系回滚 |
| 收藏/缓存 | 重复和并发PUT/DELETE保持关系与计数一致；个人favorite标记不跨用户；收藏写接口不返回非公开商品详情 |
| 分页和素材 | 原13列表遍历、过滤、空页/末页、排序与参数校验；78商品图和42轮播资源校验 |
| 上传 | 正常PNG、伪造格式、伪WebP、空文件、超大文件、像素炸弹及匿名请求 |
| 原生HTTP闭环 | 三个独立Java进程真实路由和Feign，另启第二market验证共享Session；[http-validation.json](http-validation.json) |
| 通知故障 | 停止notice时购买意向继续提交，实测一次约0.018秒；恢复后持久Outbox投递；这不是性能基准 |
| Redis认证故障 | 不可用Redis实例下已有会话请求503；新建匿名CSRF会话同样503 |
| Feign超时 | 受控5秒HTTP端点下，实际Spring配置的Feign约3.099秒返回，只有1次HTTP尝试，无自动重试；[feign-timeout-result.json](feign-timeout-result.json) |
| Redis实时断连恢复 | 已登录会话建立连接后停止/恢复Redis，断连503且恢复后原会话可用；[live-redis-result.json](live-redis-result.json) |
| MySQL重启 | 隔离实例正常关闭/重启前后用户、商品、订单和金额相同；[mysql-restart-result.json](mysql-restart-result.json) |
| H2→MySQL演练 | 旧提交已知fixture重建临时H2后导出/导入独立MySQL，七表行数/金额/ID/图片与原字段校验通过；[fixture-migration-report.json](fixture-migration-report.json) |
| Python迁移检查 | 4项通过：无昵称推断、显式映射、冲突保留、SQL值编码 |
| Fetch自检 | CSRF轮换、同源Cookie、FormData、查询参数、204及401/403/409/503处理通过 |
| 前端 | npm run build通过；升级Vite8.3.5后npm audit报告0漏洞；构建仍有Element Plus整包体积提示 |
| 浏览器 | 注册/重新登录/管理员后台通过；登录刷新恢复、伪造localStorage拒绝、站内通知、错误保留旧内容、390px无页面横溢、后台补投列表通过，页面JS错误0 |
| 轮播回归 | 14页面/42不同图片，悬停及焦点后继续播放、暂停/恢复和页面切换恢复均通过 |

截图：[桌面集市](screenshots/market.png)、[小屏集市](screenshots/market-mobile.png)、[站内通知](screenshots/notifications.png)、[通知故障保留内容](screenshots/notifications-outage.png)、[管理概览](screenshots/admin.png)、[Outbox](screenshots/outbox.png)。全部来自真实页面，没有生成替代截图。

## 自动化交付

GitHub Linux runner 已通过两项独立任务：`integration` 验证 Maven、Vue 构建、真实 MySQL / Redis / Java 进程及浏览器回归；`compose` 构建并启动六个服务，检查通知停机恢复、MySQL 重启持久化及 Redis 故障返回 503。

[已通过的 main 分支验收记录](https://github.com/17elr/campus-secondhand-system/actions/runs/40074759924)对应提交 `e56d56d6d891d45d0f5ad045b578c948b7ed96a0`，任务日志与截图产物可从该记录下载。[当前 CI 状态](https://github.com/17elr/campus-secondhand-system/actions/workflows/ci.yml)随后续提交更新。

本机没有 Docker，容器验收由 GitHub runner 实际执行。未执行生产部署、正式压测、TLS 终端验证、生产备份恢复或真实用户历史恢复；这些检查不代表生产 SLA。
