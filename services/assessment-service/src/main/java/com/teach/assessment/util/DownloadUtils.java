package com.teach.assessment.util;

import java.nio.file.Paths;

/** Minimal filename sanitising helper used when rendering exam attachments. */
public final class DownloadUtils {
    private DownloadUtils() {}
    public static String displayFilename(String name) {
        if (name == null || name.trim().isEmpty()) return "attachment";
        try { return Paths.get(name).getFileName().toString(); } catch (Exception e) { return "attachment"; }
    }
}
