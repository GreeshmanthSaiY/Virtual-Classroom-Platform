package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload to update an existing discussion answer")
public class UpdateAnswerRequest {

    @NotBlank(message = "Answer body cannot be blank")
    @Schema(description = "Updated text of the answer", example = "Updated clarification on DFS stack usage...")
    private String body;

    public UpdateAnswerRequest() {}

    public UpdateAnswerRequest(String body) {
        this.body = body;
    }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
