# BudgetPilot 部署手册

## 项目概览

BudgetPilot 是一个个人预算管理系统，替代 Actual Budget，部署于家庭 Linux 服务器。

### 技术栈
- **后端**: Spring Boot 3.3 + Java 21 + MyBatis-Plus + MySQL 8.0 + Redis 7
- **前端**: Vue 3 + Vite + Naive UI + ECharts + PWA
- **通知**: Telegram Bot API
- **部署**: Docker Compose + Nginx 反向代理

### 端口规划
| 服务 | 端口 | 说明 |
|------|------|------|
| API (Spring Boot) | 6060 | 后端服务 |
| MySQL | 3307 (映射到容器 3306) | 仅 127.0.0.1 监听 |
| Redis | 6380 (映射到容器 6379) | 仅 127.0.0.1 监听 |
| Nginx | 80 / 443 | 前端静态 + API 反向代理（可选） |

> **注意**：80 和 8080 端口已被其他服务占用，API 使用 6060 端口。

---

## 一、环境准备

### 1.1 检查系统要求

```bash
# 查看系统信息
uname -a

# 查看 CPU 核心数
nproc

# 查看内存（最低 2GB，推荐 4GB+）
free -h

# 查看磁盘空间（需要 10GB+）
df -h /
```

### 1.2 安装 Docker

```bash
# 一键安装脚本
curl -fsSL https://get.docker.com | sh

# 将当前用户加入 docker 组（避免每次 sudo）
sudo usermod -aG docker $USER

# 重新登录使组生效（或执行）
newgrp docker

# 验证安装
docker --version
docker compose version
```

### 1.3 安装构建依赖（Java 21 + Maven + Node.js）

如果你使用 Docker Compose 部署（推荐），后端通过 Dockerfile 构建，**不需要**宿主机安装 Java/Maven。但前端需要 Node.js 来构建。

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-21-jdk maven nodejs npm

# CentOS/Rocky
sudo dnf install -y java-21-openjdk-devel maven nodejs

# 验证
java -version          # 应为 21.x
mvn --version          # 应为 3.9+
node --version         # 应为 18+
```

如果服务器没有包管理器提供 Java 21，可以用 SDKMAN：

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.5-tem
sdk install maven
```

### 1.4 安装 Node.js（如系统版本过低）

```bash
# 使用 nvm 安装 Node 20
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc   # 或 source ~/.zshrc
nvm install 20
node --version     # 应显示 v20.x.x
```

---

## 二、部署步骤

### 2.1 上传代码到服务器

**方式一：Git Clone（推荐）**

```bash
# SSH 到服务器
ssh your-server

# 创建项目目录
sudo mkdir -p /opt/budgetpilot
sudo chown $USER:$USER /opt/budgetpilot

# Clone 代码
git clone <your-repo-url> /opt/budgetpilot
cd /opt/budgetpilot
```

**方式二：SCP 上传**

```bash
# 本地机器执行
scp -r budget-management/ your-server:/opt/budgetpilot

# SSH 到服务器
ssh your-server
cd /opt/budgetpilot
```

**方式三：打包上传**

```bash
# 本地打包
tar -czf budgetpilot.tar.gz --exclude='.git' budget-management/

# 上传
scp budgetpilot.tar.gz your-server:/opt/

# 服务器解压
ssh your-server
cd /opt
tar -xzf budgetpilot.tar.gz
mv budget-management budgetpilot
cd /opt/budgetpilot
```

### 2.2 确认项目文件结构

```bash
cd /opt/budgetpilot

# 确认关键文件存在
ls -la docker-compose.yml Dockerfile pom.xml nginx.conf nginx-docker.conf
ls -la sql/schema.sql sql/init_data.sql
ls -la src/main/resources/application.yml
ls -la frontend/package.json frontend/vite.config.js
```

### 2.3 创建环境变量文件

```bash
cd /opt/budgetpilot

cat > .env << 'EOF'
DB_ROOT_PASS=your_secure_root_password
DB_PASS=your_budgetpilot_db_password
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
EXCHANGE_RATE_API_KEY=
EOF

# 设置权限（仅所有者可读）
chmod 600 .env
```

