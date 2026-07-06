---
name: "mes-api-reference"
description: "MES 系统 API 参考：通用表查询、订单聚合接口，base URL http://192.168.1.103:8088"
---

# MES API 参考

## 基础信息
- **基础地址：** http://192.168.1.103:8088
- **鉴权：** 无需 Token（内网访问）

## 通用表查询

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/tables | 列出所有表 |
| GET | /api/tables/{name}/columns | 查表结构 |
| GET | /api/tables/{name}?page=1&size=50&filter={"col1":"val1"} | 分页查询，filter 支持等值过滤和范围操作符 `$gte`/`$gt`/`$lte`/`$lt` |

## 订单聚合接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/orders/{orderNo}?site=3100 | 一次返回订单主表+客户名+明细列表 |

## 数采数据接口（电流/电压）

⚠️ **dc_ 开头的表（dc_station_to_electricity、dc_station_to_voltage 等）是数据采集大表，不支持通用表查询，必须使用专用接口。**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/dc/electricity?startTime=&endTime=&station=&page=&size= | 查询电流数据，startTime/endTime 必填 |
| GET | /api/dc/voltage?startTime=&endTime=&station=&page=&size= | 查询电压数据，startTime/endTime 必填 |

参数：startTime/endTime 格式 `yyyy-MM-dd HH:mm:ss`，station 可选，page 默认 1，size 默认 50 最大 500。

## 使用场景

当用户请求查询 MES 系统数据时，优先使用对应的聚合接口而非通用表查询。
- 查订单 → `/api/orders/{orderNo}?site=3100`
- 查表数据 → `/api/tables/{name}`
- 查表结构 → `/api/tables/{name}/columns`
