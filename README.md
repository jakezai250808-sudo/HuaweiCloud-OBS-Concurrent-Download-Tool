# obsdl-backend

基于 **Java 17 + Spring Boot 3.x** 的多模块后端工程，包含以下模块：

- `common`：公共 DTO、枚举、工具类
- `master`：任务编排服务（独立 Spring Boot 应用）
- `worker`：任务执行服务（独立 Spring Boot 应用）

## 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+（生产/默认模式）

## 2. 项目结构

```text
obsdl-backend
├── pom.xml
├── common
├── master
├── worker
└── db/init.sql
```

## 3. 构建命令

在项目根目录执行：

```bash
mvn -q -DskipTests package
```

## 3.1 一键运行 API 测试（含接口模拟 + H2 数据库校验）

```bash
./scripts/run-master-api-tests.sh
```

说明：
- 测试位于 `master/src/test/java/com/obsdl/master/controller`。
- 使用 `MockMvc` 模拟调用 `accounts/tasks/workers/obs` 全部 API。
- 使用 `JdbcTemplate` 对 H2 内存库执行增删改查并断言关键字段（如 `download_task`、`task_object`）正确性。

## 3.2 API 文档导出（自动更新）

- 导出目录：`docs/api/openapi.json`
- 导出来源：`master` 服务的 `GET /v3/api-docs`
- 自动更新方式：执行 `mvn -pl master test` 时，`OpenApiDocExportTest` 会自动覆盖更新文档文件。

## 4. 本地启动命令

### 4.1 启动 master

```bash
mvn -pl master -am spring-boot:run
```

默认端口：`8080`

- Swagger UI: `http://localhost:8080/swagger-ui.html`

#### Demo 运行模式（内存数据库）

如需快速体验，可启用 `demo` profile，自动切换为 H2 内存数据库并初始化建表 SQL（无需本地 MySQL）：

```bash
mvn -pl master -am spring-boot:run -Dspring-boot.run.profiles=demo
```

### 4.2 启动 worker

```bash
mvn -pl worker -am spring-boot:run
```

默认端口：`8081`

> 说明：仓库已在 `.mvn/maven.config` 中默认启用 `-am`，因此即使直接使用 `mvn -pl master spring-boot:run` 也会自动联动构建依赖模块。

> 说明：`master`/`worker` 依赖同仓库内的 `common` 模块；若你本地禁用了 `.mvn/maven.config` 默认参数，请手动加 `-am`（also make）避免 `com.obsdl:common` 解析失败。

## 5. 配置示例

### 5.1 master 配置（`master/src/main/resources/application.yml`）

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/obsdl?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 5.2 worker 配置（`worker/src/main/resources/application.yml`）

```yaml
server:
  port: 8081
```

## 6. 数据库初始化方式

执行以下命令初始化数据库和表：

```bash
mysql -uroot -proot < db/init.sql
```

> `demo` profile 下会自动执行 `master/src/main/resources/db/demo-init.sql`，无需手动初始化。

## 7. 示例接口

### 7.1 master 创建任务

```bash
curl -X POST 'http://localhost:8080/api/tasks' \
  -H 'Content-Type: application/json' \
  -d '{
    "accountId": 1,
    "bucket":"example-bucket",
    "selection": {
      "objects": [
        "path/to/file-1.zip",
        "path/to/file-2.zip"
      ]
    }
  }'
```

响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "taskId": 1001
  }
}
```

> `selection.prefix` 目前尚未支持递归展开对象，后续会补充。

### 7.2 worker 接收任务

```bash
curl -X POST 'http://localhost:8081/api/v1/worker/tasks' \
  -H 'Content-Type: application/json' \
  -d '{
    "bucket":"example-bucket",
    "objectKey":"path/to/file.zip",
    "concurrency":8
  }'
```

## 8. Worker 执行模式说明

worker 已支持 register/heartbeat/lease/download/rsync/report 主流程，运行方式见 `worker/README.md`。

## 9. ROS L1 Web 控制后端（master）

新增接口前缀：`/api/v1/ros`，用于启动/停止 rosbag(ROS1)/ros2 bag(ROS2) 播放和 rosbridge websocket。

### 9.1 依赖准备

- 安装 ROS1/ROS2（例如 noetic + humble）
- 安装 rosbridge_server 包
- Linux 环境执行（进程管理依赖 `bash/setsid/kill`）

### 9.2 配置项（master）

`master/src/main/resources/application.yml` / `application-demo.yml`：

```yaml
control:
  ros1-setup: /opt/ros/noetic/setup.bash
  ros2-setup: /opt/ros/humble/setup.bash
  log-out: /tmp/rosctl.out
  log-err: /tmp/rosctl.err
  host-for-ws-url: ""
  token-cache-ttl-seconds: 60
