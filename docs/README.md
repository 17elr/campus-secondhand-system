# 拾光集市 · 文档中心

[返回项目首页](../README.md) · [页面预览](GALLERY.md) · [GitHub CI](https://github.com/17elr/campus-secondhand-system/actions/workflows/ci.yml)

从运行项目到理解实现，每份文档各回答一个问题。

## 开始使用

| 你想做什么 | 从这里开始 |
| --- | --- |
| 在本机启动并体验 | [快速上手](QUICKSTART.md)：环境、Compose、演示账号和端口 |
| 先看看实际页面 | [页面预览](GALLERY.md)：集市、手机布局、管理后台和通知 |
| 修改代码、运行测试 | [本地开发](DEVELOPMENT.md)：三个 Java 模块、Vue、环境变量和验证命令 |

## 理解实现

| 文档 | 内容 |
| --- | --- |
| [系统设计](系统设计方案.md) | 服务边界、Session 鉴权、交易状态机、Outbox 投递 |
| [API 与权限](API.md) | 浏览器接口、内部通知契约、参数和权限要求 |
| [数据字典](DATA_DICTIONARY.md) | 两个 schema、业务表、约束和索引 |
| [验证记录](VALIDATION.md) | 实际测试、浏览器回归、故障恢复与 CI 证据 |

## 迁移与维护

| 文档 | 内容 |
| --- | --- |
| [数据迁移](MIGRATION.md) | 导出、显式身份映射、冲突报告、导入和校验 |
| [运行维护](OPERATIONS.md) | 启停、备份、排障和回退 |
| [案例迁移报告](fixture-migration-report.json) | 七张表的行数、金额、ID 与图片路径校验 |

## 项目讲解

- [面试说明](INTERVIEW.md)：技术选择、事务边界、通知幂等和已验证范围。
- [简历项目介绍](简历项目介绍.md)：与实际代码一致的项目描述。

交易记录只表达购买意向与线下交接状态；系统不处理真实资金。