**参数说明**：

| 变量 | 必填 | 说明 |
|------|------|------|
| `DB_ROOT_PASS` | 是 | MySQL root 密码 |
| `DB_PASS` | 是 | budgetpilot 数据库用户密码 |
| `TELEGRAM_BOT_TOKEN` | 否 | Telegram Bot Token，用于推送通知 |
| `TELEGRAM_CHAT_ID` | 否 | 接收通知的 Telegram Chat ID |
| `EXCHANGE_RATE_API_KEY` | 否 | 汇率 API Key，用于多币种换算 |

### 2.4 构建后端 JAR

```bash
cd /opt/budgetpilot

# Maven 构建（跳过测试以加快速度）
mvn clean package -DskipTests

# 构建完成后确认 jar 文件
ls -la target/budgetpilot-*.jar
# 应显示类似：-rw-r--r-- 1 user user 85M Apr 15 10:00 target/budgetpilot-1.0.0-SNAPSHOT.jar
```

### 2.5 构建前端

```bash
cd /opt/budgetpilot/frontend

# 安装依赖
npm install

# 构建生产包
npm run build

# 确认构建产物
ls -la dist/

# 将 dist 内容复制到 web 目录（Nginx 读取）
sudo mkdir -p /opt/budgetpilot/web
sudo cp -r dist/* /opt/budgetpilot/web/

# 确认文件存在
ls /opt/budgetpilot/web/index.html
```

### 2.6 首次启动 Docker Compose

```bash
cd /opt/budgetpilot

# 确认 SQL 初始化文件位置（Docker 首次启动时会自动执行）
ls -la sql/schema.sql sql/init_data.sql

# 启动所有服务（首次会自动构建 Docker 镜像）
docker compose up -d

# 观察启动日志
docker compose logs -f
```

**启动过程**：
1. Docker 会拉取 MySQL 8.0 和 Redis 7 镜像
2. 根据 Dockerfile 构建 API 镜像
3. 先启动 MySQL，健康检查通过后启动 API
4. MySQL 首次启动会自动执行 `sql/` 目录下的初始化脚本

### 2.7 验证部署

```bash
# 1. 检查容器状态（应看到 3 个容器都在运行）
docker compose ps

# 2. 测试 API（端口 6060）
curl http://127.0.0.1:6060/api/v1/accounts
# 成功返回示例: {"code":0,"message":"ok","data":[]}

# 3. 检查数据库表是否创建成功
source .env  # 加载环境变量
docker compose exec mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot -e "SHOW TABLES;"
# 应看到 t_account, t_category, t_transaction 等 10 张表

# 4. 测试 Redis
docker compose exec redis redis-cli ping
# 应返回: PONG

# 5. 检查 MySQL 健康状态
docker compose ps mysql
# STATUS 列应显示 "healthy"
```

---

## 三、Nginx 配置

Nginx 用于代理前端静态文件和 API 请求，同时提供 HTTPS 支持。

### 3.1 方案 A：使用服务器已有 Nginx

```bash
# 编辑 Nginx 配置
sudo nano /etc/nginx/conf.d/budgetpilot.conf
```

写入以下内容（参考项目 `nginx.conf`）：

```nginx
server {
    listen 80;
    server_name your-domain.com;   # 替换为你的域名或服务器 IP

    # 前端静态文件
    location / {
        root /opt/budgetpilot/web;
        try_files $uri $uri/ /index.html;
    }

    # 代理后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:6060;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 上传文件
    location /uploads/ {
        alias /opt/budgetpilot/uploads/;
        client_max_body_size 5m;
    }
}
```

**测试并重启 Nginx**：

```bash
# 检查配置语法
sudo nginx -t

# 重新加载
sudo systemctl reload nginx
```

### 3.2 方案 B：使用项目自带的 Docker Nginx（独立部署）

