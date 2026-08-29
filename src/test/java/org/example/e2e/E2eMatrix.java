package org.example.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class E2eMatrix {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<Map<String, Object>> CASES = new ArrayList<>();

    private E2eMatrix() {
    }

    static synchronized void add(String name, String number, String functions, Object input,
                                  String precondition, String expected, Object actual,
                                  String assertion, boolean passed) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("测试名称", name);
        item.put("测试编号", number);
        item.put("测试函数", functions);
        item.put("输入数据", input);
        item.put("前置条件", precondition);
        item.put("预期输出", expected);
        item.put("实际输出", actual);
        item.put("断言结果", assertion);
        item.put("通过", passed);
        CASES.add(item);
        writeReports();
        System.out.println("[E2E " + number + "] " + (passed ? "PASS" : "FAIL") + " " + name);
    }

    private static void writeReports() {
        try {
            Path directory = E2eConfig.ROOT.resolve("target").resolve("e2e-reports");
            Files.createDirectories(directory);
            List<Map<String, Object>> ordered = orderedCases();
            Files.write(directory.resolve("e2e-mainline-matrix.json"),
                    JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ordered).getBytes(StandardCharsets.UTF_8));
            Files.write(directory.resolve("e2e-mainline-matrix.md"), markdown().getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            System.err.println("Unable to write E2E matrix report: " + exception.getMessage());
        }
    }

    private static String markdown() throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("# 端到端测试矩阵\n\n");
        output.append("| 测试名称 | 测试编号 | 测试函数 | 输入数据 | 前置条件 | 预期输出 | 实际输出 | 断言结果 |\n");
        output.append("|---|---|---|---|---|---|---|---|\n");
        for (Map<String, Object> item : orderedCases()) {
            output.append(cell(item.get("测试名称"))).append('|')
                    .append(cell(item.get("测试编号"))).append('|')
                    .append(cell(item.get("测试函数"))).append('|')
                    .append(cell(JSON.writeValueAsString(item.get("输入数据")))).append('|')
                    .append(cell(item.get("前置条件"))).append('|')
                    .append(cell(item.get("预期输出"))).append('|')
                    .append(cell(JSON.writeValueAsString(item.get("实际输出")))).append('|')
                    .append(cell(item.get("断言结果"))).append('|').append('\n');
        }
        return output.toString();
    }

    private static List<Map<String, Object>> orderedCases() {
        List<Map<String, Object>> ordered = new ArrayList<>(CASES);
        Collections.sort(ordered, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return order(String.valueOf(left.get("测试编号"))) - order(String.valueOf(right.get("测试编号")));
            }

            private int order(String number) {
                if ("E000".equals(number)) return 0;
                if ("S000".equals(number)) return 1;
                if ("T000".equals(number)) return 2;
                if ("A000".equals(number)) return 3;
                if ("F010".equals(number)) return 4;
                return 99;
            }
        });
        return ordered;
    }

    private static String cell(Object value) {
        if (value == null) return "";
        return String.valueOf(value).replace("|", "\\|").replace("\r", " ").replace("\n", "<br>");
    }
}
