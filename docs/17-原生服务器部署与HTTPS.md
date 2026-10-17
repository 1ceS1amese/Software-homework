# 17 原生服务器部署与 HTTPS

本指南用于用户明确授权的独立演示部署。必须使用本项目的新数据库实例与数据目录，保留目标上的其他服务；禁止上传本地开发数据库、真实 `.env.local`、SSH 私钥、依赖缓存或测试认证状态。此流程不启动 Docker / Testcontainers，不创建云资源。

## 1. 依赖与交付目录

原生运行方案使用 Java 17、MySQL 8、Nginx、OpenSSL、Python 3 和 systemd。必须先检查目标实际版本、资源、监听端口与 Nginx 的配置包含关系；Debian 12 的 JRE 可通过 `openjdk-17-jre-headless` 软件包准备，其他系统应使用对应官方发行包。已有 MySQL 二进制可供新实例只读使用，不得复用其数据目录或管理账号。

| 路径 | 内容与权限 |
|---|---|
| `/opt/csms/releases/<release-id>` | 校验后的 JAR、前端静态文件、通用模板和 manifest；目录 755，文件 644 |
| `/opt/csms/current` | 指向当前发布目录的符号链接 |
| `/opt/csms/mysql` | 指向经过版本核验的 MySQL 安装目录；不含本机开发数据 |
| `/etc/csms/application-demo.yml` | 外部演示运行配置，不含真实密钥 |
| `/etc/csms/backend.env` | 目标独立生成的数据库/JWT/进程参数；root 所有，600 |
| `/etc/csms/mysql.cnf` | 新实例的配置；不替换原有 MySQL 配置 |
| `/etc/csms/mysql-root.cnf` | 仅用于新实例管理的客户端配置；root 所有，600 |
| `/etc/csms/tls` | 私有 CA、服务器证书与私钥；私钥 600，公开证书 644 |
| `/var/lib/csms-mysql` | 新实例持久数据；`csms-mysql` 所有，700 |
| `/run/csms-mysql` | 新实例 socket/PID，由 systemd 创建 |
| `/var/log/csms`、`/var/log/csms-mysql` | 本项目日志，不进入 Git |

后端应以 `csms` 运行，数据库应以 `csms-mysql` 运行，均为不可交互登录的专用系统账号。后端与数据库仅监听回环地址；对外入口由 Nginx 提供。默认端口分别为 8081、3307、80/443，实际使用前必须确认空闲或可通过独立站点共享。

## 2. 构建与上传

在本地项目根目录执行：

```bash
source scripts/env.sh
(cd backend && bash ./mvnw -B -ntp package)
(cd frontend && pnpm install --frozen-lockfile && pnpm build)
```

必须从同一受版本控制的源码快照组织发布包，只包含：

- `backend/target/csms-backend-1.0.0-SNAPSHOT.jar`，发布时命名为 `app.jar`。
- `frontend/dist/`，发布时命名为 `frontend/`。
- [原生配置模板](../deploy/native/)，发布时命名为 `native/`。
- 记录源码提交与每个文件 SHA-256 的 manifest，以及发布包 SHA-256 校验文件。

上传至本项目专用的接收目录。解包前必须验证压缩包摘要、文件清单与每个文件摘要；禁止绝对路径、`..`、符号链接和清单之外的文件。不得将依赖目录、工具安装目录、真实配置、数据库或机器记录打包。构建目录只是中间产物，完成交付后可以清理。

解包时即使使用了严格 `umask`，也必须显式设置发布根目录和子目录为 755，再以实际服务账号检查读取权限：

```bash
# 目标服务器；current 已指向经过核验的发布目录
runuser -u csms -- test -r /opt/csms/current/app.jar
# Nginx 账号应以 nginx -T 中的实际 user 指令为准
```

## 3. 独立数据库与后端

必须先确认 `/opt/csms`、本项目单元名与新数据目录没有无关内容。账号、目录和 MySQL 二进制准备完成后，按模板安装：