```

### 9.3 Token 改为数据库读取

`X-CTRL-TOKEN` 不再来自固定配置，而是查询数据库表 `api_token`（仅 `enabled=1` 生效）。服务端带 60 秒 TTL 本地缓存，避免每次请求查库。

默认初始化 token：`change_me`（请务必上线前修改）。

示例 SQL：

```sql
SELECT token FROM api_token WHERE enabled=1;
UPDATE api_token SET token='xxx' WHERE name='default';
INSERT INTO api_token(token,name,enabled) VALUES('xxx','default',1);
```

### 9.4 API 调用示例

```bash
curl -X POST 'http://localhost:8080/api/v1/ros/start' \
  -H 'Content-Type: application/json' \
  -H 'X-CTRL-TOKEN: change_me' \
  -d '{
    "rosVersion": "ROS1",
    "bagPath": "/abs/path/to/demo.bag",
    "loop": true,
    "useSimTime": true,
    "rate": 1.0,
    "port": 9090
  }'

curl -X GET 'http://localhost:8080/api/v1/ros/status' -H 'X-CTRL-TOKEN: change_me'
curl -X POST 'http://localhost:8080/api/v1/ros/stop' -H 'X-CTRL-TOKEN: change_me'
```

- `status` 会校验 PID 存活；若关键进程退出则返回 `STOPPED` 且 message=`Process not alive`。
- 返回 `wsUrl`（默认自动选择机器可达 IP，且多网卡时优先 10 网段，例如 `ws://10.10.1.23:9090`）；如需固定域名/IP，可配置 `control.host-for-ws-url`。

## 10. Docker 部署（构建镜像 + 启动脚本）

### 10.1 构建镜像

```bash
./scripts/build-images.sh
```

可选自定义镜像名：

```bash
MASTER_IMAGE=myrepo/obsdl-master:1.0 WORKER_IMAGE=myrepo/obsdl-worker:1.0 ./scripts/build-images.sh
```

可选自定义基础镜像（Dockerfile 第一行 `FROM`）：

```bash
BASE_IMAGE=eclipse-temurin:17-jre ./scripts/build-images.sh
```

若需分别指定 master/worker：

```bash
MASTER_BASE_IMAGE=eclipse-temurin:17-jre WORKER_BASE_IMAGE=eclipse-temurin:17-jre ./scripts/build-images.sh
```

脚本支持以下打包模式（`BUILD_JAR`）：
- `auto`（默认）：若 jar 缺失，或检测到不是可执行 Spring Boot jar（manifest 无 `Start-Class`），则自动执行 Maven 打包+repackage
- `always`：总是执行 Maven 打包+repackage
- `never`：不执行 Maven（适合你这种 Windows 打包、Ubuntu 仅构建镜像；但要求已存在可执行 Spring Boot jar）

示例（Ubuntu 仅构建镜像，不跑 Maven）：

```bash
BUILD_JAR=never ./scripts/build-images.sh
```

脚本会构建两张镜像：
- `scripts/docker/master.Dockerfile`
- `scripts/docker/worker.Dockerfile`

如果日志出现：

```
no main manifest attribute, in /app/app.jar
```

说明放进镜像的是普通 jar（非 Spring Boot 可执行 jar）。请在打包机执行：

```bash
mvn -pl master -am -DskipTests package spring-boot:repackage
mvn -pl worker -am -DskipTests package spring-boot:repackage
```

或直接在构建机上使用：

```bash
BUILD_JAR=always ./scripts/build-images.sh
```

如果遇到如下报错：

```
exec: "java": executable file not found in $PATH
```

说明构建时使用了不包含 JRE 的基础镜像。请使用带 Java 运行时的基础镜像重新构建：

```bash
BASE_IMAGE=eclipse-temurin:17-jre ./scripts/build-images.sh
```

`start-master-demo.sh` 现在会在启动前检查镜像中是否存在 `java`，并给出提示。

### 10.2 一键启动 MySQL + master + worker

```bash
./scripts/start-stack.sh
```

默认行为：
- 启动 `mysql:8.0` 容器并挂载 `db/init.sql` 自动初始化
- 启动 `obsdl/master:latest`（连接容器内 MySQL）
- 启动 `obsdl/worker:latest`（`MASTER_URL` 指向 master 容器）

常用环境变量：
- `MYSQL_ROOT_PASSWORD`（默认 `root`）
- `MYSQL_DATABASE`（默认 `obsdl`）
- `MASTER_PORT`（默认 `8080`）
- `WORKER_PORT`（默认 `8081`）
- `MYSQL_PORT`（默认 `3306`）
- `HOST_BIND`（默认 `0.0.0.0`，允许本机 IP 访问）
- `ACCESS_HOST`（默认自动取本机首个 IP，用于输出访问地址）

停止：

```bash
docker rm -f obsdl-worker obsdl-master obsdl-mysql
```

### 10.3 仅启动 demo 模式 master（内存 H2）

```bash
./scripts/start-master-demo.sh
```

支持通过 `HOST_BIND` 绑定监听地址（默认 `0.0.0.0`），支持通过 `ACCESS_HOST` 指定输出访问地址：

```bash
HOST_BIND=0.0.0.0 ACCESS_HOST=192.168.1.10 ./scripts/start-master-demo.sh
```

脚本会等待容器启动（默认 `STARTUP_WAIT_SECONDS=10`）；若容器异常退出（如 `Exited (1)`），会自动打印最近日志并返回失败码。

访问：
- master: `http://<你的主机IP>:8080`
- H2 Console: `http://<你的主机IP>:8080/h2-console`
