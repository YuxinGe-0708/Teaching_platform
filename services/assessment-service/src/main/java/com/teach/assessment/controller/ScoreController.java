package com.teach.assessment.controller;

import com.teach.assessment.dto.ApiResponse;
import com.teach.assessment.entity.Submission;
import com.teach.assessment.mapper.SubmissionMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/internal/scores")
public class ScoreController {
    private final SubmissionMapper submissions;
    public ScoreController(SubmissionMapper submissions){this.submissions=submissions;}
    @GetMapping("/student/{studentId}") public ApiResponse<List<Submission>> student(@PathVariable Long studentId){return ApiResponse.ok(submissions.findByStudentId(studentId));}
    @GetMapping("/student/{studentId}/course/{courseId}") public ApiResponse<Map<String,Object>> studentCourse(@PathVariable Long studentId,@PathVariable Long courseId){return ApiResponse.ok(summary(submissions.findByStudentId(studentId),courseId));}
    @GetMapping("/course/{courseId}") public ApiResponse<Map<String,Object>> course(@PathVariable Long courseId){return ApiResponse.ok(summary(submissions.findByCourseId(courseId),courseId));}
    @GetMapping("/course/{courseId}/export") public ApiResponse<List<Submission>> export(@PathVariable Long courseId){return ApiResponse.ok(submissions.findByCourseId(courseId));}
    private Map<String,Object> summary(List<Submission> rows,Long courseId){Map<String,Object> m=new LinkedHashMap<>();m.put("courseId",courseId);m.put("submissions",rows);double total=0;int n=0;for(Submission s:rows)if(s.getScore()!=null){total+=s.getScore();n++;}m.put("averageScore",n==0?0:total/n);m.put("submittedCount",rows.size());return m;}
}
