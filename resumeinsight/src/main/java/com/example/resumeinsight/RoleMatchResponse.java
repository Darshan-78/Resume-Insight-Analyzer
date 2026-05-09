package com.example.resumeinsight;

public class RoleMatchResponse {
     private String role;
    private int score;

    public RoleMatchResponse(String role, int score) {
        this.role = role;
        this.score = score;
    }

    public String getRole() {
        return role;
    }

    public int getScore() {
        return score;
    }
}
