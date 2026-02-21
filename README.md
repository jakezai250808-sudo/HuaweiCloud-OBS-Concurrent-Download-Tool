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
