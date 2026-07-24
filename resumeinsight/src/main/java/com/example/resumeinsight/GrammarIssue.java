package com.example.resumeinsight;

import java.util.List;

public class GrammarIssue {
    private String message;
    private String context;
    private List<String> suggestions;

    public GrammarIssue() {}

    public GrammarIssue(String message, String context, List<String> suggestions) {
        this.message = message;
        this.context = context;
        // Limit suggestions list to keep payload lightweight
        this.suggestions = suggestions.size() > 3 ? suggestions.subList(0, 3) : suggestions;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
