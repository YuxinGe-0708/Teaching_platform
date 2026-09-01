package com.teach.assessment.service;

import com.teach.assessment.entity.Submission;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {
    @Test void resubmissionUpdatesAssessmentDatabaseRecord() {
        TaskMapper tasks=mock(TaskMapper.class); SubmissionMapper submissions=mock(SubmissionMapper.class); Submission old=new Submission();old.setId(4L);old.setContent("old");
        when(submissions.findByStudentAndTask(7L,3L)).thenReturn(old); Submission saved=new TaskService(tasks,submissions).submit(3L,7L,"new");
        assertSame(old,saved);assertEquals("new",saved.getContent());verify(submissions).updateContent(old);verify(submissions,never()).insert(any());
    }
}
