# Worker 运行说明

本模块实现了 worker 节点的自动拉取/下载/回传流程：

1. 启动后向 `MASTER_URL` 注册 (`/api/workers/register`)。
2. 定时发送心跳 (`/api/workers/heartbeat`)。
3. 后台循环执行 lease (`/api/tasks/{LEASE_TASK_ID}/lease`，批量 50)。
4. 并发下载到 `${CACHE_DIR}/${LEASE_TASK_ID}/${objectKey}.part`，下载成功后 rename 为正式文件。
5. 执行 rsync 回传到：
   `${MASTER_SSH}:${TARGET_PATH}/${TARGET_TMP_SUBDIR}/${LEASE_TASK_ID}/${objectKey}`。
6. 成功时 report `done`，失败时 report `failed` 并附带错误信息。

> 当前下载逻辑通过 `ObsClientFacade` 抽象；默认 `PlaceholderObsClientFacade` 会抛出未配置异常，便于后续替换为华为云 OBS Java SDK 实现。

## 必要环境变量

- `MASTER_URL`：master 服务地址，例如 `http://master:8080`
- `WORKER_ID`：worker 唯一标识
- `MAX_DL_CONCURRENCY`：最大下载并发
- `CACHE_DIR`：本地缓存目录
- `MASTER_SSH`：rsync 远端，如 `root@master-host`
- `TARGET_TMP_SUBDIR`：目标临时子目录（默认 `.obsdl_tmp`）

## 其他建议变量

- `LEASE_TASK_ID`：当前 worker 拉取的任务 ID
- `TARGET_PATH`：master 目标根目录
- `WORKER_PORT`：worker 端口

## Docker 运行示例

```bash
docker run --rm -d \
  --name obsdl-worker-01 \
  -p 8081:8081 \
  -e MASTER_URL=http://192.168.1.10:8080 \
  -e WORKER_ID=worker-01 \
  -e MAX_DL_CONCURRENCY=8 \
  -e CACHE_DIR=/data/obsdl-cache \
  -e MASTER_SSH=root@192.168.1.10 \
  -e TARGET_PATH=/data/downloads \
  -e TARGET_TMP_SUBDIR=.obsdl_tmp \
  -e LEASE_TASK_ID=1001 \
  -v /data/obsdl-cache:/data/obsdl-cache \
  obsdl-worker:latest
```
