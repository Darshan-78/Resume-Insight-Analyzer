package com.example.resumeinsight;
import java.util.List;

public class ResumeResponse {
    private List<String> detectedSkills;
    private List<RoleMatchResponse> roleRanking;
    private String bestRoleRecommendation;

    public ResumeResponse(List<String> detectedSkills, List<RoleMatchResponse> roleRanking, String bestRoleRecommendation) {
        this.detectedSkills = detectedSkills;
        this.roleRanking = roleRanking;
        this.bestRoleRecommendation = bestRoleRecommendation;
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
}