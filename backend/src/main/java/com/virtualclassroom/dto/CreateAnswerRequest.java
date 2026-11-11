package com.virtualclassroom.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAnswerRequest {

    @NotBlank(message = "Answer body is required")
    private String body;

    public CreateAnswerRequest() {}

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