```bash
# 目标服务器；不得覆盖原有 MySQL 配置或数据目录
install -m 644 /opt/csms/current/native/mysql.cnf.example /etc/csms/mysql.cnf
install -m 644 /opt/csms/current/native/application-demo.yml /etc/csms/application-demo.yml
install -m 644 /opt/csms/current/native/csms-mysql.service /etc/systemd/system/csms-mysql.service
install -m 644 /opt/csms/current/native/csms-backend.service /etc/systemd/system/csms-backend.service

# 只允许在本项目新建且为空的数据目录执行一次
/opt/csms/mysql/bin/mysqld --no-defaults --initialize-insecure \
  --user=csms-mysql --basedir=/opt/csms/mysql --datadir=/var/lib/csms-mysql \
  --log-error=/var/log/csms-mysql/initialize.log

systemctl daemon-reload
systemd-analyze verify /etc/systemd/system/csms-mysql.service /etc/systemd/system/csms-backend.service
systemctl enable --now csms-mysql
```

初始化后必须通过本实例 socket 创建独立演示库及应用账号，只授予该库权限，并立即设置随机数据库 root 密码。客户端必须显式使用本实例 socket 与配置文件，并将 `MYSQL_TEST_LOGIN_FILE` 指向本项目的不存在路径，防止读取机器登录文件；不得向客户端传入现有实例凭据。

在目标生成新的数据库密码、JWT 密钥及三角色访问密码。以 [backend.env.example](../deploy/native/backend.env.example) 为字段参考，写入权限为 600 的 `/etc/csms/backend.env`；真实值禁止进入 Git、前端或命令行参数。服务使用 `demo` profile 和外部 YAML，不加载本地 `.env.local`。外部配置禁止对非空未知库自动建立 Flyway 基线，并排除未使用的默认内存用户自动配置；JWT 登录继续使用业务用户。

```bash
# 目标服务器：真实配置准备完成后
systemctl enable --now csms-backend
curl --fail http://127.0.0.1:8081/actuator/health
```

Flyway 必须完成 V1～V4，健康状态必须为 `UP`。公网开放前应在回环接口将 `admin`、`teacher1`、`student1` 的初始演示密码更换为新生成值，并验证重新登录成功；其他初始演示账号应停用。本方案使用演示迁移和演示数据，不能直接套用于真实教务生产库。

## 4. IP 证书与客户端信任

必须将实际 IP 作为参数；以下命令中的 `PUBLIC_IP` 应先由使用者在服务器设置。

```bash
# 目标服务器；生成自签名根 CA，再签发包含 IP SAN 的服务器证书
python3 /opt/csms/current/native/create-certificate.py "$PUBLIC_IP"
```

