package org.example.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class E2eMatrix {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<Map<String, Object>> CASES = new ArrayList<>();

    private E2eMatrix() {
    }

    static synchronized void add(String testName, String testId, String testFunction, Object inputData,
                                  String preconditions, String expectedOutput, Object actualOutput,
                                  String assertionResult, boolean passed) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("测试名称", testName);
        row.put("测试编号", testId);
        row.put("测试函数（代码语言）", testFunction);
        row.put("输入数据（代码语言）", inputData);
        row.put("前置条件（自然语言）", preconditions);
        row.put("预期输出（自然语言）", expectedOutput);
        row.put("实际输出（代码语言）", actualOutput);
        row.put("断言结果", assertionResult);
        row.put("passed", passed);
        CASES.add(row);
        write();
    }

    static synchronized void write() {
        try {
            File jsonFile = new File(E2eConfig.MATRIX_FILE);
            File parent = jsonFile.getParentFile();
            if (parent != null) parent.mkdirs();
            JSON.writerWithDefaultPrettyPrinter().writeValue(jsonFile, CASES);
            writeCsv(new File(E2eConfig.MATRIX_CSV_FILE));
        } catch (IOException e) {
            throw new IllegalStateException("写入 E2E 测试矩阵失败：" + e.getMessage(), e);
        }
    }

    private static void writeCsv(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            String[] headers = {"测试名称", "测试编号", "测试函数（代码语言）", "输入数据（代码语言）",
                    "前置条件（自然语言）", "预期输出（自然语言）", "实际输出（代码语言）", "断言结果", "passed"};
            writer.write(join(headers));
            writer.write("\n");
            for (Map<String, Object> row : CASES) {
                String[] values = new String[headers.length];
                for (int i = 0; i < headers.length; i++) values[i] = String.valueOf(row.get(headers[i]));
                writer.write(join(values));
                writer.write("\n");
            }
        }
    }

    private static String join(String[] values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(',');
            String escaped = value == null ? "" : value.replace("\"", "\"\"");
            result.append('"').append(escaped).append('"');
        }
        return result.toString();
    }
}
