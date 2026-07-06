# MEMORY — 长期记忆

## 基础设施

### MySQL Docker (192.168.1.103 / mes-dev)

| 项目 | 值 |
|------|-----|
| 容器名 | mysql-mes |
| 数据卷 | /data/mysql/ |
| 端口映射 | 3306:3306 |
| root 密码 | 见 /data/mysql/.root_password |
| 应用用户 | mes / mes123 |
| 内存约束 | 宿主机 1.9G，innodb_buffer_pool=256M |
| 参考 | memory/2026-06-24.md |

### InfluxDB Docker (192.168.1.103 / mes-dev)

| 项目 | 值 |
|------|-----|
| 容器名 | influxdb |
| 版本 | v2.9.1 |
| 数据卷 | /data/influxdb |
| 端口映射 | 8086:8086 |
| 管理 UI | http://192.168.1.103:8086 |
| 用户名 | admin |
| 密码 | influxAdmin123 |
| 组织 | mes |
| Bucket | mes-data |
| 数据保留 | 30天 |
| 参考 | memory/2026-07-02.md |

### SSH 主机

- **mes-dev** → 192.168.1.103, user: wgq, sudo 密码: root
  - OS: Ubuntu 20.04.6 LTS
  - 密钥: ~/.ssh/id_rsa

### mes-query-api (192.168.1.103)

| 项目 | 值 |
|------|-----|
| 项目路径 | /data/projects/mes-query-api |
| 端口 | 8088 |
| 技术栈 | Spring Boot 2.7.18 + Java 11 |
| 数据库 | ddcoreprd (MySQL) |
| 启动方式 | `nohup java -jar target/mes-query-api-1.0.0.jar > app.log 2>&1 &` |
| 构建 | `mvn package -DskipTests -q` |
| 源码 | Controller: TableController.java / OrderController.java, Service: TableService.java / OrderService.java |
| 发布流程 | 见 memory/2026-06-25.md |
| 提交流程 | 见 memory/2026-06-25.md |
| 编码规范 | 见 /data/projects/mes-query-api/CONTRIBUTING.md，本地副本 skills/mes-api-reference/CONTRIBUTING.md |
| 数采接口 | GET /api/dc/electricity（电流）、GET /api/dc/voltage（电压），dc_* 大表不支持通用表查询 |
| mapping文件 | 开发者说的"mapping文件"指的是**小生（agent:mes）**的记忆文件：/home/wgq/.openclaw/workspace-mes/memory/table-mapping.md |
| 小生 | agent:mes，workspace: /home/wgq/.openclaw/workspace-mes/，负责 MES 系统查询 |

## Promoted From Short-Term Memory (2026-06-29)

<!-- openclaw-memory-promotion:memory:memory/2026-06-24.md:14:17 -->
- MySQL 配置建议（基于 1.5G 可用内存）: innodb_buffer_pool_size: 256M; innodb_log_file_size: 64M; max_connections: 100; 保守配置，留足 OS 和 swap 余量 [score=0.857 recalls=0 avg=0.620 source=memory/2026-06-24.md:14-17]
<!-- openclaw-memory-promotion:memory:memory/2026-06-24.md:9:10 -->
- MySQL Docker 容器配置决策: **端口映射：** -p 3306:3306; **宿主机资源：** 内存 1.9G，可用 ~1.5G，Swap 2G [score=0.857 recalls=0 avg=0.620 source=memory/2026-06-24.md:9-10]
<!-- openclaw-memory-promotion:memory:memory/2026-06-24.md:5:8 -->
- MySQL Docker 容器配置决策: **目标宿主机：** 192.168.1.103 (mes-dev); **数据目录：** /data/mysql/（已创建，属主 wgq:wgq）; **root 密码：** 随机生成 16 位，存放于 /data/mysql/.root_password; **应用用户：** mes / mes123 [score=0.857 recalls=0 avg=0.620 source=memory/2026-06-24.md:5-8]

## Promoted From Short-Term Memory (2026-06-30)

<!-- openclaw-memory-promotion:memory:memory/2026-06-24.md:21:24 -->
- 执行结果: [x] Docker run 成功; 容器名：mysql-mes; 内存限制：512m; my.cnf 已挂载，配置验证通过 [score=0.893 recalls=0 avg=0.620 source=memory/2026-06-24.md:21-24]
<!-- openclaw-memory-promotion:memory:memory/2026-06-24.md:25:28 -->
- 执行结果: buffer_pool: 256M, max_connections: 100, charset: utf8mb4; root 密码：/data/mysql/.root_password; mes 用户密码：mes123; 警告：内核不支持 swap limit（`--memory-swap` 无效），但不影响运行 [score=0.893 recalls=0 avg=0.620 source=memory/2026-06-24.md:25-28]
<!-- openclaw-memory-promotion:memory:memory/2026-06-25.md:7:9 -->
- 发布流程: `mvn package -DskipTests -q` →; `kill <旧pid>` →; `nohup java -jar target/mes-query-api-1.0.0.jar > app.log 2>&1 &` → [score=0.893 recalls=0 avg=0.620 source=memory/2026-06-25.md:7-9]
<!-- openclaw-memory-promotion:memory:memory/2026-06-25.md:24:25 -->
- 编码规范: 见 /data/projects/mes-query-api/CONTRIBUTING.md; 本地副本：skills/mes-api-reference/CONTRIBUTING.md [score=0.861 recalls=0 avg=0.620 source=memory/2026-06-25.md:24-25]
<!-- openclaw-memory-promotion:memory:memory/2026-06-25.md:19:21 -->
- 本次修改: `TableController.java` / `TableService.java`：新增 sort/order 参数支持排序; sort 默认 CREATE_TIME，order 默认 desc; sort 列名通过白名单校验 [score=0.861 recalls=0 avg=0.620 source=memory/2026-06-25.md:19-21]
<!-- openclaw-memory-promotion:memory:memory/2026-06-25.md:13:16 -->
- 提交流程: `git add -A`; `git commit -m "feat/fix/refactor: <描述>"`（遵循 CONTRIBUTING.md 提交规范）; `git push origin master`; Remote: `git@github.com:WuKouChiao/mes-query.git` [score=0.861 recalls=0 avg=0.620 source=memory/2026-06-25.md:13-16]

## Promoted From Short-Term Memory (2026-07-06)

<!-- openclaw-memory-promotion:memory:memory/2026-07-02.md:10:11 -->
- InfluxDB Docker 部署 (192.168.1.103): 用户名: admin; 密码: influxAdmin123 [score=0.857 recalls=0 avg=0.620 source=memory/2026-07-02.md:10-11]
<!-- openclaw-memory-promotion:memory:memory/2026-07-02.md:13:15 -->
- InfluxDB Docker 部署 (192.168.1.103): Bucket: mes-data; 数据保留: 30天; API token 需要从 UI 获取或通过 CLI 生成 [score=0.857 recalls=0 avg=0.620 source=memory/2026-07-02.md:13-15]
<!-- openclaw-memory-promotion:memory:memory/2026-07-02.md:4:7 -->
- InfluxDB Docker 部署 (192.168.1.103): 容器名: influxdb; 镜像: influxdb:2 (v2.9.1); 数据目录: /data/influxdb; 端口: 8086 [score=0.857 recalls=0 avg=0.620 source=memory/2026-07-02.md:4-7]
