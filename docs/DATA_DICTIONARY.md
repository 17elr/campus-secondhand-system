# 数据字典

MySQL 8.0 / utf8mb4。升级SQL位于 `server/src/main/resources/db/migration/` 和 `notice-service/src/main/resources/db/migration/`，由Flyway记录版本。没有启动DROP或默认演示覆盖逻辑。金额为DECIMAL(10,2)，接口校验0—99999999.99、最多两位小数；历史创建时间保留TIMESTAMP(6)。

| schema.表 | 关键字段与约束 | 用途 |
|---|---|---|
| market.sys_user | id PK、username唯一、password_hash、nickname、role、status、auth_version、created_at | 用户身份；nickname仅展示，密码哈希不序列化 |
| market.goods | id PK、seller_id FK、seller_name快照、title、price、category、condition_level、location、description、image_url、status、favorite_count、reserved_order_id、version、ownership_verified | 商品；reserved_order_id逻辑关联当前意向；归属未核对不可写 |
| market.trade_order | id PK、goods_id、buyer_id/seller_id FK、title/price/location/昵称快照、status、request_key、request_hash、ownership_verified | 成交意向快照；(buyer_id,request_key)唯一 |
| market.trade_order | active_goods_id生成列唯一、created_at、confirmed_at、completed_at、cancelled_at、cancel_reason | 已核对的PENDING/CONFIRMED占活动商品唯一值；其他状态为NULL |
| market.favorite | (user_id,goods_id)联合PK、created_at | 收藏关系；关系和计数同事务 |
| market.want_post | id PK、user_id、requester_name快照、title、category、budget、description、status、created_at | 求购，归属以user_id为准 |
| market.goods_message | id PK、goods_id、user_id、user_name快照、content、reply_content、reply_name快照、status、created_at、replied_at | 数据库咨询与卖家/管理员回复 |
| market.report | id PK、reporter_id FK、reporter_name快照、target_type、target_id、reason、status、created_at | 举报；历史未映射ID保持NULL |
| market.outbox_event | event_id UUID PK、recipient_id、type、business_id、payload JSON、status、attempt、next_run_at、lease_until、lease_owner、last_error、created_at | 可恢复通知交付；状态PENDING/PROCESSING/DELIVERED/FAILED |
| market.operation_log | id PK、actor_id、action、target_id、reason、created_at | 管理强制关闭、内容处理、角色与账号变更和补投留痕 |
| market.demo_seed | id PK、created_at | 仅demo profile的幂等导入标记 |
| notice.notification | id PK、event_id、recipient_id、type、business_id、title、body、occurred_at、payload_hash、is_read | (event_id,recipient_id)唯一；recipient_id是外部ID，无跨库外键 |

索引包括商品status/category/id、订单buyer_id/id与seller_id/id、求购/咨询user_id/id、Outbox状态/下次执行/租约、通知接收人/id。MySQL各schema用户无其他schema授权，notice不能读取market表。

Redis：Session前缀 `market:session:`；详情缓存 `market:goods:v1:{id}`，TTL30—40秒。测试会话前缀独立，且使用Redis逻辑库1。公开缓存不含密码、联系方式、审核原因或个人收藏标记，个人收藏单独查询后组合。
