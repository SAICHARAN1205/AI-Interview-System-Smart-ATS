package com.aihiringplatform.backend.dto;

public class CaptchaResponse {

    private String token;
    private String question;

    public CaptchaResponse() {}

    public CaptchaResponse(String token, String question) {
        this.token = token;
        this.question = question;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
