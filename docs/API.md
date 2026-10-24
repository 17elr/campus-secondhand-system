# API与权限

响应通常为 `{code,message,data}`；分页data为 `{records,total,page,size}`。通知分页另带unread。请求/响应金额为DECIMAL对应数值，后端计算使用BigDecimal。错误：401未登录/会话失效，403越权/CSRF，404不存在/不可见，409状态或幂等冲突，400参数错误，503认证/通知服务不可用。

所有写接口带Cookie及X-CSRF-Token。先GET csrf得到匿名会话，再POST login/register，登录后重新GET csrf。无需且不信任X-User-Id/X-User-Role。

| 方法/路径 | 参数/数据 | 权限与行为 |
|---|---|---|
| GET /api/auth/csrf | 无 | 创建/取当前会话CSRF token |
| POST /api/auth/login | username,password | 验证BCrypt、更换会话 |
| POST /api/auth/register | username,password,nickname | 只注册USER并登录 |
| GET /api/auth/me | 无 | 当前数据库身份 |
| POST /api/auth/logout | 无 | 销毁当前会话 |
| PUT /api/auth/profile | nickname | 修改本人显示昵称 |
| PUT /api/auth/password | oldPassword,newPassword | 修改后失效全部旧会话 |
| GET /api/goods | PageQuery | 匿名在售列表 |
| GET /api/goods/mine | PageQuery | 本人ID归属商品 |
| GET /api/goods/favorites | PageQuery | 本人收藏 |
| GET /api/goods/{id} | 无 | 公开在售；其他状态需交易参与人、卖家或管理员 |
| POST /api/goods | title,price,category,conditionLevel,location,description,imageUrl | 登录发布，卖家ID由服务端设置；201 |
| PUT /api/goods/{id} | 同上 | 卖家且ON_SALE/OFF_SHELF、归属已核对 |
| PATCH /api/goods/{id}/status | value=ON_SALE/OFF_SHELF | 无活动预留的本人商品 |
| PUT/DELETE /api/goods/{id}/favorite | 无 | 设置收藏/取消收藏，重复调用幂等 |
| POST /api/orders | goodsId,requestKey（查询参数） | 买家发起意向，同key同参数返回原结果 |
| GET /api/orders/mine | PageQuery | 本人买入或卖出的意向 |
| PATCH /api/orders/{id}/confirm | 无 | 卖家确认PENDING |
| PATCH /api/orders/{id}/cancel | 无 | 买家取消或卖家拒绝PENDING |
| PATCH /api/orders/{id}/complete | 无 | 买家完成CONFIRMED线下交接 |
| GET /api/messages/goods/{goodsId} | PageQuery | 可见商品的公开咨询 |
| GET /api/messages/mine | PageQuery,onlyMine | 本人发出或本人商品收到的咨询 |
| POST /api/messages | goodsId,content | 1—500字，服务端设置提问人 |
| PATCH /api/messages/{id}/reply | content | 卖家按ID回复 |
| GET /api/wants | PageQuery,onlyMine | 公开已发布及本人记录 |
| POST /api/wants | title,category,budget,description | 登录提交审核 |
| PUT /api/wants/{id} | 同上 | 本人编辑后回到待审核 |
| PATCH /api/wants/{id}/status | value=FOUND/WITHDRAWN | 本人合法状态转换 |
| POST /api/reports | targetType,targetId,reason | 登录举报，操作者ID服务端设置 |
| POST /api/uploads/image | multipart file | 登录；PNG/JPEG，5MB、2000万像素 |
| GET /api/notifications | page,size | 登录后market以可信ID通过Feign查询 |
| PUT /api/notifications/{id}/read | 无 | 校验接收人；重复已读幂等 |
| GET /api/admin/overview | 无 | 管理员数据概览 |
| GET /api/admin/{goods,users,orders,reports,messages,wants} | PageQuery | 原六个管理列表；概览复用goods分页 |
| PATCH /api/admin/{goods,users,orders,reports,wants}/{id}/status | value,reason | 管理员Service白名单、状态检查、操作留痕；订单仅CANCELLED |
| PATCH /api/admin/users/{id}/role | value=USER/ADMIN,reason | 管理员；修改角色递增authVersion |
| PATCH /api/admin/messages/{id}/reply | content | 管理员回复并留痕/通知 |
| GET /api/admin/outbox | page,size | 投递状态、尝试次数、错误类别 |
| POST /api/admin/outbox/{eventId}/retry | reason | 仅FAILED事件安排补投 |
| GET /api/health | 无 | 初始化完成后200 |

PageQuery：page=1—1000000、size=1—100（默认9）、keyword、category、status、condition、minPrice/maxPrice、sort=latest/priceAsc/priceDesc/popular、onlyMine。不同列表只应用相关筛选项；所有SQL均使用参数绑定。

## notice内部契约

只在本机或Compose内部网络提供；需要至少32字符的NOTICE_INTERNAL_TOKEN，使用X-Internal-Token传递，不下发浏览器。

| 方法/路径 | 数据/结果 |
|---|---|
| PUT /internal/notifications/{eventId} | recipientId,type,businessId,title,body,occurredAt；同事件同接收人同内容返回同ID，异内容409 |
| GET /internal/users/{userId}/notifications | page,size；返回records,total,unread,page,size |
| PUT /internal/users/{userId}/notifications/{id}/read | 本人归属校验，重复调用幂等 |

内部eventId为UUID；occurredAt为ISO 8601时间。notice不跨库查询用户。无凭据403、他人通知404。网关不路由这些路径。
