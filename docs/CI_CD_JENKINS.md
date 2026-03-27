# Jenkins CI/CD 使用说明（本项目）

本文档对应仓库根目录 `Jenkinsfile`，用于实现：

1. 自动拉代码
2. 构建后端 `app.jar`
3. 构建前端 `dist`
4. 自动部署 ECS（后端 + 前端）
5. 健康检查

---

## 1. 前置准备

### 1.1 Jenkins 节点依赖

- JDK 17
- Maven 3.8+
- Node.js 18+（建议 20）
- npm
- rsync
- ssh/scp

### 1.2 ECS 固定路径（与你当前部署一致）

- 后端 jar: `/opt/tennis-court-booking/runtime/app.jar`
- 后端服务名: `tennis-court-booking`
- 前端静态目录: `/var/www/tennis/dist/`

### 1.3 Jenkins 凭据

在 Jenkins `Credentials` 新增：

- 类型: `SSH Username with private key`
- ID: `ecs-ssh`（与 Jenkinsfile 默认一致）
- 用户名: `root`（或你的部署用户）
- 私钥: 对应可登录 ECS 的私钥

---

## 2. Jenkinsfile 关键配置

在仓库根目录 `Jenkinsfile` 的 `environment` 中按实际修改：

- `DEPLOY_HOST`：ECS 公网 IP
- `DEPLOY_USER`：ECS 登录用户
- `SSH_CREDENTIALS_ID`：Jenkins 凭据 ID
- `BACKEND_JAR_LOCAL`：本地构建后的 jar 路径
- `BACKEND_JAR_REMOTE`：ECS 目标 jar 路径
- `BACKEND_SERVICE`：systemd 服务名
- `FRONTEND_DIST_REMOTE`：ECS 前端目录

---

## 3. Jenkins 任务配置

推荐 `Pipeline` 类型任务：

1. New Item -> Pipeline
2. Pipeline definition 选 `Pipeline script from SCM`
3. 选择 Git 仓库
4. Script Path 填 `Jenkinsfile`
5. Save 后 `Build Now`

---

## 4. 触发方式

`Jenkinsfile` 已带参数：

- `DEPLOY=true`：构建后自动部署
- `DEPLOY=false`：只构建不部署

可先用 `DEPLOY=false` 验证构建稳定，再开启自动部署。

---

## 5. 常见问题

### 5.1 前端更新后线上没变化

- 确认 `Deploy Frontend` 阶段成功
- 确认 `rsync --delete` 成功覆盖 `dist`
- 浏览器强制刷新（Ctrl/Cmd + Shift + R）

### 5.2 后端服务启动失败

在 ECS 执行：

```bash
sudo journalctl -u tennis-court-booking -f -n 150
```

### 5.3 SSH 连接失败

- 检查 Jenkins 凭据是否正确
- 检查安全组和 ECS 防火墙
- 检查部署用户是否有目标目录和 systemctl 权限

---

## 6. 可选增强（后续）

- 按分支部署（dev/staging/prod）
- 加入 `input` 人工审批后再部署生产
- 钉钉/企业微信通知
- 自动回滚（保留最近 N 个 jar）

