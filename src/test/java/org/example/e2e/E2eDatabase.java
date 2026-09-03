package org.example.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

final class E2eDatabase implements AutoCloseable {
    private final Connection connection;

    private E2eDatabase(Connection connection) {
        this.connection = connection;
    }

    static E2eDatabase connect(String schema) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("测试 JDBC 驱动未加载，请确认 test 依赖已安装。", e);
        }
        SQLException first = null;
        for (String password : new String[] {E2eConfig.DB_PASSWORD, "LZY1234lzy060812"}) {
            if (first != null && password.equals(E2eConfig.DB_PASSWORD)) continue;
            try {
                return new E2eDatabase(DriverManager.getConnection(
                        E2eConfig.jdbcUrl(schema), E2eConfig.DB_USERNAME, password));
            } catch (SQLException e) {
                if (first == null) first = e;
            }
        }
        throw new SQLException("无法连接 MySQL schema=" + schema + "，已尝试测试配置及临时凭据 root/LZY1234lzy060812。", first);
    }

    String scalar(String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = prepare(sql, args); ResultSet result = statement.executeQuery()) {
            return result.next() ? String.valueOf(result.getObject(1)) : null;
        }
    }

    Map<String, String> row(String sql, String[] columns, Object... args) throws SQLException {
        Map<String, String> result = new LinkedHashMap<>();
        try (PreparedStatement statement = prepare(sql, args); ResultSet rows = statement.executeQuery()) {
            if (!rows.next()) return result;
            for (String column : columns) {
                Object value = rows.getObject(column);
                result.put(column, value == null ? null : String.valueOf(value));
            }
            return result;
        }
    }

    int execute(String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = prepare(sql, args)) {
            return statement.executeUpdate();
        }
    }

    private PreparedStatement prepare(String sql, Object... args) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
        return statement;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
