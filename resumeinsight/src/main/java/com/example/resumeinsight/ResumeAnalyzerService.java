package com.example.resumeinsight;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResumeAnalyzerService { 
    class Resume {
    String text;
    String words[];
    String detectedSkills[];

    Resume(String text) {
        this.text = text.toLowerCase().trim();
        this.words = this.text.split("[^a-zA-Z]+");
        }
    }
    class Role {
    String roleName;
    String requiredSkills[];
    int score;
    String level;
    String feedback;
    int matchCount;
    String missingSkills[];

    Role(String roleName, String requiredSkills[]) {
        this.roleName = roleName;
        this.requiredSkills = requiredSkills;
        }
    }
    
// Normalize words (synonyms handling)
    public String[] normalizeWords(String[] words) {

    String[][] synonyms = {
        {"js", "javascript"},
        {"py", "python"},
        {"reactjs", "react"},
        {"node", "nodejs"}
    };

    for(int i = 0; i < words.length; i++) {
        for(int j = 0; j < synonyms.length; j++) {
            if(words[i].equals(synonyms[j][0])) {
                words[i] = synonyms[j][1];
            }
        }
    }

    return words;
}

    // Detect skills
    public String[] detectSkills(String words[], String allSkills[]) {
        String detected[] = new String[words.length];
        int k = 0;

        for(int i=0; i<words.length; i++) {
            if(words[i].length() == 0) {
                continue;
            }

            for(int j=0; j<allSkills.length; j++) {
                if(words[i].equals(allSkills[j])) {
                    detected[k] = words[i];
                    k++;
                    break;
                }
            }
        }

        String result[] = new String[k];
        for(int i=0; i<k; i++) {
            result[i] = detected[i];
        }

        return result;
    }

    // Get level
    public String getLevel(int score) {
        if(score <= 30) return "Beginner";
        else if(score <= 60) return "Intermediate";
        else return "Strong";
    }

    // Get feedback
    public String getFeedback(int score, String role, String missingSkills[]) {

        if(score == 100 || missingSkills.length == 0) {
            return "Excellent match for " + role + "!";
        }

        if(score <= 30) {
            return "Improve fundamentals: " + String.join(", ", missingSkills);
        }
        else if(score <= 60) {
            return "Work on: " + String.join(", ", missingSkills);
        }
        else {
            return "Good, but improve: " + String.join(", ", missingSkills);
        }
    }

    // Analyze resume for all roles
   public ResumeResponse analyze(Resume resume, String selectedRole) {
    StringBuilder report = new StringBuilder();
    List<RoleMatchResponse> roleRanking = new ArrayList<>();
    String bestRoleRecommendation = "";

        // All skills list
        String allSkills[] = {
            "html","css","javascript","react","nodejs","mysql","git",
            "java","springboot","sql","maven","hibernate","junit",
            "python","excel","pandas","matplotlib","seaborn","powerbi",
            "kotlin","android","flutter","firebase","xml",
            "powerpoint","tableau","business_analysis","communication",
            "aws","azure","docker","kubernetes","terraform","linux",
            "numpy","scikit-learn","tensorflow","jupyter"
        };
   
        // Normalize words first
         resume.words = normalizeWords(resume.words);

// Detect + remove duplicates
        String detected[] = detectSkills(resume.words, allSkills);
        String uniqueDetected[] = removeDuplicates(detected);

        resume.detectedSkills = uniqueDetected;

        int k = uniqueDetected.length;

        // No skills case
        if(k == 0) {
            report.append("\nNo skills detected in resume.");
            
           return new ResumeResponse(
           Arrays.asList(resume.detectedSkills),
           roleRanking,
           "No suitable role found"
);
        }

        // Print detected skills
        report.append("Detected Skills:\n");
        report.append("--------------------------------------\n");
        report.append(String.join(", ", uniqueDetected)).append("\n\n");

        // Roles
        String roles[] = {
            "Web Developer", "Java Developer", "Data Analyst",
            "Mobile App Developer", "Business Analyst",
            "Cloud Engineer", "ML Engineer"
        };

        ArrayList<Role> bestRoles = new ArrayList<>();
        int bestScore = -1;

        // Role skills
        String roleSkills[][] = {
            {"html","css","javascript","react","nodejs","mysql","git"},
            {"java","springboot","sql","maven","git","hibernate","junit"},
            {"python","sql","excel","pandas","matplotlib","seaborn","powerbi"},
            {"java","kotlin","android","flutter","git","firebase","xml"},
            {"excel","sql","powerpoint","tableau","business_analysis","communication","python"},
            {"aws","azure","docker","kubernetes","terraform","linux","python"},
            {"python","numpy","pandas","scikit-learn","tensorflow","matplotlib","jupyter"}
        };

         ArrayList<Role> allRoles = new ArrayList<>();
        

        report.append("======================================\n");
        report.append("        RESUME INSIGHT REPORT\n");
        report.append("======================================\n\n");

        // Loop through roles
            for(int i=0; i<roles.length; i++) {

                if(selectedRole != null && !selectedRole.isEmpty() && !roles[i].equals(selectedRole)) {
                continue;
        }

            Role role = new Role(roles[i], roleSkills[i]);

            int matchCount = 0;

            // Count matched skills
            for(int j=0; j<role.requiredSkills.length; j++) {
                for(int x=0; x<k; x++) {
                    if(role.requiredSkills[j].equals(uniqueDetected[x])) {
                        matchCount++;
                        break;
                    }
                }
            }

            role.matchCount = matchCount;

            // Calculate score
            role.score = (matchCount * 100) / role.requiredSkills.length;

            // Find missing skills
            String missingTemp[] = new String[role.requiredSkills.length];
            int m = 0;

            for(int j=0; j<role.requiredSkills.length; j++) {
                boolean found = false;

                for(int x=0; x<k; x++) {
                    if(role.requiredSkills[j].equals(uniqueDetected[x])) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    missingTemp[m] = role.requiredSkills[j];
                    m++;
                }
            }

            // Exact size array
            role.missingSkills = new String[m];
            for(int j=0; j<m; j++) {
                role.missingSkills[j] = missingTemp[j];
            }

            // Level and feedback
            role.level = getLevel(role.score);
            role.feedback = getFeedback(role.score, role.roleName, role.missingSkills);

            if (role.score > bestScore) {
                 bestScore = role.score;
                 bestRoles.clear();
                 bestRoles.add(role);
           }
               else if (role.score == bestScore) {
              bestRoles.add(role);
       }

            // Output
           report.append("Role: ").append(role.roleName).append("\n");
           report.append("Score: ").append(role.score).append("%\n");
           report.append("Level: ").append(role.level).append("\n\n");

          report.append("Matched Skills: ").append(role.matchCount).append("\n");

          if(m > 0) {
                        report.append("Missing Skills: ")
                       .append(String.join(", ", role.missingSkills))
                       .append("\n");
} else {
      report.append("All skills matched!\n");
}

                  report.append("\nFeedback:\n");
                  report.append(role.feedback).append("\n");
                  report.append("\n--------------------------------------\n\n");

         //keep track of best role
        allRoles.add(role);
    }
        // ===== DAY 14: ROLE RANKING =====

        for(int i = 0; i < allRoles.size(); i++) {
            for(int j = i + 1; j < allRoles.size(); j++) {

                if(allRoles.get(j).score > allRoles.get(i).score) {

                Role temp = allRoles.get(i);
                allRoles.set(i, allRoles.get(j));
                allRoles.set(j, temp);
        }
    }
}

    report.append("\n======================================\n");
    report.append("        ROLE RANKING (TOP MATCHES)\n");
    report.append("======================================\n");

    for(int i = 0; i < allRoles.size(); i++) {
        Role r = allRoles.get(i);

        report.append(i + 1)
              .append(". ")
              .append(r.roleName)
              .append(" - ")
              .append(r.score)
              .append("%\n");

    roleRanking.add(
        new RoleMatchResponse(
            r.roleName,
            r.score
        )
    );
}

      report.append("\n======================================\n");
      report.append("    BEST ROLE RECOMMENDATION\n");
      report.append("======================================\n");

if(bestScore < 40) {
    bestRoleRecommendation = "No strong role match detected. Try adding more domain-specific skills.";

    report.append(bestRoleRecommendation).append("\n");
}
else {
    for(int i = 0; i < bestRoles.size(); i++) {
        Role r = bestRoles.get(i);

        report.append(i + 1)
              .append(". ")
              .append(r.roleName)
              .append(" - ")
              .append(r.score)
              .append("%\n");

        bestRoleRecommendation += r.roleName + " - " + r.score + "%\n";
    }
}
       return new ResumeResponse(
        Arrays.asList(resume.detectedSkills),
        roleRanking,
        bestRoleRecommendation.trim()
    );
}

    public String[] removeDuplicates(String detected[]) {
        String uniqueDetected[] = new String[detected.length];
        int k = 0;

        for(int i=0; i<detected.length; i++) {
            boolean found = false;

            for(int j=0; j<k; j++) {
                if(detected[i].equals(uniqueDetected[j])) {
                    found = true;
                    break;
                }
            }

            if(!found) {
                uniqueDetected[k] = detected[i];
                k++;
            }
        }

        String result[] = new String[k];
        for(int i=0; i<k; i++) {
            result[i] = uniqueDetected[i];
        }

        return result;
    }
}