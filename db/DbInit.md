# 数据库初始化说明

## 1. 创建数据库和表
在项目根目录执行以下命令：

```bash
mysql -uroot -p < db/init.sql
```

该脚本会完成：
- 创建 `obsdl` 数据库（如不存在）。
- 创建 4 张核心表：`obs_account`、`download_task`、`task_object`、`worker_node`。
- 建立必要索引：
  - `task_object(task_id, status)` 组合索引。
  - `task_object(task_id, object_key)` 唯一索引。

## 2. 配置 master 数据源
参考 `master/src/main/resources/application.yml` 中的 JDBC 配置，修改为你的 MySQL 地址、账号和密码。

## 3. 启动 master 服务验证

```bash
mvn -pl master spring-boot:run
```

启动后可通过日志确认 MyBatis-Plus 成功连接数据库，并访问：

- Swagger UI: `http://localhost:8080/swagger-ui.html`
