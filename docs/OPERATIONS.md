# 运行、备份与故障处理

## 启停与配置

`python scripts/create_env.py`以独占写方式创建.env，不覆盖现有配置，不输出凭据；随机值适用于MySQL初始化脚本要求的字符集。`docker compose up -d --build`启动，`docker compose stop/start`停止/恢复。mysql-data、redis-data与uploads是独立命名卷。MySQL/Redis主机端口仅127.0.0.1；market/notice未发布端口。HTTP开发使用SameSite=Lax、HttpOnly；HTTPS部署设置COOKIE_SECURE=true，并在同域反向代理终止TLS。

正式环境保持默认profile，不启用demo。初始管理员可将已注册账号经受控SQL改为ADMIN并递增auth_version；之后角色调整走管理员API并留痕。不要通过注册请求创建管理员。

## 一致备份

暂停写入或停止market，保持MySQL运行，使用mysqldump的single-transaction和routines/triggers导出market、notice，并备份同一时点uploads卷。为数据库、图片归档生成SHA256和时间标签；凭据不写进命令历史或公开文件。

在新卷/新schema演练恢复，验证七表行数、金额、ID映射和图片访问后再切换配置。原备份与旧卷保持只读，禁止用清理卷命令完成回退。孤立图片只能先出清单，本项目没有自动清理历史图片的逻辑。

## 故障

| 现象 | 检查与处理 |
|---|---|
| 登录或me返回503 | 检查Redis地址、密码、连接和日志；恢复共享Session服务，不降级到客户端身份 |
| 通知页503但交易成功 | 检查notice进程及内部凭据，页面保留旧内容；交易无需重做 |
| Outbox PENDING/PROCESSING | 查看next_run_at、attempt和租约；确认notice可达、凭据一致 |
| Outbox FAILED | 最多3次后停止；管理员在“通知投递”说明原因补投，事件ID不变 |
| 进程中途退出 | 租约过期后重新认领；最终尝试租约过期标记FAILED，由管理员核对 |
| 商品预留409 | 刷新当前数据；不要再次生成新的幂等键来绕过冲突 |
| 历史归属待核对 | 先导出报告、查证映射及活动订单冲突，保留只读，不改昵称解决 |
| MySQL连接中断 | 恢复数据库后等待连接池重建；不要切回内存库 |
| 图片上传400 | 检查5MB、格式、实际解码和2000万像素上限；WebP由前端转换后验证 |

定时任务每批最多20事件，租约15秒，连接/读取超时1/3秒；容量超出该小规模实现时再增加批量并行与投递指标。last_error只记录HTTP状态或异常类别，不保存内部token。

## 回退

代码旧基线为d650859979324881753cf306c9c1cf2b8549de6c。保留该提交和本轮全部提交，使用新的分支/还原提交回退代码，不重写远端历史。数据升级没有自动down脚本：旧版不理解seller_id/Session/新状态，不应直接接新库；恢复到备份的独立数据库和对应uploads副本后再切换连接。回退会话前使旧Session失效并重新登录，避免把新旧认证混用。

本机此次验证使用额外13319/16379实例，原有3306实例与用户上传文件未更改。旧源码、Git bundle、uploads备份单独存放于工作区外，未上传原始用户数据和本机凭据。