如果你服务器没有 Nginx，可以使用项目里的 `nginx-docker.conf`：

```bash
cd /opt/budgetpilot

# 创建 SSL 证书目录
sudo mkdir -p /opt/budgetpilot/ssl

# 放入证书文件（已有则跳过）
sudo cp your-cert.crt /opt/budgetpilot/ssl/budget.crt
sudo cp your-key.key /opt/budgetpilot/ssl/budget.key

# 复制 Nginx 配置
sudo cp nginx-docker.conf /etc/nginx/conf.d/budgetpilot.conf

# 测试并重启
sudo nginx -t
sudo systemctl reload nginx
```

### 3.3 配置 SSL 证书

**Tailscale 环境（推荐）**：

```bash
# Tailscale MagicDNS 自带 TLS 证书
# 证书通常在：
sudo ls /var/lib/tailscale/certs/

# 或者使用 Tailscale 的 HTTPS 功能自动获取
sudo tailscale cert budget.tailnet.ts.net
```

**Let's Encrypt（公网域名）**：

```bash
# 安装 certbot
sudo apt install -y certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期（添加到 crontab）
sudo crontab -e
# 添加: 0 3 * * 1 certbot renew --quiet
```

**自签名证书（仅测试）**：

```bash
openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout /opt/budgetpilot/ssl/budget.key \
  -out /opt/budgetpilot/ssl/budget.crt \
  -subj "/CN=localhost"
```

### 3.4 验证访问

```bash
# HTTP 访问（前端页面）
curl http://localhost/

# API 通过 Nginx 代理访问
curl http://localhost/api/v1/accounts

# HTTPS 访问（如果配了 SSL）
curl -k https://localhost/api/v1/accounts
```

浏览器访问：
- **HTTP**: `http://<server-ip>` 或 `http://your-domain.com`
- **HTTPS**: `https://<server-ip>` 或 `https://your-domain.com`

---

## 四、Telegram Bot 配置

### 4.1 创建 Bot

1. 在 Telegram 中搜索 `@BotFather`
2. 发送 `/newbot`
3. 按提示设置 Bot 名称
4. 复制获得的 **Bot Token**（格式：`123456:ABC-DEF...`）

### 4.2 获取 Chat ID

1. 打开你创建的 Bot，发送一条任意消息
2. 浏览器访问：`https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates`
3. 在返回的 JSON 中找到 `"chat": {"id": 123456789}`，这个数字就是 **Chat ID**

### 4.3 配置到系统

**方式一：通过前端页面（推荐）**

1. 打开系统设置页面
2. 填入 Bot Token 和 Chat ID
3. 点击「保存」

**方式二：通过 SQL 直接写入**

```bash
cd /opt/budgetpilot
source .env

# 进入数据库
docker compose exec mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot

# 执行 SQL（替换实际值）
UPDATE t_config SET config_value = '<YOUR_BOT_TOKEN>' WHERE config_key = 'telegram_bot_token';
UPDATE t_config SET config_value = '<YOUR_CHAT_ID>' WHERE config_key = 'telegram_chat_id';

# 退出
exit
```

**方式三：通过环境变量（重启后生效）**

```bash
# 编辑 .env 文件
nano /opt/budgetpilot/.env

# 填入值后重启
cd /opt/budgetpilot
docker compose restart budgetpilot-api
```

### 4.4 测试推送

```bash
# 通过 API 触发测试推送
curl -X POST http://127.0.0.1:6060/api/v1/system/telegram/test

# 成功应返回: {"code":0,"message":"ok","data":"测试消息已发送"}
```

---

## 五、ExchangeRate-API 配置

### 5.1 注册 API Key

1. 访问 https://www.exchangerate-api.com/
2. 注册免费账户（每月 1,500 次请求额度）
3. 复制 API Key

### 5.2 配置到系统

```bash
cd /opt/budgetpilot
source .env

docker compose exec mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot

UPDATE t_config SET config_value = '<YOUR_API_KEY>' WHERE config_key = 'exchange_rate_api_key';
exit
```

