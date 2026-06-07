package org.example.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(Model model) {
        model.addAttribute("error", "上传文件过大，请压缩视频或调整 MAX_FILE_SIZE / MAX_REQUEST_SIZE 后重试。");
        return "redirect:/teacher/course/manage";
    }
}
