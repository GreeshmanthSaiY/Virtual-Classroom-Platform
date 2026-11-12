package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload to update an existing discussion question")
public class UpdateQuestionRequest {

    @NotBlank(message = "Question title cannot be blank")
    @Size(max = 250, message = "Title must not exceed 250 characters")
    @Schema(description = "Updated title of the question", example = "Updated: How does BFS differ from DFS?")
    private String title;

    @NotBlank(message = "Question body cannot be blank")
    @Schema(description = "Updated body of the question with detailed context", example = "I need clarification on recursion depth limits...")
    private String body;

    public UpdateQuestionRequest() {}

    public UpdateQuestionRequest(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
