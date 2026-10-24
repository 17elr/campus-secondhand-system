# 快速上手

[项目首页](../README.md) · [文档中心](README.md) · [本地开发](DEVELOPMENT.md)

使用 Docker Compose 启动完整系统，适合先体验页面和业务流程。

## 1. 准备环境

- Docker 与 Compose v2。
- Python 3，用于生成本机随机凭据。
- Git，用于获取源码。

```sh
git clone https://github.com/17elr/campus-secondhand-system.git
cd campus-secondhand-system
python scripts/create_env.py
```

脚本只创建尚不存在的 `.env`，不覆盖已有配置，也不打印生成的密码。

## 2. 选择初始数据

| 用途 | `.env` 中的设置 | 初次启动的行为 |
| --- | --- | --- |
| 本地演示 | `MARKET_PROFILES=demo` | 在空库中导入 36 个案例账号、78 件商品及原有分页案例 |
| 正式初始化 | 保持 `MARKET_PROFILES=` | 只执行数据库升级，不创建公开演示账号 |

`demo` 初始化只允许空数据库；已经导入后，重启沿用原数据。正式使用可通过注册页创建普通账号，再经受控数据库操作授予首个管理员，详见 [运行维护](OPERATIONS.md)。

## 3. 启动与体验

```sh
docker compose up -d --build
```

首次构建会下载 Maven / npm 依赖并编译三个 Java 模块。启动完成后，访问 [http://127.0.0.1:15173](http://127.0.0.1:15173)。

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 普通用户 | `student` | `123456` |
| 卖家示例 | `zhou` | `123456` |
| 管理员 | `admin` | `admin123` |

可从以下流程开始：浏览商品 → 留言咨询 → 发起购买意向 → 切换到对应卖家确认 → 买家确认线下交接。站内通知可从右上角铃铛查看。本系统不处理真实资金。

## 4. 地址与持久化

| 服务 | 本机入口 | 说明 |
| --- | --- | --- |
| 网页 | `127.0.0.1:15173` | 用户端与管理端，同源代理 API 和图片 |
| 网关 | `127.0.0.1:18080` | 统一业务入口，不路由 `/internal` |
| MySQL | `127.0.0.1:13319` | market / notice 两个 schema、不同账号 |
| Redis | `127.0.0.1:16379` | 共享 Session 与公开商品详情缓存 |
| market / notice | Compose 内部网络 | 不发布主机端口 |

MySQL、Redis、uploads 分别使用持久卷。日常停止和恢复：

```sh
docker compose stop
docker compose start
```

停止服务不需要删除卷。数据备份、文件备份与回退步骤见 [运行维护](OPERATIONS.md)。

## 5. 查看状态

```sh
docker compose ps
docker compose logs -f market gateway notice
```

如遇登录 503、通知不可用或商品状态冲突，可按 [故障处理表](OPERATIONS.md#故障) 检查。修改代码和运行测试请继续阅读 [本地开发](DEVELOPMENT.md)。