脚本应生成私有 CA、含 IP SAN 的服务器证书与完整链。根 CA 私钥和服务器私钥只保留在目标，权限必须为 600；公开 CA 可以交付给访问者。重复执行应验证已有证书并保留密钥；错误 IP、文件不完整或剩余有效期不足 30 天必须拒绝复用。机制参考 [OpenSSL 证书生成说明](https://docs.openssl.org/3.5/man1/openssl-req/)。

自建 CA 默认不在浏览器信任库中，首次访问会出现证书信任提示。访问者应通过管理员交付渠道核对 CA 指纹，再导入公开 CA；每台设备应单独建立信任。站点提供 `/csms-ca.crt` 作为公开根证书下载入口。不得把私钥或包含密码的文件放入静态目录。

## 5. Nginx 站点与反代

应从 `nginx -T` 确认现有 `http` 块包含的站点目录，在其中新增本项目独立 vhost 文件；面板管理的 Nginx 应沿用它的包含目录，禁止替换整份主配置。以 [nginx.conf.example](../deploy/native/nginx.conf.example) 为模板，仅替换 `__PUBLIC_IP__` 与 `__BACKEND_PORT__`，保留 Nginx 自身的 `$host` 等变量。

配置必须实现：

- IP 的 443 端口托管前端，服务器本机的 80 端口跳转 HTTPS；SPA 深层路由刷新应返回入口页面。
- `/api/` 反代到回环后端并移除此前缀，沿用当前应用兼容调用；不能据此声称 `docs/05` 的全部标准 API 已完成。
- API 代理缓存显式关闭，避免继承已有 Nginx 的缓存策略；静态指纹资源允许长期缓存。
- 安全头和缓存头由 Nginx 统一输出，隐藏后端重复头；演示私有 CA 不启用长期 HSTS 策略。
- 反代覆盖真实来源与协议头，登录接口应限速；数据库与后端端口不得新增公网监听。

```bash
# 目标服务器：站点配置准备完成后
nginx -t
nginx -s reload
```

重载后必须等待新配置实际生效，再核对公开响应。应沿用目标已有的 Nginx 管理方式并核实开机启动；应用与新数据库单元应为 enabled。

## 6. 上线校验与二次验证

```bash
# 目标服务器：自己的服务与监听状态
systemctl is-active csms-mysql csms-backend
systemctl is-enabled csms-mysql csms-backend
ss -ltn

# 本地或另一台访问机器：CA 文件须由可信交付渠道取得
curl --fail --cacert "$CSMS_CA_CERT" "https://$PUBLIC_IP/login"
curl --fail --cacert "$CSMS_CA_CERT" "https://$PUBLIC_IP/api/actuator/health"
```

必须严格核对 CA 链、IP 标识、证书有效期及 TLS 1.2/1.3，不能仅用跳过证书校验的请求代替。随后用独立角色会话按 [三端验收指南](16-三端联动与数据库验收.md) 验证页面增改删、刷新后持久化、选退课与教师名单同步、成绩暂存/发布/撤回与学生同步，并直接核对新数据库。

浏览器验证应包含 Chromium、Firefox、WebKit 及 390/768/1440 视口。页面刷新前必须确认业务表格数据已加载；表格可横向滚动，应通过实际可访问数据行核对，避免把隐藏测量节点当作可见内容。系统运行库必须准备完整，禁止通过删除页面错误、数据库或权限断言绕过失败。

应停止并重启本项目自己的后端和数据库，确认业务数据摘要与账户配置保持一致；禁止为此重启整台承载其他服务的主机。第二轮复测若需重置演示关系，必须先备份**本项目新库**，仅清理本轮拥有的测试关系并留操作记录，不得使用既有业务库。两轮通过后保留必要演示数据，清理过期发布副本、临时包、原始跟踪和认证产物。

## 7. 维护与回退

日志位于本项目目录，可通过 `journalctl -u csms-backend` 与 `journalctl -u csms-mysql` 排错；对外提供日志前必须脱敏。访问密码、数据库管理配置、SSH 信息与实际地址应只通过私有交付渠道提供。

升级前应对独立演示库做一致性备份，备份目录应为 700、文件为 600；`mysqldump` 必须显式使用本项目 socket 的管理配置，禁止读取默认机器登录文件。新程序应放入新发布目录，校验后原子切换 `current`，只重启自己的后端。后续升级应保留先前验收通过的版本作为回退；数据库兼容性必须单独评估，禁止删库或复写旧迁移来回退。

证书续签应在备份后于独立目录完成签发和 IP 校验，再切换证书并重载 Nginx；私有 CA 更换需要向访问者重新交付信任。密钥、真实配置和备份始终禁止上传到代码仓库。

## 8. 访问边界

本次交付为指定演示目标，真实地址与密码不写入仓库。某些网络路径可能在公网 HTTP 到达 Nginx 前返回外部跳转；必须分别观察公网响应与服务器本机响应，不能据此反复修改已正确的站点配置。此时应使用完整 `https://<server-ip>/` 地址，并由服务器所有者按其网络接入条件处理 HTTP 入口。

本方案不包含原生移动 App、离线消息中心、正式生产发版或原始设计中仍未交付的管理模块；范围继续见 README Roadmap 与 docs/16。