### 5.3 手动刷新汇率

```bash
curl -X POST http://127.0.0.1:6060/api/v1/system/rates/refresh
```

---

## 六、日常运维

### 6.1 查看日志

```bash
cd /opt/budgetpilot

# 查看所有服务日志
docker compose logs -f

# 只看 API 日志
docker compose logs -f budgetpilot-api

# 只看最近 100 行
docker compose logs --tail=100 budgetpilot-api

# 查看 MySQL 日志
docker compose logs mysql

# 查看 Redis 日志
docker compose logs redis
```

### 6.2 重启服务

```bash
cd /opt/budgetpilot

# 重启 API
docker compose restart budgetpilot-api

# 全部重启
docker compose restart
```

### 6.3 更新部署

```bash
cd /opt/budgetpilot

# 1. 拉取最新代码
git pull
# 或上传新代码

# 2. 重新构建后端 JAR
mvn clean package -DskipTests

# 3. 重新构建前端
cd frontend
npm install
npm run build
cp -r dist/* /opt/budgetpilot/web/
cd ..

# 4. 重新构建 Docker 镜像并启动
docker compose up -d --build

# 5. 验证
docker compose ps
curl http://127.0.0.1:6060/api/v1/accounts
```

### 6.4 仅更新后端（不改前端时）

```bash
cd /opt/budgetpilot

# 重新构建 JAR
mvn clean package -DskipTests

# 重启 API 容器（会自动用新 Dockerfile 构建镜像）
docker compose up -d --build budgetpilot-api
```

### 6.5 仅更新前端（不改后端时）

```bash
cd /opt/budgetpilot/frontend

npm install
npm run build

# 同时更新两个目录（nginx web 目录和 Spring Boot static 目录）
cp -r dist/* /opt/budgetpilot/web/
cp -r dist/* /opt/budgetpilot/src/main/resources/static/

# 刷新 Nginx 缓存
sudo nginx -s reload
```

> **注意**：如果使用 nginx 代理前端，需要更新 `/opt/budgetpilot/web/` 目录；如果直接访问 Spring Boot 端口，需要更新 `src/main/resources/static/` 目录。两个目录都更新可以确保两种访问方式都生效。

### 6.6 数据库备份

```bash
cd /opt/budgetpilot
source .env

# 创建备份目录
mkdir -p backup

# 手动备份
DATE=$(date +%Y%m%d_%H%M%S)
docker compose exec mysql mysqldump -ubudgetpilot -p${DB_PASS} budgetpilot \
  --single-transaction --routines \
  > backup/budgetpilot_${DATE}.sql

# 压缩
gzip backup/budgetpilot_${DATE}.sql

# 确认备份文件
ls -lh backup/budgetpilot_${DATE}.sql.gz
```

**定时备份（crontab）**：

```bash
crontab -e

# 添加：每天凌晨 3 点自动备份
0 3 * * * cd /opt/budgetpilot && source .env && DATE=$(date +\%Y\%m\%d_\%H\%M\%S) && docker compose exec mysql mysqldump -ubudgetpilot -p${DB_PASS} budgetpilot --single-transaction --routines > backup/budgetpilot_\${DATE}.sql && gzip backup/budgetpilot_\${DATE}.sql && find backup/ -name "*.sql.gz" -mtime +30 -delete
```

**恢复备份**：

```bash
cd /opt/budgetpilot
source .env

# 解压
gunzip backup/budgetpilot_20260415_030000.sql.gz

# 恢复
docker compose exec -T mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot < backup/budgetpilot_20260415_030000.sql
```

### 6.7 数据库直连

```bash
cd /opt/budgetpilot
source .env

# 进入 MySQL 交互式命令行
docker compose exec mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot

# 常用查询
SELECT COUNT(*) FROM t_transaction;
SELECT * FROM t_account WHERE is_active = 1;
SELECT * FROM t_config;

# 退出
exit
```

### 6.8 清理空间

