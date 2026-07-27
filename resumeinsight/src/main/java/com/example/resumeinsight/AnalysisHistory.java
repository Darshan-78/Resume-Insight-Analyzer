package com.example.resumeinsight;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_history")
public class AnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String filename;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills; // Comma-separated

    @Column(columnDefinition = "TEXT")
    private String missingSkills; // Comma-separated

    @Column(columnDefinition = "TEXT")
    private String matchedSoftSkills; // Comma-separated

    @Column(columnDefinition = "TEXT")
    private String missingSoftSkills; // Comma-separated

    @Column(columnDefinition = "TEXT")
    private String atsWarnings; // Comma-separated (joined by ;)

    private int score;

    @Column(columnDefinition = "TEXT")
    private String bestRole;

    @Column(columnDefinition = "TEXT")
    private String grammarIssuesJson; // Serialized JSON string of detected issues

    @Column(columnDefinition = "TEXT")
    private String roadmap; // AI 3-month roadmap details

    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
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

    public String getMatchedSoftSkills() {
        return matchedSoftSkills;
    }

    public void setMatchedSoftSkills(String matchedSoftSkills) {
        this.matchedSoftSkills = matchedSoftSkills;
    }

    public String getMissingSoftSkills() {
        return missingSoftSkills;
    }

    public void setMissingSoftSkills(String missingSoftSkills) {
        this.missingSoftSkills = missingSoftSkills;
    }

    public String getAtsWarnings() {
        return atsWarnings;
    }

    public void setAtsWarnings(String atsWarnings) {
        this.atsWarnings = atsWarnings;
    }
}
