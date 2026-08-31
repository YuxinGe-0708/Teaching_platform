package org.example.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class E2eDatabase implements AutoCloseable {
    private final Connection connection;

    private E2eDatabase(Connection connection) {
        this.connection = connection;
    }

    static E2eDatabase connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MySQL JDBC driver is not available", exception);
        }
        return new E2eDatabase(DriverManager.getConnection(E2eConfig.DB_URL, E2eConfig.DB_USER, E2eConfig.DB_PASSWORD));
    }

    List<List<String>> query(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<List<String>> rows = new ArrayList<>();
                int columns = resultSet.getMetaData().getColumnCount();
                while (resultSet.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= columns; i++) row.add(resultSet.getString(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    String scalar(String sql, Object... parameters) throws SQLException {
        List<List<String>> rows = query(sql, parameters);
        return rows.isEmpty() || rows.get(0).isEmpty() ? "" : rows.get(0).get(0);
    }

    Map<String, String> row(String sql, String[] columns, Object... parameters) throws SQLException {
        List<List<String>> rows = query(sql, parameters);
        Map<String, String> result = new LinkedHashMap<>();
        if (rows.isEmpty()) return result;
        for (int i = 0; i < columns.length; i++) {
            result.put(columns[i], i < rows.get(0).size() ? rows.get(0).get(i) : "");
        }
        return result;
    }

    int execute(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
