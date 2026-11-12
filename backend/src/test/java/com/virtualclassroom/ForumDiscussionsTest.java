package com.virtualclassroom;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.AnswerRepository;
import com.virtualclassroom.repository.FacultySubjectAssignmentRepository;
import com.virtualclassroom.repository.QuestionRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.ForumService;
import com.virtualclassroom.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumDiscussionsTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private FacultySubjectAssignmentRepository facultyAssignmentRepository;

    private ForumService forumService;
    private User studentAuthor;
    private User otherStudent;
    private User facultyInstructor;
    private Subject subject;
    private Question question;
    private Answer answer;

    @BeforeEach
    void setUp() {
        forumService = new ForumService(
                questionRepository, answerRepository, subjectRepository,
                notificationService, auditLogService, facultyAssignmentRepository
        );

        studentAuthor = User.builder()
                .id(10L)
                .email("author@university.edu")
                .fullName("Question Author")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        otherStudent = User.builder()
                .id(20L)
                .email("other@university.edu")
                .fullName("Other Student")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        facultyInstructor = User.builder()
                .id(30L)
                .email("prof@university.edu")
                .fullName("Prof. Oak")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();

        subject = Subject.builder()
                .id(100)
                .code("CS100")
                .title("Data Structures")
                .yearLevel(1)
                .build();

        question = Question.builder()
                .id(1L)
                .subject(subject)
                .author(studentAuthor)
                .title("What is the time complexity of QuickSelect?")
                .body("Is average O(N) but worst-case O(N^2)?")
                .isResolved(false)
                .createdAt(Instant.now())
                .build();

        answer = Answer.builder()
                .id(2L)
                .question(question)
                .author(otherStudent)
                .body("Yes, QuickSelect uses partition and achieves average O(N).")
                .isAccepted(false)
                .isEndorsed(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Question owner can update question title and body")
    void updateQuestion_Owner_UpdatesSuccessfully() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenReturn(question);

        UpdateQuestionRequest request = new UpdateQuestionRequest(
                "Updated QuickSelect Question", "Updated body context"
        );

        QuestionDto result = forumService.updateQuestion(1L, request, studentAuthor, "127.0.0.1");

        assertNotNull(result);
        assertEquals("Updated QuickSelect Question", question.getTitle());
        verify(questionRepository).save(question);
        verify(auditLogService).logAction(eq(studentAuthor), eq("UPDATE_FORUM_QUESTION"), anyString(), eq("1"), anyString(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Non-owner cannot update someone else's question")
    void updateQuestion_NonOwner_ThrowsAccessDenied() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        UpdateQuestionRequest request = new UpdateQuestionRequest("Hacked Title", "Hacked body");

        assertThrows(AccessDeniedException.class, () ->
                forumService.updateQuestion(1L, request, otherStudent, "127.0.0.1")
        );
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Question owner can delete their question")
    void deleteQuestion_Owner_DeletesSuccessfully() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        forumService.deleteQuestion(1L, studentAuthor, "127.0.0.1");

        verify(questionRepository).delete(question);
        verify(auditLogService).logAction(eq(studentAuthor), eq("DELETE_FORUM_QUESTION"), anyString(), eq("1"), anyString(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Assigned faculty can endorse a helpful answer")
    void endorseAnswer_AssignedFaculty_EndorsesAndNotifiesAuthor() {
        when(answerRepository.findById(2L)).thenReturn(Optional.of(answer));
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(30L, 100)).thenReturn(true);
        when(answerRepository.save(any(Answer.class))).thenReturn(answer);

        AnswerDto result = forumService.endorseAnswer(2L, facultyInstructor, "127.0.0.1");

        assertNotNull(result);
        assertTrue(answer.getIsEndorsed());
        assertEquals(facultyInstructor, answer.getEndorsedBy());
        verify(notificationService).createNotification(eq(otherStudent), eq("Answer Endorsed by Faculty"), anyString(), eq("ANSWER_ENDORSED"));
        verify(auditLogService).logAction(eq(facultyInstructor), eq("ENDORSE_FORUM_ANSWER"), anyString(), eq("2"), anyString(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Unassigned faculty cannot endorse answer")
    void endorseAnswer_UnassignedFaculty_ThrowsAccessDenied() {
        when(answerRepository.findById(2L)).thenReturn(Optional.of(answer));
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(30L, 100)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                forumService.endorseAnswer(2L, facultyInstructor, "127.0.0.1")
        );
        assertFalse(answer.getIsEndorsed());
        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Search questions across title and body with pagination")
    void searchQuestions_ReturnsFilteredPage() {
        Page<Question> mockPage = new PageImpl<>(List.of(question));
        when(questionRepository.searchQuestions("QuickSelect", 100, PageRequest.of(0, 10)))
                .thenReturn(mockPage);

        Page<QuestionDto> results = forumService.searchQuestions("QuickSelect", 100, PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("What is the time complexity of QuickSelect?", results.getContent().get(0).getTitle());
    }
}
