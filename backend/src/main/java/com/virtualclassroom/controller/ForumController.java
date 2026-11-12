package com.virtualclassroom.controller;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/discussions", "/api/v1/forum"})
@Tag(name = "Q&A Discussions", description = "Student-Faculty Discussion Threads")
public class ForumController {

    private final ForumService forumService;
    private final UserRepository userRepository;

    public ForumController(ForumService forumService, UserRepository userRepository) {
        this.forumService = forumService;
        this.userRepository = userRepository;
    }


    @GetMapping("/questions")
    @Operation(summary = "Get all questions across subjects or optionally filtered by subjectId")
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getAllQuestions(
            @RequestParam(required = false) Integer subjectId
    ) {
        List<QuestionDto> questions = (subjectId != null)
                ? forumService.getQuestionsBySubject(subjectId)
                : forumService.getAllQuestions();
        return ResponseEntity.ok(ApiResponse.success(questions, "Questions retrieved successfully"));
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get all questions posted in a subject forum")
    public ResponseEntity<ApiResponse<List<QuestionDto>>> getQuestionsBySubject(@PathVariable Integer subjectId) {
        List<QuestionDto> questions = forumService.getQuestionsBySubject(subjectId);
        return ResponseEntity.ok(ApiResponse.success(questions, "Questions retrieved"));
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Get question thread details with all submitted answers")
    public ResponseEntity<ApiResponse<QuestionDto>> getQuestionDetails(@PathVariable Long questionId) {
        QuestionDto question = forumService.getQuestionDetails(questionId);
        return ResponseEntity.ok(ApiResponse.success(question, "Question details retrieved"));
    }

    @PostMapping("/questions")
    @Operation(summary = "Post a new question in the subject forum")
    public ResponseEntity<ApiResponse<QuestionDto>> postQuestion(
            @Valid @RequestBody CreateQuestionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        QuestionDto question = forumService.postQuestion(request, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(question, "Question posted successfully"));
    }

    @PostMapping("/questions/{questionId}/answers")
    @Operation(summary = "Submit an answer to an existing discussion thread")
    public ResponseEntity<ApiResponse<AnswerDto>> postAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody CreateAnswerRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        AnswerDto answer = forumService.postAnswer(questionId, request, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(answer, "Answer submitted successfully"));
    }

    @PatchMapping("/questions/{questionId}/resolve")
    @Operation(summary = "Mark a question as resolved")
    public ResponseEntity<ApiResponse<Void>> resolveQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        forumService.markQuestionResolved(questionId, user);
        return ResponseEntity.ok(ApiResponse.success(null, "Question marked as resolved"));
    }

    @PatchMapping("/answers/{answerId}/accept")
    @Operation(summary = "Mark an answer as accepted")
    public ResponseEntity<ApiResponse<Void>> acceptAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        forumService.markAnswerAccepted(answerId, user);
        return ResponseEntity.ok(ApiResponse.success(null, "Answer accepted successfully"));
    }

    @PatchMapping("/answers/{answerId}/endorse")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Faculty instructor endorsement of a high-quality answer")
    public ResponseEntity<ApiResponse<AnswerDto>> endorseAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        AnswerDto answer = forumService.endorseAnswer(answerId, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(answer, "Answer endorsed successfully"));
    }

    @PutMapping("/questions/{questionId}")
    @Operation(summary = "Update an existing discussion question (Author or Admin)")
    public ResponseEntity<ApiResponse<QuestionDto>> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        QuestionDto question = forumService.updateQuestion(questionId, request, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(question, "Question updated successfully"));
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete an existing discussion question (Author or Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        forumService.deleteQuestion(questionId, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Question deleted successfully"));
    }

    @PutMapping("/answers/{answerId}")
    @Operation(summary = "Update an existing answer (Author or Admin)")
    public ResponseEntity<ApiResponse<AnswerDto>> updateAnswer(
            @PathVariable Long answerId,
            @Valid @RequestBody UpdateAnswerRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        AnswerDto answer = forumService.updateAnswer(answerId, request, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(answer, "Answer updated successfully"));
    }

    @DeleteMapping("/answers/{answerId}")
    @Operation(summary = "Delete an existing answer (Author or Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        forumService.deleteAnswer(answerId, user, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Answer deleted successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search discussion questions by keyword and optional subject with pagination")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<QuestionDto>>> searchQuestions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer subjectId,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable
    ) {
        org.springframework.data.domain.Page<QuestionDto> questions = forumService.searchQuestions(q, subjectId, pageable);
        return ResponseEntity.ok(ApiResponse.success(questions, "Search results retrieved"));
    }
}
