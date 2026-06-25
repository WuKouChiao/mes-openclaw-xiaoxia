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

### SSH 主机

- **mes-dev** → 192.168.1.103, user: wgq, sudo 密码: root
  - OS: Ubuntu 20.04.6 LTS
  - 密钥: ~/.ssh/id_rsa
