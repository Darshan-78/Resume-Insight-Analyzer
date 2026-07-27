package com.example.resumeinsight;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.CategoryId;

public class ResumeAnalyzerService {

    // Predefined skills pool
    public static final String[] ALL_SKILLS = {
        "html", "css", "javascript", "react", "nodejs", "mysql", "git",
        "java", "springboot", "sql", "maven", "hibernate", "junit",
        "python", "excel", "pandas", "matplotlib", "seaborn", "powerbi",
        "kotlin", "android", "flutter", "firebase", "xml",
        "powerpoint", "tableau", "business_analysis",
        "aws", "azure", "docker", "kubernetes", "terraform", "linux",
        "numpy", "scikit-learn", "tensorflow", "jupyter"
    };

    public static final List<String> SOFT_SKILLS_KEYS = Arrays.asList(
        "communication", "teamwork_collaboration", "problem_solving", "leadership",
        "time_management", "adaptability", "attention_to_detail", "critical_thinking",
        "work_ethic", "ownership_accountability"
    );

    public static final Map<String, String> SKILL_DISPLAY_NAMES = new HashMap<>();
    static {
        SKILL_DISPLAY_NAMES.put("html", "HTML");
        SKILL_DISPLAY_NAMES.put("css", "CSS");
        SKILL_DISPLAY_NAMES.put("javascript", "JavaScript");
        SKILL_DISPLAY_NAMES.put("react", "React");
        SKILL_DISPLAY_NAMES.put("nodejs", "Node.js");
        SKILL_DISPLAY_NAMES.put("mysql", "MySQL");
        SKILL_DISPLAY_NAMES.put("git", "Git");
        SKILL_DISPLAY_NAMES.put("java", "Java");
        SKILL_DISPLAY_NAMES.put("springboot", "Spring Boot");
        SKILL_DISPLAY_NAMES.put("sql", "SQL");
        SKILL_DISPLAY_NAMES.put("maven", "Maven");
        SKILL_DISPLAY_NAMES.put("hibernate", "Hibernate");
        SKILL_DISPLAY_NAMES.put("junit", "JUnit");
        SKILL_DISPLAY_NAMES.put("python", "Python");
        SKILL_DISPLAY_NAMES.put("excel", "Excel");
        SKILL_DISPLAY_NAMES.put("pandas", "Pandas");
        SKILL_DISPLAY_NAMES.put("matplotlib", "Matplotlib");
        SKILL_DISPLAY_NAMES.put("seaborn", "Seaborn");
        SKILL_DISPLAY_NAMES.put("powerbi", "Power BI");
        SKILL_DISPLAY_NAMES.put("kotlin", "Kotlin");
        SKILL_DISPLAY_NAMES.put("android", "Android");
        SKILL_DISPLAY_NAMES.put("flutter", "Flutter");
        SKILL_DISPLAY_NAMES.put("firebase", "Firebase");
        SKILL_DISPLAY_NAMES.put("xml", "XML");
        SKILL_DISPLAY_NAMES.put("powerpoint", "PowerPoint");
        SKILL_DISPLAY_NAMES.put("tableau", "Tableau");
        SKILL_DISPLAY_NAMES.put("business_analysis", "Business Analysis");
        SKILL_DISPLAY_NAMES.put("communication", "Communication");
        SKILL_DISPLAY_NAMES.put("aws", "AWS");
        SKILL_DISPLAY_NAMES.put("azure", "Azure");
        SKILL_DISPLAY_NAMES.put("docker", "Docker");
        SKILL_DISPLAY_NAMES.put("kubernetes", "Kubernetes");
        SKILL_DISPLAY_NAMES.put("terraform", "Terraform");
        SKILL_DISPLAY_NAMES.put("linux", "Linux");
        SKILL_DISPLAY_NAMES.put("numpy", "NumPy");
        SKILL_DISPLAY_NAMES.put("scikit-learn", "Scikit-Learn");
        SKILL_DISPLAY_NAMES.put("tensorflow", "TensorFlow");
        SKILL_DISPLAY_NAMES.put("jupyter", "Jupyter");

        // Soft skills mapping
        SKILL_DISPLAY_NAMES.put("teamwork_collaboration", "Teamwork/Collaboration");
        SKILL_DISPLAY_NAMES.put("problem_solving", "Problem-solving");
        SKILL_DISPLAY_NAMES.put("leadership", "Leadership");
        SKILL_DISPLAY_NAMES.put("time_management", "Time Management");
        SKILL_DISPLAY_NAMES.put("adaptability", "Adaptability");
        SKILL_DISPLAY_NAMES.put("attention_to_detail", "Attention to Detail");
        SKILL_DISPLAY_NAMES.put("critical_thinking", "Critical Thinking");
        SKILL_DISPLAY_NAMES.put("work_ethic", "Work Ethic");
        SKILL_DISPLAY_NAMES.put("ownership_accountability", "Ownership/Accountability");
    }

