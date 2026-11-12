package com.virtualclassroom.service;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForumService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final SubjectRepository subjectRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final FacultySubjectAssignmentRepository facultyAssignmentRepository;

    public ForumService(
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            SubjectRepository subjectRepository,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this(questionRepository, answerRepository, subjectRepository, notificationService, auditLogService, null);
    }

    @Autowired
    public ForumService(
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            SubjectRepository subjectRepository,
            NotificationService notificationService,
            AuditLogService auditLogService,
            @Autowired(required = false) FacultySubjectAssignmentRepository facultyAssignmentRepository
    ) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.subjectRepository = subjectRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.facultyAssignmentRepository = facultyAssignmentRepository;
    }


    @Transactional(readOnly = true)
    public List<QuestionDto> getAllQuestions() {
        return questionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestionsBySubject(Integer subjectId) {
        return questionRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionDetails(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        List<AnswerDto> answers = answerRepository.findByQuestionIdOrderByCreatedAtAsc(questionId).stream()
                .map(this::toAnswerDto)
                .collect(Collectors.toList());

        QuestionDto dto = toSummaryDto(question);
        dto.setAnswers(answers);
        return dto;
    }

    @Transactional
    public QuestionDto postQuestion(CreateQuestionRequest request, User author, String ipAddress) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        Question question = Question.builder()
                .subject(subject)
                .author(author)
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .isResolved(false)
                .build();

        Question saved = questionRepository.save(question);

        auditLogService.logAction(author, "POST_FORUM_QUESTION", "questions", saved.getId().toString(),
                "Posted question: " + saved.getTitle(), ipAddress);

        return toSummaryDto(saved);
    }

    @Transactional
    public AnswerDto postAnswer(Long questionId, CreateAnswerRequest request, User author, String ipAddress) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        Answer answer = Answer.builder()
                .question(question)
                .author(author)
                .body(request.getBody().trim())
                .isAccepted(false)
                .build();

        Answer saved = answerRepository.save(answer);

        if (!question.getAuthor().getId().equals(author.getId())) {
            notificationService.createNotification(
                    question.getAuthor(),
                    "New Reply to Your Question",
                    author.getFullName() + " replied to your question: \"" + question.getTitle() + "\"",
                    "FORUM_REPLY"
            );
        }

        auditLogService.logAction(author, "POST_FORUM_ANSWER", "answers", saved.getId().toString(),
                "Posted answer for question ID: " + questionId, ipAddress);

        return toAnswerDto(saved);
    }

    @Transactional
    public void markQuestionResolved(Long questionId, User actor) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (!question.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only the author of the question or an admin can mark a question as resolved");
        }
        question.setIsResolved(true);
        questionRepository.save(question);
    }

    @Transactional
    public void markAnswerAccepted(Long answerId, User actor) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found"));
        Question question = answer.getQuestion();
        if (!question.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only the author of the question or an admin can mark an answer as accepted");
        }
        answer.setIsAccepted(true);
        answerRepository.save(answer);
        question.setIsResolved(true);
        questionRepository.save(question);
    }

    @Transactional
    public QuestionDto updateQuestion(Long questionId, UpdateQuestionRequest request, User actor, String ipAddress) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        if (!question.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only the original author or an administrator can edit this question");
        }

        question.setTitle(request.getTitle().trim());
        question.setBody(request.getBody().trim());
        Question saved = questionRepository.save(question);

        auditLogService.logAction(actor, "UPDATE_FORUM_QUESTION", "questions", saved.getId().toString(),
                "Updated question: " + saved.getTitle(), ipAddress);

        return toSummaryDto(saved);
    }

    @Transactional
    public void deleteQuestion(Long questionId, User actor, String ipAddress) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        if (!question.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only the original author or an administrator can delete this question");
        }

        questionRepository.delete(question);
        auditLogService.logAction(actor, "DELETE_FORUM_QUESTION", "questions", questionId.toString(),
                "Deleted question: " + question.getTitle(), ipAddress);
    }

    @Transactional
    public AnswerDto updateAnswer(Long answerId, UpdateAnswerRequest request, User actor, String ipAddress) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found with ID: " + answerId));

        if (!answer.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only the original author or an administrator can edit this answer");
        }

        answer.setBody(request.getBody().trim());
        Answer saved = answerRepository.save(answer);

        auditLogService.logAction(actor, "UPDATE_FORUM_ANSWER", "answers", saved.getId().toString(),
                "Updated answer on question ID: " + answer.getQuestion().getId(), ipAddress);

        return toAnswerDto(saved);
    }

    @Transactional
    public void deleteAnswer(Long answerId, User actor, String ipAddress) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found with ID: " + answerId));

        if (!answer.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only the original author or an administrator can delete this answer");
        }

        answerRepository.delete(answer);
        auditLogService.logAction(actor, "DELETE_FORUM_ANSWER", "answers", answerId.toString(),
                "Deleted answer ID: " + answerId, ipAddress);
    }

    @Transactional
    public AnswerDto endorseAnswer(Long answerId, User facultyUser, String ipAddress) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found with ID: " + answerId));

        if (facultyUser.getRole() != Role.ROLE_ADMIN) {
            boolean assigned = facultyAssignmentRepository != null && facultyAssignmentRepository.existsByFacultyIdAndSubjectId(
                    facultyUser.getId(), answer.getQuestion().getSubject().getId());
            if (!assigned) {
                throw new AccessDeniedException("Only the assigned faculty instructor for this subject or an administrator can endorse answers");
            }
        }

        answer.setIsEndorsed(true);
        answer.setEndorsedBy(facultyUser);
        Answer saved = answerRepository.save(answer);

        if (!answer.getAuthor().getId().equals(facultyUser.getId())) {
            notificationService.createNotification(
                    answer.getAuthor(),
                    "Answer Endorsed by Faculty",
                    facultyUser.getFullName() + " endorsed your answer to: \"" + answer.getQuestion().getTitle() + "\"",
                    "ANSWER_ENDORSED"
            );
        }

        auditLogService.logAction(facultyUser, "ENDORSE_FORUM_ANSWER", "answers", saved.getId().toString(),
                "Endorsed answer by " + answer.getAuthor().getEmail(), ipAddress);

        return toAnswerDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<QuestionDto> searchQuestions(String query, Integer subjectId, Pageable pageable) {
        return questionRepository.searchQuestions(query, subjectId, pageable).map(this::toSummaryDto);
    }

    private QuestionDto toSummaryDto(Question q) {
        return QuestionDto.builder()
                .id(q.getId())
                .subjectId(q.getSubject().getId())
                .subjectTitle(q.getSubject().getTitle())
                .authorId(q.getAuthor().getId())
                .authorName(q.getAuthor().getFullName())
                .authorRole(q.getAuthor().getRole().name())
                .title(q.getTitle())
                .body(q.getBody())
                .isResolved(q.getIsResolved())
                .answersCount(q.getAnswers() != null ? q.getAnswers().size() : 0)
                .createdAt(q.getCreatedAt())
                .build();
    }

    private AnswerDto toAnswerDto(Answer a) {
        AnswerDto.Builder builder = AnswerDto.builder()
                .id(a.getId())
                .questionId(a.getQuestion().getId())
                .authorId(a.getAuthor().getId())
                .authorName(a.getAuthor().getFullName())
                .authorRole(a.getAuthor().getRole().name())
                .body(a.getBody())
                .isAccepted(a.getIsAccepted())
                .isEndorsed(a.getIsEndorsed())
                .createdAt(a.getCreatedAt());

        if (a.getEndorsedBy() != null) {
            builder.endorsedById(a.getEndorsedBy().getId())
                    .endorsedByName(a.getEndorsedBy().getFullName());
        }

        return builder.build();
    }
}
