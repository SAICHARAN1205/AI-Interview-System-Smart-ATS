package com.aihiringplatform.backend.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class InterviewAnswerRequest {
    private Integer questionIndex;

    @Size(max = 6000, message = "Answer must be 6000 characters or less")
    private String answer;
    private Integer currentQuestionIndex;
    private Integer elapsedSeconds;

    @Size(max = 6000, message = "Transcript must be 6000 characters or less")
    private String transcript;
    private List<MonitoringEventDto> monitoringEvents;
    private Boolean cameraUsed;
    private Boolean microphoneUsed;

    public Integer getQuestionIndex() {
        return questionIndex;
    }

    public void setQuestionIndex(Integer questionIndex) {
        this.questionIndex = questionIndex;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(Integer currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public Integer getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(Integer elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public java.util.List<MonitoringEventDto> getMonitoringEvents() {
        return monitoringEvents;
    }

    public void setMonitoringEvents(java.util.List<MonitoringEventDto> monitoringEvents) {
        this.monitoringEvents = monitoringEvents;
    }

    public Boolean getCameraUsed() {
        return cameraUsed;
    }

    public void setCameraUsed(Boolean cameraUsed) {
        this.cameraUsed = cameraUsed;
    }

    public Boolean getMicrophoneUsed() {
        return microphoneUsed;
    }

    public void setMicrophoneUsed(Boolean microphoneUsed) {
        this.microphoneUsed = microphoneUsed;
    }
}
