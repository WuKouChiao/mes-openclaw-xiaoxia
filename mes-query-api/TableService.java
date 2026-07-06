package com.mes.query.service;

import com.mes.query.common.BusinessException;
import com.mes.query.common.SqlTimer;
import com.mes.query.vo.ColumnVO;
import com.mes.query.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 表查询服务——只读，ddcoreprd 库
 */
@Slf4j
@Service
public class TableService {

    private static final String DATABASE = "ddcoreprd";
    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private DataSource dataSource;

    /** 缓存的表名白名单，项目启动后延迟加载 */
    private volatile Set<String> tableCache;

    /** 黑名单：禁止通用查询的表（大表，必须走专用接口 + 索引） */
    private static final Set<String> TABLE_BLACKLIST = new HashSet<>(Arrays.asList(
        "dc_station_to_electricity",
        "dc_station_to_voltage"
    ));

    /** 支持的范围操作符 */
    private static final Set<String> RANGE_OPERATORS = new HashSet<>(Arrays.asList(
        "$gt", "$gte", "$lt", "$lte"
    ));

    /**
     * 列出所有表名
     */
    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(DATABASE, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        tables.sort(String::compareTo);
        return tables;
    }

    /**
     * 获取表列信息
     *
     * @param tableName 表名（需通过白名单校验）
     */
    public List<ColumnVO> getColumns(String tableName) throws SQLException {
        validateTable(tableName);
        List<ColumnVO> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(DATABASE, null, tableName, null)) {
            while (rs.next()) {
                ColumnVO col = new ColumnVO();
                col.setName(rs.getString("COLUMN_NAME"));
                col.setType(rs.getString("TYPE_NAME"));
                col.setSize(rs.getInt("COLUMN_SIZE"));
                col.setNullable(rs.getInt("NULLABLE") == 1);
                col.setRemarks(rs.getString("REMARKS"));
                columns.add(col);
            }
        }
        return columns;
    }

    /**
     * 过滤条件模型
     */
    private static class FilterClause {
        final String column;
        final String operator;  // =, >, >=, <, <=
        final String value;

        FilterClause(String column, String operator, String value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
        }
    }

    /**
     * 分页查询表数据（支持等值过滤、范围过滤和排序）
     *
     * @param tableName 表名
     * @param page      页码，从 1 开始
     * @param size      每页条数，最大 500
     * @param filter    过滤条件 JSON
     *                  等值: {"col1":"val1"}
     *                  范围: {"col1":{"$gte":"2026-06-01 00:00:00","$lt":"2026-06-10 00:00:00"}}
     * @param sort      排序字段
     * @param order     排序方向 asc/desc
     */
    public PageVO<Map<String, Object>> queryTable(String tableName, int page, int size, String filter,
                                                   String sort, String order) throws SQLException {
        long startTime = System.currentTimeMillis();
        validateTable(tableName);
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page < 1) page = 1;

        // 解析过滤条件
        List<FilterClause> filters = parseFilter(filter, tableName);

        // 构建 WHERE
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<String> paramValues = new ArrayList<>();
        for (FilterClause fc : filters) {
            whereClause.append(" AND ").append(fc.column).append(" ").append(fc.operator).append(" ?");
            paramValues.add(fc.value);
        }
        String where = whereClause.toString();

        // 查总数
        String countSql = "SELECT COUNT(*) FROM " + DATABASE + "." + tableName + where;
        long total;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < paramValues.size(); i++) {
                ps.setString(i + 1, paramValues.get(i));
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            total = rs.getLong(1);
        }

        // 排序
        Set<String> validColumns = new HashSet<>();
        for (ColumnVO col : getColumns(tableName)) {
            validColumns.add(col.getName());
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(DATABASE).append(".").append(tableName)
                .append(where);
        if (sort != null && !sort.isEmpty() && validColumns.contains(sort)) {
            String dir = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
            sql.append(" ORDER BY ").append(sort).append(" ").append(dir);
        }

        // 查数据（分页）
        sql.append(" LIMIT ").append(size).append(" OFFSET ").append((page - 1) * size);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < paramValues.size(); i++) {
                ps.setString(i + 1, paramValues.get(i));
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }

        long elapsed = SqlTimer.logQuery(tableName, sql.toString(), paramValues, startTime);
        return new PageVO<>(total, page, size, rows);
    }

    // ---- 私有方法 ----

    /**
     * 表名白名单校验
     */
    private void validateTable(String tableName) throws SQLException {
        if (tableCache == null) {
            synchronized (this) {
                if (tableCache == null) {
                    tableCache = new HashSet<>(listTables());
                }
            }
        }
        if (!tableCache.contains(tableName)) {
            throw new BusinessException(400, "表不存在: " + tableName);
        }
        if (TABLE_BLACKLIST.contains(tableName)) {
            throw new BusinessException(400, "此表为数据采集大表，请使用专用接口 /api/dc/ 查询，不支持通用表查询: " + tableName);
        }
    }

    /**
     * 列名白名单校验 + 过滤条件解析
     * 支持:
     *   等值: {"col":"val"}
     *   范围: {"col":{"$gte":"val1","$lt":"val2"}}
     */
    @SuppressWarnings("unchecked")
    private List<FilterClause> parseFilter(String filter, String tableName) throws SQLException {
        List<FilterClause> result = new ArrayList<>();
        if (filter == null || filter.isEmpty()) {
            return result;
        }

        Set<String> validColumns = new HashSet<>();
        for (ColumnVO col : getColumns(tableName)) {
            validColumns.add(col.getName());
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> raw = mapper.readValue(filter, Map.class);
            // key -> value（json 的顶层过滤）
            parseFilterRaw(raw, result, validColumns);
        } catch (Exception e) {
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new BusinessException(400, "filter 格式错误，应为 JSON: {\"col1\":\"val1\",\"col2\":{\"$gte\":\"2026-06-01 00:00:00\"}}");
        }
        return result;
    }

    private void parseFilterRaw(Map<String, Object> raw, List<FilterClause> result, Set<String> validColumns) {
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String col = entry.getKey();
            if (!validColumns.contains(col)) {
                throw new BusinessException(400, "表中不存在列: " + col);
            }
            Object val = entry.getValue();
            if (val instanceof Map) {
                // 范围过滤: {"col":{"$gte":"v1","$lt":"v2"}}
                Map<String, Object> opMap = (Map<String, Object>) val;
                for (Map.Entry<String, Object> opEntry : opMap.entrySet()) {
                    String op = opEntry.getKey();
                    String opValue = String.valueOf(opEntry.getValue());
                    if (!RANGE_OPERATORS.contains(op)) {
                        throw new BusinessException(400, "不支持的操作符: " + op + "，支持: " + RANGE_OPERATORS);
                    }
                    String sqlOp;
                    switch (op) {
                        case "$gt":  sqlOp = ">";  break;
                        case "$gte": sqlOp = ">="; break;
                        case "$lt":  sqlOp = "<";  break;
                        case "$lte": sqlOp = "<="; break;
                        default: throw new BusinessException(400, "不支持的操作符: " + op);
                    }
                    result.add(new FilterClause(col, sqlOp, opValue));
                }
            } else {
                // 等值过滤
                result.add(new FilterClause(col, "=", String.valueOf(val)));
            }
        }
    }
}
