package com.example.resumeinsight;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_history")
public class AnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills; // Comma-separated

    @Column(columnDefinition = "TEXT")
    private String missingSkills; // Comma-separated

    private int score;

    private String bestRole;

    @Column(columnDefinition = "TEXT")
    private String grammarIssuesJson; // Serialized JSON string of detected issues

    @Column(columnDefinition = "TEXT")
    private String roadmap; // AI 3-month roadmap details

    private LocalDateTime timestamp;

    private String sessionId;

    public AnalysisHistory() {}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getBestRole() {
        return bestRole;
    }

    public void setBestRole(String bestRole) {
        this.bestRole = bestRole;
    }

    public String getGrammarIssuesJson() {
        return grammarIssuesJson;
    }

    public void setGrammarIssuesJson(String grammarIssuesJson) {
        this.grammarIssuesJson = grammarIssuesJson;
    }

    public String getRoadmap() {
        return roadmap;
    }

    public void setRoadmap(String roadmap) {
        this.roadmap = roadmap;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
