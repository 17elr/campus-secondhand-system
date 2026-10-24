# 本地开发与验证

[项目首页](../README.md) · [文档中心](README.md) · [快速上手](QUICKSTART.md)

## 开发环境

Java 17、Maven、Node.js 22.12+、Python 3，以及独立的 MySQL 8.0 / Redis。CI 使用 Java 17 和 Node.js 22。

保留原目录：`server` 即 market-service，`gateway` 和 `notice-service` 为独立 Maven 模块，`web` 使用 Vue 3 / JavaScript。

## 环境变量

可以先通过 Compose 启动基础设施：

```sh
docker compose up -d mysql redis
```

在当前终端设置 `.env` 对应的 `MARKET_DB_PASSWORD`、`NOTICE_DB_PASSWORD`、`REDIS_PASSWORD` 和 `NOTICE_INTERNAL_TOKEN`。Java 进程不会自动读取根目录 `.env`。

| 变量 | 默认值或用途 |
| --- | --- |
| `MARKET_DB_URL` | `jdbc:mysql://127.0.0.1:13319/market?serverTimezone=Asia/Shanghai` |
| `MARKET_DB_USER` | `market` |
| `NOTICE_DB_URL` | `jdbc:mysql://127.0.0.1:13319/notice?serverTimezone=Asia/Shanghai` |
| `NOTICE_DB_USER` | `notice` |
| `REDIS_HOST` / `REDIS_PORT` | `127.0.0.1` / `16379` |
| `NOTICE_INTERNAL_TOKEN` | market 与 notice 使用同一个高熵内部凭据 |
| `SPRING_PROFILES_ACTIVE` | 默认不导入案例；空演示库可设为 `demo` |

在 `.env` 中更新数据库密码不会自动改写已有 MySQL 用户密码；已有环境应按 [运行维护](OPERATIONS.md) 处理。

## 构建与启动

先构建三个 Java 模块，再分别在三个终端启动：

```sh
mvn -B -ntp -Dmaven.test.skip=true package
java -jar notice-service/target/notice-service-1.0.0-SNAPSHOT.jar
java -jar server/target/market-service-1.0.0-SNAPSHOT.jar
java -jar gateway/target/gateway-1.0.0-SNAPSHOT.jar
```

默认开发端口：notice `18085`、market `18084`、gateway `18080`，均绑定本机。上传保存位置受工作目录与 `UPLOAD_DIR` 影响；在不同终端启动时应使用相同且稳定的工作目录，或将 `UPLOAD_DIR` 设为固定绝对目录。

启动前端：

```sh
npm --prefix web ci
npm --prefix web run dev
```

浏览器访问 `http://127.0.0.1:15173`。Vite 固定同源代理到网关，登录使用 Cookie 与 CSRF，不需要自传身份头。

## 自动化验证

Java 测试需要独立 `market_test` / `notice_test` schema 及 Redis。建库过程与环境配置见 [CI 工作流](../.github/workflows/ci.yml) 和 [测试数据库 SQL](../infra/ci-databases.sql)。测试会新增数据，请使用隔离测试库。上述 SQL 面向全新 CI 环境；已有开发环境应单独创建测试 schema 和授权，不要整段重复初始化。

本地测试通过 `TEST_MARKET_DB_URL` / `TEST_MARKET_DB_USER` / `TEST_MARKET_DB_PASSWORD`、`TEST_NOTICE_DB_URL` / `TEST_NOTICE_DB_USER` / `TEST_NOTICE_DB_PASSWORD` 指向测试库；`TEST_REDIS_HOST` / `TEST_REDIS_PORT` 指向隔离 Redis。使用带密码的 Redis 时也需设置 `REDIS_PASSWORD`。

```sh
mvn -B -ntp verify
python -m unittest discover -s scripts -p test_migration.py
node scripts/test_api.mjs
npm --prefix web run build
```

设置隔离数据库连接后，可运行真实进程联调；脚本自行启停 Java 服务，要求相关端口可用：

```sh
python scripts/smoke_services.py
```

对专用 Compose 测试栈运行故障检查：

```sh
python scripts/smoke_services.py --compose
```

HTTP 回归会创建案例商品和意向；Compose 故障检查会停止/恢复通知服务、重启 MySQL 和 Redis。实际通过的检查与局限见 [验证记录](VALIDATION.md)。

## 浏览器与素材

安装 Playwright 的环境可以传入已有模块路径：

```sh
node scripts/check_migration_ui.cjs <playwright模块路径>
node scripts/check_auth_ui.cjs <playwright模块路径>
node scripts/check_banner_ui.cjs <playwright模块路径>
```

页面回归包含登录恢复、权限、通知、管理后台和小屏布局；注册回归会增加测试账号。原 78 张商品图、42 张轮播图与来源清单保留在 `web/public/images/`。日常运行使用本地素材，不需要重新下载图片。
