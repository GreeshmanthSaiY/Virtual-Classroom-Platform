package com.virtualclassroom.service;

import com.virtualclassroom.dto.AnswerDto;
import com.virtualclassroom.dto.CreateAnswerRequest;
import com.virtualclassroom.dto.CreateQuestionRequest;
import com.virtualclassroom.dto.QuestionDto;
import com.virtualclassroom.model.Answer;
import com.virtualclassroom.model.Question;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.AnswerRepository;
import com.virtualclassroom.repository.QuestionRepository;
import com.virtualclassroom.repository.SubjectRepository;
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

    public ForumService(QuestionRepository questionRepository, AnswerRepository answerRepository, SubjectRepository subjectRepository, NotificationService notificationService, AuditLogService auditLogService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.subjectRepository = subjectRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
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
        question.setIsResolved(true);
        questionRepository.save(question);
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
        return AnswerDto.builder()
                .id(a.getId())
                .questionId(a.getQuestion().getId())
                .authorId(a.getAuthor().getId())
                .authorName(a.getAuthor().getFullName())
                .authorRole(a.getAuthor().getRole().name())
                .body(a.getBody())
                .isAccepted(a.getIsAccepted())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
