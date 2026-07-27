package com.example.resumeinsight;
import java.util.List;

public class ResumeResponse {
    private List<String> detectedSkills;
    private List<RoleMatchResponse> roleRanking;
    private String bestRoleRecommendation;

    // Upgraded portfolio response fields
    private boolean jdMatchMode;
    private List<String> jdSkills;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private int score;
    private List<GrammarIssue> grammarIssues;
    private Long id;

    // Additional portfolio response fields
    private List<String> structureSuggestions;
    private List<BulletFeedbackItem> bulletFeedback;
    private List<String> atsWarnings;
    private List<PrioritizedSkill> missingSkillsPrioritized;

    private List<String> detectedSoftSkills;
    private List<String> matchedSoftSkills;
    private List<String> missingSoftSkills;

    public static class BulletFeedbackItem {
        private String originalBullet;
        private List<String> suggestions;

        public BulletFeedbackItem() {}

        public BulletFeedbackItem(String originalBullet, List<String> suggestions) {
            this.originalBullet = originalBullet;
            this.suggestions = suggestions;
        }

        public String getOriginalBullet() { return originalBullet; }
        public void setOriginalBullet(String originalBullet) { this.originalBullet = originalBullet; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }

    public static class PrioritizedSkill {
        private String name;
        private String priority; // "Critical" or "Nice to have"

        public PrioritizedSkill() {}

        public PrioritizedSkill(String name, String priority) {
            this.name = name;
            this.priority = priority;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    public ResumeResponse(List<String> detectedSkills, List<RoleMatchResponse> roleRanking, String bestRoleRecommendation) {
        this.detectedSkills = detectedSkills;
        this.roleRanking = roleRanking;
        this.bestRoleRecommendation = bestRoleRecommendation;
    }

    public ResumeResponse(List<String> detectedSkills, List<RoleMatchResponse> roleRanking, String bestRoleRecommendation,
                          boolean jdMatchMode, List<String> jdSkills, List<String> matchedSkills, List<String> missingSkills,
                          int score, List<GrammarIssue> grammarIssues, Long id) {
        this.detectedSkills = detectedSkills;
        this.roleRanking = roleRanking;
        this.bestRoleRecommendation = bestRoleRecommendation;
        this.jdMatchMode = jdMatchMode;
        this.jdSkills = jdSkills;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.score = score;
        this.grammarIssues = grammarIssues;
        this.id = id;
    }

    public ResumeResponse(List<String> detectedSkills, List<RoleMatchResponse> roleRanking, String bestRoleRecommendation,
                          boolean jdMatchMode, List<String> jdSkills, List<String> matchedSkills, List<String> missingSkills,
                          int score, List<GrammarIssue> grammarIssues, Long id,
                          List<String> structureSuggestions, List<BulletFeedbackItem> bulletFeedback,
                          List<String> atsWarnings, List<PrioritizedSkill> missingSkillsPrioritized) {
        this.detectedSkills = detectedSkills;
        this.roleRanking = roleRanking;
        this.bestRoleRecommendation = bestRoleRecommendation;
        this.jdMatchMode = jdMatchMode;
        this.jdSkills = jdSkills;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.score = score;
        this.grammarIssues = grammarIssues;
        this.id = id;
        this.structureSuggestions = structureSuggestions;
        this.bulletFeedback = bulletFeedback;
        this.atsWarnings = atsWarnings;
        this.missingSkillsPrioritized = missingSkillsPrioritized;
    }

    public ResumeResponse(List<String> detectedSkills, List<RoleMatchResponse> roleRanking, String bestRoleRecommendation,
                          boolean jdMatchMode, List<String> jdSkills, List<String> matchedSkills, List<String> missingSkills,
                          int score, List<GrammarIssue> grammarIssues, Long id,
                          List<String> structureSuggestions, List<BulletFeedbackItem> bulletFeedback,
                          List<String> atsWarnings, List<PrioritizedSkill> missingSkillsPrioritized,
                          List<String> detectedSoftSkills, List<String> matchedSoftSkills, List<String> missingSoftSkills) {
        this.detectedSkills = detectedSkills;
        this.roleRanking = roleRanking;
        this.bestRoleRecommendation = bestRoleRecommendation;
        this.jdMatchMode = jdMatchMode;
        this.jdSkills = jdSkills;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.score = score;
        this.grammarIssues = grammarIssues;
        this.id = id;
        this.structureSuggestions = structureSuggestions;
        this.bulletFeedback = bulletFeedback;
        this.atsWarnings = atsWarnings;
        this.missingSkillsPrioritized = missingSkillsPrioritized;
        this.detectedSoftSkills = detectedSoftSkills;
        this.matchedSoftSkills = matchedSoftSkills;
        this.missingSoftSkills = missingSoftSkills;
    }

    public List<String> getDetectedSkills() {
        return detectedSkills;
    }

    public List<RoleMatchResponse> getRoleRanking() {
        return roleRanking;
    }

    public String getBestRoleRecommendation() {
        return bestRoleRecommendation;
    }

    public boolean isJdMatchMode() {
        return jdMatchMode;
    }

    public List<String> getJdSkills() {
        return jdSkills;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public int getScore() {
        return score;
    }

    public List<GrammarIssue> getGrammarIssues() {
        return grammarIssues;
    }

    public Long getId() {
        return id;
    }

    public List<String> getStructureSuggestions() {
        return structureSuggestions;
    }

    public List<BulletFeedbackItem> getBulletFeedback() {
        return bulletFeedback;
    }

    public List<String> getAtsWarnings() {
        return atsWarnings;
    }

    public List<PrioritizedSkill> getMissingSkillsPrioritized() {
        return missingSkillsPrioritized;
    }

    public List<String> getDetectedSoftSkills() {
        return detectedSoftSkills;
    }

    public List<String> getMatchedSoftSkills() {
        return matchedSoftSkills;
    }

    public List<String> getMissingSoftSkills() {
        return missingSoftSkills;
    }
}