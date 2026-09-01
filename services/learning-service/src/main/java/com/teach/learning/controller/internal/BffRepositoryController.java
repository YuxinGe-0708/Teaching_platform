package com.teach.learning.controller.internal;

import com.teach.learning.dto.ApiResponse;
import com.teach.learning.entity.*;
import com.teach.learning.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Internal persistence facade used only by the Thymeleaf BFF. */
@RestController
@RequestMapping("/internal/bff")
public class BffRepositoryController {
    private final CourseMapper courses;
    private final CourseEnrollmentMapper enrollments;
    private final CourseClassMapper classes;
    private final ResourceMapper resources;
    private final ResourceProgressMapper progress;
    private final StudyNoteMapper notes;
    private final DiscussionPostMapper posts;
    private final DiscussionReplyMapper replies;

    public BffRepositoryController(CourseMapper courses, CourseEnrollmentMapper enrollments,
                                   CourseClassMapper classes, ResourceMapper resources,
                                   ResourceProgressMapper progress, StudyNoteMapper notes,
                                   DiscussionPostMapper posts, DiscussionReplyMapper replies) {
        this.courses=courses; this.enrollments=enrollments; this.classes=classes; this.resources=resources;
        this.progress=progress; this.notes=notes; this.posts=posts; this.replies=replies;
    }

