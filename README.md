# obsdl-backend

基于 **Java 17 + Spring Boot 3.x** 的多模块后端工程，包含以下模块：

- `common`：公共 DTO、枚举、工具类
- `master`：任务编排服务（独立 Spring Boot 应用）
- `worker`：任务执行服务（独立 Spring Boot 应用）

## 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+

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

## 4. 本地启动命令

### 4.1 启动 master

```bash
mvn -pl master spring-boot:run
```

默认端口：`8080`

- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4.2 启动 worker

```bash
mvn -pl worker spring-boot:run
```

默认端口：`8081`

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