```bash
# 清理 Docker 悬空镜像
docker image prune -f

# 清理所有未使用的 Docker 资源（镜像、容器、网络、卷）
docker system prune -a

# 清理 30 天前的备份
find /opt/budgetpilot/backup -name "*.sql.gz" -mtime +30 -delete

# 查看磁盘使用
du -sh /opt/budgetpilot/*
```

---

## 七、常用 API 接口

所有 API 基础路径为 `http://127.0.0.1:6060/api/v1`

### 账户管理

```bash
# 获取账户列表
curl http://127.0.0.1:6060/api/v1/accounts

# 创建账户
curl -X POST http://127.0.0.1:6060/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"招商储蓄卡","type":2,"currency":"CNY","initialBalance":10000}'

# 更新账户
curl -X PUT http://127.0.0.1:6060/api/v1/accounts/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"工商银行"}'
```

### 交易管理

```bash
# 创建交易（支出）
curl -X POST http://127.0.0.1:6060/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":1,"amount":50,"currency":"CNY","accountId":1,"categoryId":1,"transactionDate":"2026-04-15","note":"午餐"}'

# 创建待确认交易（isConfirmed=false）
curl -X POST http://127.0.0.1:6060/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":1,"amount":5000,"accountId":1,"categoryId":1,"transactionDate":"2026-04-15","note":"待确认支出","isConfirmed":false}'

# 分页查询交易
curl "http://127.0.0.1:6060/api/v1/transactions?page=1&size=20"

# 按日期范围查询
curl "http://127.0.0.1:6060/api/v1/transactions?startDate=2026-04-01&endDate=2026-04-30"

# 按状态筛选（confirmed=true 已确认，confirmed=false 待确认）
curl "http://127.0.0.1:6060/api/v1/transactions?confirmed=false"

# 确认待确认交易
curl -X POST http://127.0.0.1:6060/api/v1/transactions/{id}/confirm
```

> **状态筛选说明**：
> - `confirmed=true` - 只返回已确认交易（影响余额）
> - `confirmed=false` - 只返回待确认交易（不影响余额）
> - 不传 `confirmed` 参数 - 返回全部交易

### 分类管理

```bash
# 获取分类列表
curl http://127.0.0.1:6060/api/v1/categories

# 创建分类
curl -X POST http://127.0.0.1:6060/api/v1/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"餐饮","type":1,"parentId":0}'
```

### 报表

```bash
# 月度总览
curl "http://127.0.0.1:6060/api/v1/reports/monthly-summary?month=2026-04"

# 趋势分析（最近 12 个月）
curl "http://127.0.0.1:6060/api/v1/reports/trend?months=12"
```

### 系统

```bash
# 查看配置
curl http://127.0.0.1:6060/api/v1/system/config

# 测试 Telegram 推送
curl -X POST http://127.0.0.1:6060/api/v1/system/telegram/test

# 刷新汇率
curl -X POST http://127.0.0.1:6060/api/v1/system/rates/refresh
```

---

## 八、故障排查

### API 无法启动

```bash
# 查看详细错误
docker compose logs budgetpilot-api 2>&1 | tail -50
```

**常见问题**：

| 问题 | 排查方法 | 解决方案 |
|------|---------|---------|
| 数据库连接失败 | `docker compose ps mysql` 检查是否 healthy | 等待 MySQL 健康检查通过后再启动 API |
| 端口 6060 被占用 | `ss -tlnp \| grep 6060` | 停止占用端口的进程或更换端口 |
| 内存不足 | `free -h` | 减少容器内存限制或升级服务器 |
| JAR 文件缺失 | `ls target/budgetpilot-*.jar` | 重新执行 `mvn clean package` |

### 端口占用检查

```bash
# 检查 6060 端口是否被占用
ss -tlnp | grep 6060
# 或
netstat -tlnp | grep 6060

# 如果有输出，说明端口已被占用
# 查看具体进程
lsof -i :6060
```

### 数据库未初始化

