package com.example.resumeinsight;
import java.util.List;

public class ResumeRequest {

    private String text;
    private String selectedRole;
    private String jobDescription;
    private String filename;
    private List<String> atsWarnings;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSelectedRole() {
        return selectedRole;
    }

    public void setSelectedRole(String selectedRole) {
        this.selectedRole = selectedRole;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<String> getAtsWarnings() {
        return atsWarnings;
    }

    public void setAtsWarnings(List<String> atsWarnings) {
        this.atsWarnings = atsWarnings;
    }
}