    private static final Map<String, Pattern> SOFT_SKILL_PATTERNS = new LinkedHashMap<>();
    static {
        SOFT_SKILL_PATTERNS.put("communication", Pattern.compile("\\b(communication(\\s+skills)?|verbal\\s+and\\s+written\\s+communication)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("teamwork_collaboration", Pattern.compile("\\b(teamwork|collaboration|collaborative|team\\s+player|cross[-\\s_]functional\\s+collaboration)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("problem_solving", Pattern.compile("\\b(problem[-\\s_]solving(\\s+skills)?|analytical\\s+thinking)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("leadership", Pattern.compile("\\b(leadership)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("time_management", Pattern.compile("\\b(time[-\\s_]management)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("adaptability", Pattern.compile("\\b(adaptability|adaptable)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("attention_to_detail", Pattern.compile("\\b(attention\\s+to\\s+detail)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("critical_thinking", Pattern.compile("\\b(critical\\s+thinking)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("work_ethic", Pattern.compile("\\b(work\\s+ethic)\\b", Pattern.CASE_INSENSITIVE));
        SOFT_SKILL_PATTERNS.put("ownership_accountability", Pattern.compile("\\b(ownership|accountability|accountable)\\b", Pattern.CASE_INSENSITIVE));
    }

    public List<String> extractSoftSkills(String text) {
        List<String> detected = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return detected;
        }
        for (Map.Entry<String, Pattern> entry : SOFT_SKILL_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                detected.add(entry.getKey());
            }
        }
        return detected;
    }

    public static String getSkillDisplayName(String skill) {
        if (skill == null) return "";
        return SKILL_DISPLAY_NAMES.getOrDefault(skill.toLowerCase().trim(), skill);
    }

    public static List<String> formatSkills(List<String> skills) {
        List<String> formatted = new ArrayList<>();
        if (skills != null) {
            for (String skill : skills) {
                formatted.add(getSkillDisplayName(skill));
            }
        }
        return formatted;
    }

    // Predefined roles
    public static final String[] ROLES = {
        "Web Developer", "Java Developer", "Data Analyst",
        "Mobile App Developer", "Business Analyst",
        "Cloud Engineer", "ML Engineer"
    };

    // Predefined skill sets mapped to roles
    public static final String[][] ROLE_SKILLS = {
        {"html", "css", "javascript", "react", "nodejs", "mysql", "git"},
        {"java", "springboot", "sql", "maven", "git", "hibernate", "junit"},
        {"python", "sql", "excel", "pandas", "matplotlib", "seaborn", "powerbi"},
        {"java", "kotlin", "android", "flutter", "git", "firebase", "xml"},
        {"excel", "sql", "powerpoint", "tableau", "business_analysis", "communication", "python"},
        {"aws", "azure", "docker", "kubernetes", "terraform", "linux", "python"},
        {"python", "numpy", "pandas", "scikit-learn", "tensorflow", "matplotlib", "jupyter"}
    };

    public static class RoleMapping {
        private String roleName;
        private List<String> requiredSkills;

        public RoleMapping() {}

        public RoleMapping(String roleName, List<String> requiredSkills) {
            this.roleName = roleName;
            this.requiredSkills = requiredSkills;
        }

        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    }

    public static final List<RoleMapping> ROLE_MAPPINGS = new ArrayList<>();

    static {
        try (java.io.InputStream is = ResumeAnalyzerService.class.getClassLoader().getResourceAsStream("roles-skills.json")) {
            if (is != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                RoleMapping[] mappings = mapper.readValue(is, RoleMapping[].class);
                ROLE_MAPPINGS.addAll(Arrays.asList(mappings));
            } else {
                System.err.println("Warning: roles-skills.json not found in resources. Initializing fallback roles.");
                initializeFallbackRoles();
            }
        } catch (Exception e) {
            System.err.println("Error reading roles-skills.json: " + e.getMessage());
            initializeFallbackRoles();
        }
    }

    private static void initializeFallbackRoles() {
        ROLE_MAPPINGS.clear();
        for (int i = 0; i < ROLES.length; i++) {
            ROLE_MAPPINGS.add(new RoleMapping(ROLES[i], Arrays.asList(ROLE_SKILLS[i])));
        }
    }

    // Nested classes to maintain backward compatibility with any direct class instantiations
    public class Resume {
        String text;
        String[] detectedSkills;

        public Resume(String text) {
            this.text = text;
        }
    }

    public class Role {
        String roleName;
        String[] requiredSkills;
        int score;
        String level;
        String feedback;
        String[] missingSkills;

        public Role(String roleName, String[] requiredSkills) {
            this.roleName = roleName;
            this.requiredSkills = requiredSkills;
        }
    }

    /**
     * Preprocesses text, normalizes whitespace, and resolves synonyms using word boundaries.
     */
    public String normalizeAndApplySynonyms(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.toLowerCase().trim();

        // Synonym replacements (with boundaries to prevent partial word collision)
        normalized = normalized.replaceAll("\\bjs\\b", "javascript");
        normalized = normalized.replaceAll("\\breactjs\\b", "react");
        normalized = normalized.replaceAll("\\bpy\\b", "python");
        normalized = normalized.replaceAll("\\bnode\\b", "nodejs");
        normalized = normalized.replaceAll("\\bspring\\s+boot\\b", "springboot");

        return normalized;
    }

    /**
     * Extracts predefined skills from text using regex boundaries, supporting hyphens and underscores.
     */
    public List<String> extractSkills(String text) {
        List<String> detected = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return detected;
        }
        String normalized = normalizeAndApplySynonyms(text);

        for (String skill : ALL_SKILLS) {
            // Replace '-' or '_' with generic regex separator [-_\\s]?
            String parsedSkill = skill.replace("-", "[-\\s_]?").replace("_", "[-\\s_]?");
            Pattern pattern = Pattern.compile("\\b" + parsedSkill + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                detected.add(skill);
            }
        }
        return detected;
    }

    private static final Set<String> IGNORED_WORDS = new HashSet<>(Arrays.asList(
        // Acronyms / Resume terms
        "CGPA", "GPA", "BTECH", "MTECH", "BCA", "MCA", "BSC", "MSC", "BBA", "MBA", "BE", "ME",
        "SSC", "HSC", "CBSE", "RBSE", "ICSE", "IB", "IGCSE", "AICTE", "UGC",
        // Event / Competition names
        "HACKSPLOSION", "CODEVITA", "LEETCODE", "HACKERRANK", "GEEKSFORGEEKS", "GITHUB", "LINKEDIN", "HACKEREARTH",
        // Common Indian city names
        "SAGWARA", "JAIPUR", "UDAIPUR", "MUMBAI", "DELHI", "BANGALORE", "BENGALURU", "PUNE", "CHENNAI", 
        "HYDERABAD", "KOLKATA", "AHMEDABAD", "SURAT", "NOIDA", "GURGAON", "GURUGRAM", "KOTA", "AJMER", "JODHPUR", "BIKANER",
        // Common Indian university abbreviations / names
        "JECRC", "RTU", "LPU", "VIT", "NIT", "IIT", "BITS", "VTU", "UPTU", "AKTU", "BPUT", "RGPV", "GTU", "WBSCTE"
    ));

    private static final Set<String> CONTEXT_KEYWORDS = new HashSet<>(Arrays.asList(
        "university", "college", "school", "institute", "academy", "board", 
        "city", "district", "state", "center", "centre", "campus", "tech", 
        "technology", "science", "sciences"
    ));

    private boolean isTitleCaseOrAllCaps(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        char firstChar = word.charAt(0);
        return Character.isUpperCase(firstChar);
    }

    private boolean isNearKeyword(String text, int startPos, int endPos) {
        int checkStart = Math.max(0, startPos - 40);
        int checkEnd = Math.min(text.length(), endPos + 40);
        String contextArea = text.substring(checkStart, checkEnd).toLowerCase();
        
        for (String keyword : CONTEXT_KEYWORDS) {
            Pattern p = Pattern.compile("\\b" + keyword + "\\b");
            if (p.matcher(contextArea).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run English spelling and grammar checks on text using LanguageTool.
     */
    public List<GrammarIssue> checkGrammar(String text) {
        List<GrammarIssue> issues = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return issues;
        }
        try {
            JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
            
            // Disable low-value rule categories
            langTool.disableCategory(new CategoryId("TYPOGRAPHY"));
            langTool.disableCategory(new CategoryId("STYLE"));
            
            List<RuleMatch> matches = langTool.check(text);
            for (RuleMatch match : matches) {
                String ruleId = match.getRule().getId();
                String categoryId = "";
                if (match.getRule().getCategory() != null && match.getRule().getCategory().getId() != null) {
                    categoryId = match.getRule().getCategory().getId().toString();
                }

                // 1. Filter out low-value grammar rule categories and comma placement rules
                if ("TYPOGRAPHY".equals(categoryId) || "STYLE".equals(categoryId) || ruleId.contains("COMMA")) {
                    continue;
                }

                int from = match.getFromPos();
                int to = match.getToPos();
                String matchedWord = text.substring(from, to).trim();

                // 2. Custom spelling heuristics for proper nouns & acronyms
                boolean isSpellingError = "TYPOS".equals(categoryId) || ruleId.startsWith("MORFOLOGIK_");
                if (isSpellingError) {
                    // Check custom ignore list
                    if (IGNORED_WORDS.contains(matchedWord.toUpperCase())) {
                        continue;
                    }
                    // Check if title case/ALL CAPS near institution/location clues
                    if (isTitleCaseOrAllCaps(matchedWord) && isNearKeyword(text, from, to)) {
                        continue;
                    }
                }

                int startCtx = Math.max(0, from - 20);
                int endCtx = Math.min(text.length(), to + 20);
                String context = text.substring(startCtx, endCtx).trim();
                if (startCtx > 0) context = "..." + context;
                if (endCtx < text.length()) context = context + "...";

                issues.add(new GrammarIssue(
                    match.getMessage(),
                    context,
                    match.getSuggestedReplacements()
                ));
            }
        } catch (IOException e) {
            System.err.println("LanguageTool analysis failed: " + e.getMessage());
        }
        return issues;
    }

    public String getLevel(int score) {
        if (score <= 30) return "Beginner";
        else if (score <= 60) return "Intermediate";
        else return "Strong";
    }

    public String getFeedback(int score, String role, String[] missingSkills) {
        if (score == 100 || missingSkills.length == 0) {
            return "Excellent match for " + role + "!";
        }
        if (score <= 30) {
            return "Improve fundamentals: " + String.join(", ", missingSkills);
        } else if (score <= 60) {
            return "Work on: " + String.join(", ", missingSkills);
        } else {
            return "Good, but improve: " + String.join(", ", missingSkills);
        }
    }

    private static final Set<String> WEAK_VERB_PHRASES = new HashSet<>(Arrays.asList(
        "worked on", "helped with", "responsible for", "involved in", "assisted with", "participated in", "handled"
    ));

    public List<String> checkResumeStructure(String text) {
        List<String> suggestions = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return suggestions;
        }

        String uppercaseText = text.toUpperCase();

        // 1. Check sections
        boolean hasContact = uppercaseText.contains("EMAIL") || uppercaseText.contains("PHONE") || 
                             uppercaseText.contains("LINKEDIN") || uppercaseText.contains("CONTACT");
        if (!hasContact) {
            suggestions.add("No dedicated Contact Info section or contact keywords (Email, Phone) detected.");
        }

        boolean hasEducation = uppercaseText.contains("EDUCATION") || uppercaseText.contains("ACADEMIC");
        if (!hasEducation) {
            suggestions.add("Consider adding an Education section to list your academic background.");
        }

        boolean hasSkills = uppercaseText.contains("SKILLS") || uppercaseText.contains("TECHNOLOGIES") || uppercaseText.contains("TECHNICAL SKILLS");
        if (!hasSkills) {
            suggestions.add("No dedicated Skills section detected. Make your competencies easily scanable.");
        }

        boolean hasProjectsExp = uppercaseText.contains("PROJECTS") || uppercaseText.contains("EXPERIENCE") || 
                                 uppercaseText.contains("WORK") || uppercaseText.contains("EMPLOYMENT");
        if (!hasProjectsExp) {
            suggestions.add("Consider adding a Projects or Experience section to showcase your hands-on work.");
        }

        boolean hasCertifications = uppercaseText.contains("CERTIFICATIONS") || uppercaseText.contains("CERTIFICATES") || uppercaseText.contains("CREDENTIALS");
        if (!hasCertifications) {
            suggestions.add("Consider adding a Certifications section to display your credentials.");
        }

        // 2. Length check
        String[] words = text.trim().split("\\s+");
        if (words.length > 1000) {
            suggestions.add("Your resume length suggests more than 2 pages of content (" + words.length + " words). Consider condensing it to 1-2 pages.");
        }

        return suggestions;
    }

    public List<ResumeResponse.BulletFeedbackItem> checkBulletPoints(String text) {
        List<ResumeResponse.BulletFeedbackItem> feedback = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return feedback;
        }

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Check common bullet prefixes
            if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("\u2013") || trimmed.startsWith("\u2014") || trimmed.startsWith("*")) {
                String bulletText = trimmed.substring(1).trim();
                if (bulletText.isEmpty()) {
                    continue;
                }

                List<String> bulletSuggestions = new ArrayList<>();

                // a) Check quantifiable metric
                boolean hasMetric = bulletText.matches(".*\\d+.*") || 
                                     bulletText.toLowerCase().contains("percent") || 
                                     bulletText.toLowerCase().contains("percentage");
                if (!hasMetric) {
                    bulletSuggestions.add("Consider adding measurable impact to this point (e.g. a percentage, number, or scale).");
                }

                // b) Check weak passive verb
                String lowerBullet = bulletText.toLowerCase();
                String foundWeakPhrase = null;
                for (String weakPhrase : WEAK_VERB_PHRASES) {
                    if (lowerBullet.startsWith(weakPhrase)) {
                        foundWeakPhrase = weakPhrase;
                        break;
                    }
                }
                if (foundWeakPhrase != null) {
                    bulletSuggestions.add("Starts with passive phrase '" + foundWeakPhrase + "'. Suggest using a stronger action verb alternative (e.g. 'Developed', 'Led', 'Optimized', 'Implemented', 'Designed').");
                }

                if (!bulletSuggestions.isEmpty()) {
                    feedback.add(new ResumeResponse.BulletFeedbackItem(trimmed, bulletSuggestions));
                }
            }
        }
        return feedback;
    }

    private int findHeaderIndex(String text, String... headers) {
        String lower = text.toLowerCase();
        for (String header : headers) {
            int idx = lower.indexOf(header);
            if (idx != -1) {
                return idx;
            }
        }
        return -1;
    }

    public List<ResumeResponse.PrioritizedSkill> prioritizeMissingSkills(List<String> missingSkills, String jobDescription) {
        List<ResumeResponse.PrioritizedSkill> prioritized = new ArrayList<>();
        if (missingSkills == null || missingSkills.isEmpty()) {
            return prioritized;
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            for (String skill : missingSkills) {
                prioritized.add(new ResumeResponse.PrioritizedSkill(getSkillDisplayName(skill), "Nice to have"));
            }
            return prioritized;
        }

        String lowerJd = jobDescription.toLowerCase();
        int niceToHaveIndex = findHeaderIndex(jobDescription, "nice to have", "nice-to-have", "preferred", "plus", "desired", "beneficial");

        class SkillPriorityInfo {
            String name;
            int score;
            String priorityLabel;
            
            SkillPriorityInfo(String name, int score, String priorityLabel) {
                this.name = name;
                this.score = score;
                this.priorityLabel = priorityLabel;
            }
        }

        List<SkillPriorityInfo> list = new ArrayList<>();

        for (String skill : missingSkills) {
            String parsedSkill = skill.replace("-", "[-\\s_]?").replace("_", "[-\\s_]?");
            Pattern pattern = Pattern.compile("\\b" + parsedSkill + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lowerJd);

            int count = 0;
            int firstIndex = -1;
            while (matcher.find()) {
                if (count == 0) {
                    firstIndex = matcher.start();
                }
                count++;
            }

            int baseScore = 10; // Default: Critical (Requirements section)
            String priorityLabel = "Critical";

            if (niceToHaveIndex != -1 && firstIndex >= niceToHaveIndex) {
                baseScore = 1;
                priorityLabel = "Nice to have";
            }

            // Frequency boost
            int totalScore = baseScore + (count * 2);

            list.add(new SkillPriorityInfo(getSkillDisplayName(skill), totalScore, priorityLabel));
        }

        // Sort by totalScore descending
        list.sort((a, b) -> Integer.compare(b.score, a.score));

        for (SkillPriorityInfo info : list) {
            prioritized.add(new ResumeResponse.PrioritizedSkill(info.name, info.priorityLabel));
        }

        return prioritized;
    }

    /**
     * Main analysis method supporting optional Job Description matching and advanced checks.
     */
    public ResumeResponse analyze(String resumeText, String jobDescription, String selectedRole, List<String> atsWarnings, Long savedId) {
        List<String> resumeSkills = extractSkills(resumeText);
        List<String> resumeSoftSkills = extractSoftSkills(resumeText);
        List<GrammarIssue> grammarIssues = checkGrammar(resumeText);

        List<String> structureSuggestions = checkResumeStructure(resumeText);
        List<ResumeResponse.BulletFeedbackItem> bulletFeedback = checkBulletPoints(resumeText);

        // Check if Job Description matching is selected (JD is provided)
        if (jobDescription != null && !jobDescription.trim().isEmpty()) {
            List<String> jdTechSkills = extractSkills(jobDescription);
            List<String> jdSoftSkills = extractSoftSkills(jobDescription);

            List<String> matchedTechSkills = new ArrayList<>();
            List<String> missingTechSkills = new ArrayList<>();
            for (String jdSkill : jdTechSkills) {
                if (resumeSkills.contains(jdSkill)) {
                    matchedTechSkills.add(jdSkill);
                } else {
                    missingTechSkills.add(jdSkill);
                }
            }

            List<String> matchedSoftSkills = new ArrayList<>();
            List<String> missingSoftSkills = new ArrayList<>();
            for (String jdSoft : jdSoftSkills) {
                if (resumeSoftSkills.contains(jdSoft)) {
                    matchedSoftSkills.add(jdSoft);
                } else {
                    missingSoftSkills.add(jdSoft);
                }
            }

            int totalRequired = jdTechSkills.size() + jdSoftSkills.size();
            int totalMatched = matchedTechSkills.size() + matchedSoftSkills.size();
            int score = totalRequired == 0 ? 0 : (totalMatched * 100) / totalRequired;

            StringBuilder feedbackSb = new StringBuilder();
            if (score == 100) {
                feedbackSb.append("Excellent match! Your resume contains all the skills required by the job description.");
            } else {
                if (score >= 70) {
                    feedbackSb.append("Strong match. You have most of the skills required.");
                } else if (score >= 40) {
                    feedbackSb.append("Moderate match. Focus on developing your missing skills.");
                } else {
                    feedbackSb.append("Weak match. You are missing core requirements.");
                }

                if (!missingTechSkills.isEmpty()) {
                    feedbackSb.append(" Consider learning or brushing up on technical skills: ").append(String.join(", ", formatSkills(missingTechSkills))).append(".");
                }
                if (!missingSoftSkills.isEmpty()) {
                    feedbackSb.append(" For soft skills like ").append(String.join(", ", formatSkills(missingSoftSkills)))
                              .append(", consider adding specific examples in your resume that demonstrate them (e.g. presenting results or teamwork).");
                }
            }
            String feedback = feedbackSb.toString();

            List<String> allMissing = new ArrayList<>();
            allMissing.addAll(missingTechSkills);
            allMissing.addAll(missingSoftSkills);
            List<ResumeResponse.PrioritizedSkill> missingPrioritized = prioritizeMissingSkills(allMissing, jobDescription);

            return new ResumeResponse(
                formatSkills(resumeSkills),
                new ArrayList<>(), // Empty role ranking in JD mode
                feedback,
                true, // jdMatchMode
                formatSkills(jdTechSkills),
                formatSkills(matchedTechSkills),
                formatSkills(missingTechSkills),
                score,
                grammarIssues,
                savedId,
                structureSuggestions,
                bulletFeedback,
                atsWarnings,
                missingPrioritized,
                formatSkills(resumeSoftSkills),
                formatSkills(matchedSoftSkills),
                formatSkills(missingSoftSkills)
            );
        }

        // Fallback: Predefined Role-List Matching
        List<RoleMatchResponse> roleRanking = new ArrayList<>();
        List<Role> matchedRoles = new ArrayList<>();
        int bestScore = -1;
        String bestRoleName = "No strong role match";

        for (RoleMapping mapping : ROLE_MAPPINGS) {
            String roleName = mapping.getRoleName();
            String[] requiredSkills = mapping.getRequiredSkills().toArray(new String[0]);

            if (selectedRole != null && !selectedRole.isEmpty() && !selectedRole.equals("all") && !roleName.equalsIgnoreCase(selectedRole)) {
                continue;
            }

            List<String> matchedTech = new ArrayList<>();
            List<String> missingTech = new ArrayList<>();
            List<String> matchedSoft = new ArrayList<>();
            List<String> missingSoft = new ArrayList<>();

            for (String req : requiredSkills) {
                if (SOFT_SKILLS_KEYS.contains(req)) {
                    if (resumeSoftSkills.contains(req)) {
                        matchedSoft.add(req);
                    } else {
                        missingSoft.add(req);
                    }
                } else {
                    if (resumeSkills.contains(req)) {
                        matchedTech.add(req);
                    } else {
                        missingTech.add(req);
                    }
                }
            }

            int score = requiredSkills.length == 0 ? 0 : ((matchedTech.size() + matchedSoft.size()) * 100) / requiredSkills.length;
            Role role = new Role(roleName, requiredSkills);
            role.score = score;

            List<String> allMissing = new ArrayList<>();
            allMissing.addAll(missingTech);
            allMissing.addAll(missingSoft);
            role.missingSkills = allMissing.toArray(new String[0]);

            role.level = getLevel(score);
            role.feedback = getFeedback(score, roleName, role.missingSkills);

            matchedRoles.add(role);
            roleRanking.add(new RoleMatchResponse(roleName, score));

            if (score > bestScore) {
                bestScore = score;
                bestRoleName = roleName;
            }
        }

        // Sort role rankings by score descending
        roleRanking.sort((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore()));

        String recommendationText = "Best Role: " + bestRoleName + " (" + (bestScore >= 0 ? bestScore + "%" : "0%") + ")";
        if (bestScore < 40) {
            recommendationText = "No strong role match detected (" + (bestScore >= 0 ? bestScore + "%" : "0%") + "). Try adding more domain-specific skills.";
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        List<String> matchedSoftSkills = new ArrayList<>();
        List<String> missingSoftSkills = new ArrayList<>();
        if (!matchedRoles.isEmpty()) {
            matchedRoles.sort((r1, r2) -> Integer.compare(r2.score, r1.score));
            Role bestRole = matchedRoles.get(0);
            for (String req : bestRole.requiredSkills) {
                if (SOFT_SKILLS_KEYS.contains(req)) {
                    if (resumeSoftSkills.contains(req)) {
                        matchedSoftSkills.add(req);
                    } else {
                        missingSoftSkills.add(req);
                    }
                } else {
                    if (resumeSkills.contains(req)) {
                        matchedSkills.add(req);
                    } else {
                        missingSkills.add(req);
                    }
                }
            }
        }

        List<String> allMissing = new ArrayList<>();
        allMissing.addAll(missingSkills);
        allMissing.addAll(missingSoftSkills);
        List<ResumeResponse.PrioritizedSkill> missingPrioritized = prioritizeMissingSkills(allMissing, null);

        return new ResumeResponse(
            formatSkills(resumeSkills),
            roleRanking,
            recommendationText,
            false, // jdMatchMode
            new ArrayList<>(), // jdSkills
            formatSkills(matchedSkills),
            formatSkills(missingSkills),
            bestScore >= 0 ? bestScore : 0,
            grammarIssues,
            savedId,
            structureSuggestions,
            bulletFeedback,
            atsWarnings,
            missingPrioritized,
            formatSkills(resumeSoftSkills),
            formatSkills(matchedSoftSkills),
            formatSkills(missingSoftSkills)
        );
    }

    public ResumeResponse analyze(String resumeText, String jobDescription, String selectedRole, Long savedId) {
        return analyze(resumeText, jobDescription, selectedRole, null, savedId);
    }

    public ResumeResponse analyze(Resume resume, String selectedRole) {
        return analyze(resume.text, null, selectedRole, null, null);
    }
}