```bash
cd /opt/budgetpilot
source .env

# 手动执行初始化脚本
docker compose exec mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot < sql/schema.sql
docker compose exec mysql mysql -ubudgetpilot -p${DB_PASS} budgetpilot < sql/init_data.sql
```

### MySQL 健康检查一直失败

```bash
# 查看 MySQL 日志
docker compose logs mysql

# 常见问题：
# 1. 初始化 SQL 有语法错误 → 检查 sql/schema.sql
# 2. 磁盘空间不足 → df -h
# 3. 内存不足 → free -h

# 手动进入 MySQL 检查
docker compose exec mysql mysql -uroot -p${DB_ROOT_PASS} -e "SELECT 1;"
```

### Redis 连接失败

```bash
# 测试 Redis 连通性
docker compose exec redis redis-cli ping

# 查看 Redis 日志
docker compose logs redis

# 检查配置中的 Redis 地址是否正确
docker compose exec budgetpilot-api cat /app/application.yml
```

### 前端 404 / 白屏

```bash
# 确认 web 目录有文件
ls -la /opt/budgetpilot/web/

# 应看到 index.html 和 assets/ 目录

# 如果没有文件，重新构建前端
cd /opt/budgetpilot/frontend
npm run build
cp -r dist/* /opt/budgetpilot/web/

# Nginx 配置检查
sudo nginx -t
sudo nginx -s reload
```

### Telegram 推送失败

```bash
# 检查配置是否正确
curl http://127.0.0.1:6060/api/v1/system/config

# 检查网络连通性（Telegram API 需要代理出境）
curl -v https://api.telegram.org/

# 如果无法访问，需要配置代理
# 在 docker-compose.yml 中添加环境变量：
# - HTTP_PROXY=http://proxy:port
# - HTTPS_PROXY=http://proxy:port
```

### 容器启动后自动退出

```bash
# 查看退出原因
docker compose ps -a

# 查看完整日志
docker compose logs budgetpilot-api

# 常见原因：
# 1. Java OOM → 增加 mem_limit
# 2. 数据库连接超时 → 检查 MySQL 是否先启动
# 3. 配置错误 → 检查 .env 文件
```

---

## 九、前端开发模式

如果需要在本地开发前端（非生产服务器）：

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:3000
# API 请求已通过 Vite proxy 自动转发到 http://127.0.0.1:6060
```

**Vite 开发代理配置**（`frontend/vite.config.js`）：

```js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:6060',  // 指向后端 6060 端口
      changeOrigin: true
    }
  }
}
```

### 生产构建

```bash
cd frontend
npm run build
# 输出在 frontend/dist/ 目录
# 将其内容复制到服务器的 /opt/budgetpilot/web/
```

---

## 十、完整部署流程总结

如果你是第一次部署，按以下步骤操作：

```bash
# ===== 第 1 步：准备环境 =====
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# ===== 第 2 步：上传代码 =====
# (scp 或 git clone 到 /opt/budgetpilot)
cd /opt/budgetpilot

# ===== 第 3 步：创建 .env =====
cat > .env << 'ENVEOF'
DB_ROOT_PASS=your_root_password
DB_PASS=your_db_password
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
EXCHANGE_RATE_API_KEY=
ENVEOF
chmod 600 .env

# ===== 第 4 步：构建后端 =====
mvn clean package -DskipTests

# ===== 第 5 步：构建前端 =====
cd frontend && npm install && npm run build
cp -r dist/* /opt/budgetpilot/web/
cd ..

# ===== 第 6 步：启动 Docker =====
docker compose up -d

# ===== 第 7 步：验证 =====
docker compose ps
curl http://127.0.0.1:6060/api/v1/accounts

# ===== 第 8 步（可选）：配置 Nginx =====
sudo cp nginx.conf /etc/nginx/conf.d/budgetpilot.conf
sudo nginx -t && sudo systemctl reload nginx
```

---

## 十一、PWA 添加到手机

1. Safari/Chrome 打开 BudgetPilot 网址
2. 点击「分享」→「添加到主屏幕」
3. 即可像原生 App 一样使用（支持离线缓存、推送通知）
