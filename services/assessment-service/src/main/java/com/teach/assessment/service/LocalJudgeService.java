package com.teach.assessment.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalJudgeService {

    private static final long EXECUTION_TIMEOUT_MS = 10000;
    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern ANY_CLASS = Pattern.compile("class\\s+(\\w+)");

    public JudgeService.JudgeResult judge(String code, String language, List<Map<String, String>> testCases) {
        JudgeService.JudgeResult result = new JudgeService.JudgeResult();
        result.usedLocalJudge = true;
        result.totalCases = testCases != null ? testCases.size() : 0;

        if (testCases == null || testCases.isEmpty()) {
            result.status = "IE";
            result.errorMessage = "编程题没有配置测试用例，请联系教师补充期望输出。";
            return result;
        }

        int totalWeight = 0;
        int passedWeight = 0;
        long started = System.currentTimeMillis();
        for (int i = 0; i < testCases.size(); i++) {
            Map<String, String> tc = testCases.get(i);
            JudgeService.CaseResult caseResult = executeLocally(code, language, tc, i + 1);
            result.caseResults.add(caseResult);
            result.timeUsedMs += caseResult.timeMs;
            result.memoryUsedKb = Math.max(result.memoryUsedKb, caseResult.memoryKb);
            int w = JudgeService.parseWeight(tc.get("weight"));
            totalWeight += w;
            if ("AC".equals(caseResult.status)) {
                result.passedCases++;
                passedWeight += w;
            }
            if ("CE".equals(caseResult.status) || "IE".equals(caseResult.status)) {
                result.status = caseResult.status;
                result.errorMessage = caseResult.message;
                result.score = 0;
                return result;
            }
        }

        if (result.timeUsedMs <= 0) {
            result.timeUsedMs = System.currentTimeMillis() - started;
        }
        result.score = totalWeight == 0 ? 0 : (double) passedWeight / totalWeight * 100;
        result.status = result.passedCases == result.totalCases ? "AC" : worstStatus(result.caseResults);
        return result;
    }

    private JudgeService.CaseResult executeLocally(String code, String language, Map<String, String> testCase, int index) {
        JudgeService.CaseResult cr = new JudgeService.CaseResult();
        cr.caseIndex = index;
        cr.input = testCase.getOrDefault("input", "");
        cr.expectedOutput = testCase.getOrDefault("expectedOutput", "").trim();

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("localjudge_");
            long start = System.currentTimeMillis();

            ProcessResult pr = runCode(code, language, cr.input, workDir);
            cr.timeMs = System.currentTimeMillis() - start;

            if (pr.exitCode != 0) {
                if (pr.exitCode == -1) {
                    cr.status = "TLE";
                    cr.message = "Time Limit Exceeded (执行超时)";
                    cr.actualOutput = cr.message;
                } else {
                    cr.status = "RE";
                    cr.message = describeExitCode(pr.exitCode, pr.stderr);
                    cr.actualOutput = pr.stderr.isEmpty() ? pr.stdout : pr.stderr;
                }
                return cr;
            }

            String actualOutput = pr.stdout.trim();
            cr.actualOutput = actualOutput;

            if (actualOutput.equals(cr.expectedOutput)) {
                cr.status = "AC";
                cr.message = "Accepted";
            } else {
                cr.status = "WA";
                cr.message = "Wrong Answer";
            }
        } catch (CompilationException e) {
            cr.status = "CE";
            cr.message = e.getMessage();
            cr.actualOutput = e.getMessage();
        } catch (Exception e) {
            cr.status = "IE";
            cr.message = "本地评测异常：" + e.getMessage();
            cr.actualOutput = cr.message;
        } finally {
            if (workDir != null) {
                try {
                    deleteRecursively(workDir);
                } catch (Exception ignored) {
                }
            }
        }

        return cr;
    }

    private ProcessResult runCode(String code, String language, String input, Path workDir) throws Exception {
        String lang = language != null ? language.trim().toLowerCase() : "python";
        switch (lang) {
            case "python":
            case "py":
                return runPython(code, input, workDir);
            case "java":
                return runJava(code, input, workDir);
            case "c":
            case "gcc":
                return runC(code, input, workDir);
            default:
                throw new Exception("不支持的本地评测语言：" + lang);
        }
    }

    private ProcessResult runPython(String code, String input, Path workDir) throws Exception {
        Path sourceFile = workDir.resolve("solution.py");
        Files.write(sourceFile, code.getBytes());

        ProcessBuilder pb = new ProcessBuilder("python", sourceFile.toAbsolutePath().toString());
        pb.directory(workDir.toFile());
        return execute(pb, input);
    }

    private ProcessResult runJava(String code, String input, Path workDir) throws Exception {
        String className = extractJavaClassName(code);
        Path sourceFile = workDir.resolve(className + ".java");
        Files.write(sourceFile, code.getBytes());

        ProcessBuilder compilePb = new ProcessBuilder("javac", sourceFile.toAbsolutePath().toString());
        compilePb.directory(workDir.toFile());
        ProcessResult compileResult = execute(compilePb, "");
        if (compileResult.exitCode != 0) {
            throw new CompilationException(compileResult.stderr.isEmpty() ? compileResult.stdout : compileResult.stderr);
        }

        ProcessBuilder runPb = new ProcessBuilder("java", "-cp", workDir.toAbsolutePath().toString(), className);
        runPb.directory(workDir.toFile());
        return execute(runPb, input);
    }

    private ProcessResult runC(String code, String input, Path workDir) throws Exception {
        Path sourceFile = workDir.resolve("solution.c");
        Files.write(sourceFile, code.getBytes());

        String exeName = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "solution.exe"
                : "solution";
        Path exeFile = workDir.resolve(exeName);

        ProcessBuilder compilePb = new ProcessBuilder("gcc", sourceFile.toAbsolutePath().toString(),
                "-o", exeFile.toAbsolutePath().toString());
        compilePb.directory(workDir.toFile());
        ProcessResult compileResult = execute(compilePb, "");
        if (compileResult.exitCode != 0) {
            throw new CompilationException(compileResult.stderr.isEmpty() ? compileResult.stdout : compileResult.stderr);
        }

        ProcessBuilder runPb = new ProcessBuilder(exeFile.toAbsolutePath().toString());
        runPb.directory(workDir.toFile());
        return execute(runPb, input);
    }

    private ProcessResult execute(ProcessBuilder pb, String stdin) throws Exception {
        pb.redirectErrorStream(false);
        Process process = pb.start();

        if (stdin != null && !stdin.isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(stdin.getBytes());
                os.flush();
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);

            ProcessResult result = new ProcessResult();
            if (!finished) {
                process.destroyForcibly();
                result.exitCode = -1;
                result.stderr = "Time Limit Exceeded (本地执行超时)";
                return result;
            }

            result.exitCode = process.exitValue();
            result.stdout = stdout != null ? stdout : "";
            result.stderr = stderr != null ? stderr : "";
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String extractJavaClassName(String code) {
        Matcher m = PUBLIC_CLASS.matcher(code);
        if (m.find()) return m.group(1);
        m = ANY_CLASS.matcher(code);
        if (m.find()) return m.group(1);
        return "Main";
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private String worstStatus(List<JudgeService.CaseResult> cases) {
        for (JudgeService.CaseResult cr : cases) if ("CE".equals(cr.status)) return "CE";
        for (JudgeService.CaseResult cr : cases) if ("RE".equals(cr.status)) return "RE";
        for (JudgeService.CaseResult cr : cases) if ("TLE".equals(cr.status)) return "TLE";
        for (JudgeService.CaseResult cr : cases) if ("WA".equals(cr.status)) return "WA";
        return "IE";
    }

    static class ProcessResult {
        int exitCode;
        String stdout = "";
        String stderr = "";
    }

    private String describeExitCode(int code, String stderr) {
        if (stderr != null && !stderr.isEmpty()) return stderr;
        if (code > 128 && code <= 192) {
            int sig = code - 128;
            String name = signalName(sig);
            return name != null ? name + " (signal " + sig + ")" : "进程被信号 " + sig + " 终止";
        }
        return "Runtime Error (exit code " + code + ")";
    }

    private String signalName(int sig) {
        switch (sig) {
            case 4:  return "Illegal instruction";
            case 6:  return "Aborted";
            case 8:  return "Floating point exception";
            case 11: return "Segmentation fault";
            case 5:  return "Trace/BPT trap";
            default: return null;
        }
    }

    static class CompilationException extends Exception {
        CompilationException(String message) {
            super(message);
        }
    }
}