    @GetMapping("/courses/{id}") public ApiResponse<Course> course(@PathVariable Long id){return ApiResponse.ok(courses.findById(id));}
    @GetMapping("/courses") public ApiResponse<List<Course>> courseList(@RequestParam(required=false) Long teacherId,@RequestParam(required=false) Long studentId,@RequestParam(required=false) Boolean active){if(teacherId!=null)return ApiResponse.ok(courses.findByTeacherId(teacherId));if(studentId!=null)return ApiResponse.ok(courses.findByStudentId(studentId));return ApiResponse.ok(courses.findActive());}
    @GetMapping("/courses/by-invite") public ApiResponse<Course> courseInvite(@RequestParam String inviteCode){return ApiResponse.ok(courses.findByInviteCode(inviteCode));}
    @GetMapping("/courses/count") public ApiResponse<Integer> courseCount(){return ApiResponse.ok(courses.countAll());}
    @PostMapping("/courses") public ApiResponse<Course> insertCourse(@RequestBody Course value){courses.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/courses/{id}") public ApiResponse<Integer> updateCourse(@PathVariable Long id,@RequestBody Course value){value.setId(id);return ApiResponse.ok(courses.update(value));}
    @PutMapping("/courses/{id}/status") public ApiResponse<Integer> courseStatus(@PathVariable Long id,@RequestParam String status){return ApiResponse.ok(courses.updateStatus(id,status));}
    @DeleteMapping("/courses/{id}") public ApiResponse<Integer> deleteCourse(@PathVariable Long id){return ApiResponse.ok(courses.deleteById(id));}

    @GetMapping("/enrollments/check") public ApiResponse<CourseEnrollment> enrollment(@RequestParam Long studentId,@RequestParam Long courseId){return ApiResponse.ok(enrollments.findByStudentAndCourse(studentId,courseId));}
    @GetMapping("/enrollments/student/{id}") public ApiResponse<List<CourseEnrollment>> studentEnrollments(@PathVariable Long id){return ApiResponse.ok(enrollments.findByStudentId(id));}
    @GetMapping("/enrollments/course/{id}") public ApiResponse<List<CourseEnrollment>> courseEnrollments(@PathVariable Long id){return ApiResponse.ok(enrollments.findByCourseId(id));}
    @GetMapping("/enrollments/count/{courseId}") public ApiResponse<Integer> enrollmentCount(@PathVariable Long courseId){return ApiResponse.ok(enrollments.countByCourseId(courseId));}
    @PostMapping("/enrollments") public ApiResponse<CourseEnrollment> insertEnrollment(@RequestBody CourseEnrollment value){enrollments.insert(value);return ApiResponse.ok(value);}
    @DeleteMapping("/enrollments") public ApiResponse<Integer> deleteEnrollment(@RequestParam Long studentId,@RequestParam Long courseId){return ApiResponse.ok(enrollments.deleteByStudentAndCourse(studentId,courseId));}

    @GetMapping("/classes/{id}") public ApiResponse<CourseClass> courseClass(@PathVariable Long id){return ApiResponse.ok(classes.findById(id));}
    @GetMapping("/classes") public ApiResponse<List<CourseClass>> classList(@RequestParam Long courseId){return ApiResponse.ok(classes.findByCourseId(courseId));}
    @GetMapping("/classes/by-invite") public ApiResponse<CourseClass> classInvite(@RequestParam String inviteCode){return ApiResponse.ok(classes.findByInviteCode(inviteCode));}
    @PostMapping("/classes") public ApiResponse<CourseClass> insertClass(@RequestBody CourseClass value){classes.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/classes/{id}") public ApiResponse<Integer> updateClass(@PathVariable Long id,@RequestBody CourseClass value){value.setId(id);return ApiResponse.ok(classes.update(value));}
    @PutMapping("/classes/{id}/increment") public ApiResponse<Integer> incrementClass(@PathVariable Long id){return ApiResponse.ok(classes.incrementCount(id));}
    @PutMapping("/classes/{id}/decrement") public ApiResponse<Integer> decrementClass(@PathVariable Long id){return ApiResponse.ok(classes.decrementCount(id));}
    @DeleteMapping("/classes/{id}") public ApiResponse<Integer> deleteClass(@PathVariable Long id){return ApiResponse.ok(classes.deleteById(id));}

    @GetMapping("/resources/{id}") public ApiResponse<Resource> resource(@PathVariable Long id){return ApiResponse.ok(resources.findById(id));}
    @GetMapping("/resources") public ApiResponse<List<Resource>> resourceList(@RequestParam Long courseId,@RequestParam(required=false) String type,@RequestParam(required=false) String chapter){return ApiResponse.ok(resources.searchByCourse(courseId,type,chapter));}
    @GetMapping("/resources/recent") public ApiResponse<List<Resource>> recentResources(){return ApiResponse.ok(resources.findRecent());}
    @PostMapping("/resources") public ApiResponse<Resource> insertResource(@RequestBody Resource value){resources.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/resources/{id}") public ApiResponse<Integer> updateResource(@PathVariable Long id,@RequestBody Resource value){value.setId(id);return ApiResponse.ok(resources.update(value));}
    @PutMapping("/resources/{id}/download") public ApiResponse<Integer> download(@PathVariable Long id){return ApiResponse.ok(resources.incrementDownloadCount(id));}
    @DeleteMapping("/resources/{id}") public ApiResponse<Integer> deleteResource(@PathVariable Long id){return ApiResponse.ok(resources.deleteById(id));}

    @GetMapping("/progress") public ApiResponse<ResourceProgress> progress(@RequestParam Long studentId,@RequestParam Long resourceId){return ApiResponse.ok(progress.findByStudentAndResource(studentId,resourceId));}
    @GetMapping("/progress/student-course") public ApiResponse<List<ResourceProgress>> progressStudentCourse(@RequestParam Long studentId,@RequestParam Long courseId){return ApiResponse.ok(progress.findByStudentAndCourse(studentId,courseId));}
    @GetMapping("/progress/course/{courseId}") public ApiResponse<List<ResourceProgress>> progressCourse(@PathVariable Long courseId){return ApiResponse.ok(progress.findByCourseId(courseId));}
    @PostMapping("/progress") public ApiResponse<ResourceProgress> insertProgress(@RequestBody ResourceProgress value){progress.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/progress") public ApiResponse<Integer> updateProgress(@RequestBody ResourceProgress value){return ApiResponse.ok(progress.update(value));}

    @GetMapping("/notes/{id}") public ApiResponse<StudyNote> note(@PathVariable Long id){return ApiResponse.ok(notes.findById(id));}
    @GetMapping("/notes/student/{id}") public ApiResponse<List<StudyNote>> studentNotes(@PathVariable Long id){return ApiResponse.ok(notes.findByStudentId(id));}
    @GetMapping("/notes/student-course") public ApiResponse<List<StudyNote>> studentCourseNotes(@RequestParam Long studentId,@RequestParam Long courseId){return ApiResponse.ok(notes.findByStudentAndCourse(studentId,courseId));}
    @PostMapping("/notes") public ApiResponse<StudyNote> insertNote(@RequestBody StudyNote value){notes.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/notes/{id}") public ApiResponse<Integer> updateNote(@PathVariable Long id,@RequestBody StudyNote value){value.setId(id);return ApiResponse.ok(notes.update(value));}
    @PutMapping("/notes/{id}/mind-map") public ApiResponse<Integer> updateMindMap(@PathVariable Long id,@RequestBody StudyNote value){value.setId(id);return ApiResponse.ok(notes.updateMindMap(value));}
    @DeleteMapping("/notes/{id}") public ApiResponse<Integer> deleteNote(@PathVariable Long id){return ApiResponse.ok(notes.deleteById(id));}

    @GetMapping("/posts/{id}") public ApiResponse<DiscussionPost> post(@PathVariable Long id){return ApiResponse.ok(posts.findById(id));}
    @GetMapping("/posts") public ApiResponse<List<DiscussionPost>> posts(@RequestParam Long courseId){return ApiResponse.ok(posts.findByCourseId(courseId));}
    @PostMapping("/posts") public ApiResponse<DiscussionPost> insertPost(@RequestBody DiscussionPost value){posts.insert(value);return ApiResponse.ok(value);}
    @GetMapping("/replies") public ApiResponse<List<DiscussionReply>> replies(@RequestParam Long postId){return ApiResponse.ok(replies.findByPostId(postId));}
    @PostMapping("/replies") public ApiResponse<DiscussionReply> insertReply(@RequestBody DiscussionReply value){replies.insert(value);return ApiResponse.ok(value);}
}
