# 数据迁移与身份核对

## 先保全原数据

先停止业务写入，备份数据库导出和uploads整个目录，记录文件校验值。不要删除旧库、用户目录或图片。已结束的H2 mem进程不能恢复不存在的持久数据。本轮未发现旧运行实例，实际演练使用旧提交中可证明的fixture，并未声称恢复历史用户数据。

旧版建表结构保留在Flyway V1及Git旧基线中；原demo-data、pagination-data、image-refresh和图片来源清单继续保留。默认运行不再加载H2、H2console或演示SQL。

## 导出活动H2

必须从仍可访问原数据库的进程连接导出：内存库只存在于原JVM，单独启动一个同名mem连接不会读取原实例。如果已有H2 console，可在原进程的连接里用CSVWRITE导出下列七张表；如果已有受控TCP连接，也可从该连接导出。

`sys_user, goods, trade_order, favorite, want_post, goods_message, report`。每个CSV包含原表全部列，UTF-8、第一行列名，NULL编码设为 `\N`（一个反斜杠加N），不要把空字符串当NULL。导出前后暂停写入，保证七张表快照一致。

```sql
CALL CSVWRITE('/private/legacy-export/sys_user.csv', 'SELECT * FROM sys_user', 'charset=UTF-8 nullString=\N');
```

H2版本的CSV选项应先在副本确认；也可用H2工具Csv.setNullString("\\N")导出。密码哈希只保留在私有原始导出中，不放进迁移报告或GitHub。

## 默认只审计

```sh
python scripts/migrate_legacy.py /private/legacy-export --report /private/migration-report.json
```

审计包含七表行数、金额总计、图片路径、源文件SHA256、无效关联、未核对商品/订单ID及同商品活动订单冲突。不会通过昵称自动选择账号，也不会删除冲突记录。

明确身份映射文件示例：

```json
{
  "goods": {"10": 3},
  "trade_order": {"20": {"buyerId": 1, "sellerId": 3}},
  "report": {"5": 1}
}
```

映射必须来自原可靠用户ID或经本人/管理员核对的凭证。重名、改名和无法核对的记录保持只读。存在多个活动订单或商品预留状态不一致时，整组商品与活动意向不启用；保留原金额、ID、状态与图片路径，先在副本核对。

## 导入空MySQL

先在新schema启动market默认profile，让Flyway创建结构；停掉该实例，然后导入。不要启用demo profile。MySQL默认时区与JDBC均使用Asia/Shanghai；V3保留历史微秒时间精度。

```sh
# 将密码私下设置到当前终端 MYSQL_PWD，不在命令行参数中打印。
python scripts/migrate_legacy.py /private/legacy-export --mapping /private/verified-mapping.json --report /private/import-report.json --database market --apply
```

工具使用mysql CLI，可通过--mysql提供程序路径。只允许空业务schema，SQL值使用UTF-8十六进制编码，七表插入、显式映射、预留关系和收藏计数在同一事务内完成。提交前用CHECK保护表校验原始行数和所有原字段（收藏计数按真实关系纠正）；任一失败停止批处理并回滚。提交后再比对七表ID、关联ID、金额和图片路径。已有导入可用--verify-only重新验证，不覆盖任何业务数据。

如果已有MySQL旧表：先输出同样报告和备份；在隔离副本使用一次性的 `--spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=1` 由V2/V3升级。默认配置不启用自动baseline，避免把未知库误判为旧版。随后通过显式核对映射再启用历史记录；不要在真实库直接试脚本。

## 本轮演练结果

[fixture-migration-report.json](fixture-migration-report.json)来自旧提交SQL重建的临时H2，经CSV导出后导入隔离MySQL schema并重新核验：36用户、78商品、33意向、43收藏、30求购、40咨询、31举报。商品总金额3724.90，意向快照总金额1919.00，求购预算总额2706.00；这些是fixture字段之和，不是成交额或支付额。

实际fixture未发现重复活动订单或无法映射身份。冲突保护另有自动化测试；不能将代码风险推断写成发生过的数据冲突。没有删除或默认转移任何原记录。

[H2 CSVWRITE官方说明](https://h2database.com/html/functions.html#csvwrite)。导出工具仅用于迁移，不属于项目运行/测试依赖。
