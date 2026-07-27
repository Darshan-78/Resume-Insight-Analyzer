package com.example.resumeinsight;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CrossOrigin(origins = "*")
@RestController
public class ResumeController {

    @Autowired
    private AnalysisHistoryRepository repository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final Map<String, String> RESTORE_TERMS = new java.util.LinkedHashMap<>();
    static {
        RESTORE_TERMS.put("(?i)\\bJava Script\\b", "JavaScript");
        RESTORE_TERMS.put("(?i)\\bType Script\\b", "TypeScript");
        RESTORE_TERMS.put("(?i)\\bGit Hub\\b", "GitHub");
        RESTORE_TERMS.put("(?i)\\bPostgre Sql\\b", "PostgreSQL");
        RESTORE_TERMS.put("(?i)\\bMongo Db\\b", "MongoDB");
        RESTORE_TERMS.put("(?i)\\bMy Sql\\b", "MySQL");
        RESTORE_TERMS.put("(?i)\\bNode Js\\b", "Node.js");
        RESTORE_TERMS.put("(?i)\\bLinked In\\b", "LinkedIn");
        RESTORE_TERMS.put("(?i)\\bGit Lab\\b", "GitLab");
        RESTORE_TERMS.put("(?i)\\bIntelli J\\b", "IntelliJ");
        RESTORE_TERMS.put("(?i)\\bDev Ops\\b", "DevOps");
        RESTORE_TERMS.put("(?i)\\bGraph Ql\\b", "GraphQL");
        RESTORE_TERMS.put("(?i)\\bRest Ful\\b", "RESTful");
        RESTORE_TERMS.put("(?i)\\bO Auth\\b", "OAuth");
        RESTORE_TERMS.put("(?i)\\bWeb Socket\\b", "WebSocket");
        RESTORE_TERMS.put("(?i)\\bKubernet\\b", "Kubernetes");
        RESTORE_TERMS.put("(?i)\\bTensor Flow\\b", "TensorFlow");
        RESTORE_TERMS.put("(?i)\\bPy Torch\\b", "PyTorch");
    }

    private static final Set<String> REJOIN_WITHOUT_HYPHEN = new HashSet<>(Arrays.asList(
        "program", "programmer", "programmers", "programming",
        "completed", "completing", "complete",
        "effective", "effectively",
        "collaborative", "compatibility", "predefined", "till",
        "normalization", "identify", "integration", "development",
        "coordinate", "coordination", "cooperative", "automated",
        "automating", "automation"
    ));

    private String repairHyphenatedLineWraps(String text) {
        if (text == null) {
            return "";
        }
        // Match word1 + hyphen/dash (including soft hyphen and unicode hyphens) + line break + word2
        Pattern p = Pattern.compile("(\\b\\w+)[-\\u00ad\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015]\\s*\\r?\\n\\s*([a-zA-Z]+)\\b");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String word1 = m.group(1);
            String word2 = m.group(2);
            String joined = (word1 + word2).toLowerCase();
            if (REJOIN_WITHOUT_HYPHEN.contains(joined)) {
                m.appendReplacement(sb, word1 + word2);
            } else {
                m.appendReplacement(sb, word1 + "-" + word2);
            }
        }
        m.appendTail(sb);

        // Repeat for raw \n linebreaks
        p = Pattern.compile("(\\b\\w+)[-\\u00ad\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015]\\s*\\n\\s*([a-zA-Z]+)\\b");
        m = p.matcher(sb.toString());
        sb = new StringBuffer();
        while (m.find()) {
            String word1 = m.group(1);
            String word2 = m.group(2);
            String joined = (word1 + word2).toLowerCase();
            if (REJOIN_WITHOUT_HYPHEN.contains(joined)) {
                m.appendReplacement(sb, word1 + word2);
            } else {
                m.appendReplacement(sb, word1 + "-" + word2);
            }
        }
        m.appendTail(sb);

        return sb.toString();
    }

    private String postProcessExtractedText(String text) {
        if (text == null) {
            return "";
        }
        // 1. Join non-hyphenated word-wrapped lines with a space (e.g. "problem\r\nsolving" -> "problem solving")
        text = text.replaceAll("(\\b\\w+)\\s*\\r?\\n\\s*([a-z]+)\\b", "$1 $2");
        text = text.replaceAll("(\\b\\w+)\\s*\\n\\s*([a-z]+)\\b", "$1 $2");

        // 2. Ensure bullet/dash markers (\u2013, \u2014, \u2022) separated by line breaks have a space inserted before the break
        text = text.replaceAll("(\\w)\\s*\\r?\\n\\s*([\\u2013\\u2014\\u2022])", "$1 \n$2");
        text = text.replaceAll("(\\w)\\s*\\n\\s*([\\u2013\\u2014\\u2022])", "$1 \n$2");

        // 3. Ensure standard hyphens acting as list separators have a space and newline
        text = text.replaceAll("(\\w)\\s*\\r?\\n\\s*(-)\\s*([A-Z])", "$1 \n$2 $3");
        text = text.replaceAll("(\\w)\\s*\\n\\s*(-)\\s*([A-Z])", "$1 \n$2 $3");

        // 4. Line-wrap hyphenation repair: only strip hyphen for single words, keep for compound words
        text = repairHyphenatedLineWraps(text);

        // 5. Split merged words at lowercase-to-uppercase transitions (e.g. "DeloitteHacksplosion" -> "Deloitte Hacksplosion")
        text = text.replaceAll("([a-z])([A-Z])", "$1 $2");

        // 6. Restore camelcase tech terms that shouldn't be split (using RESTORE_TERMS map)
        for (Map.Entry<String, String> entry : RESTORE_TERMS.entrySet()) {
            text = text.replaceAll(entry.getKey(), entry.getValue());
        }

        // 7. Space formatting before bullets/dashes using Unicode escapes to prevent encoding issues
        text = text.replaceAll("(\\w)([\\u2013\\u2014\\u2022])", "$1 $2");
        text = text.replaceAll("(\\w)-(\\s+[A-Z])", "$1 - $2");

        // 8. Fallback replacement for problemsolving (just in case the hyphen was completely omitted/dropped)
        text = text.replaceAll("(?i)\\bproblemsolving\\b", "problem-solving");

        return text;
    }

    /**
     * Parse uploaded PDF/DOCX/DOC file and return its textual content.
     */
    /**
     * Parse uploaded PDF/DOCX/DOC file and return its textual content and ATS diagnostics.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("error", "The uploaded file is empty.");
            return ResponseEntity.badRequest().body(result);
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String text = "";
        List<String> atsWarnings = new ArrayList<>();

        try {
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf") 
                || "application/pdf".equals(contentType)) {
                // PDF Parsing using standard PDFTextStripper with coordinate sorting to fix ligatures
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    // Check for embedded images
                    boolean hasImages = false;
                    for (PDPage page : document.getPages()) {
                        PDResources resources = page.getResources();
                        if (resources != null) {
                            for (COSName name : resources.getXObjectNames()) {
                                try {
                                    if (resources.isImageXObject(name)) {
                                        hasImages = true;
                                        break;
                                    }
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        }
                        if (hasImages) break;
                    }
                    if (hasImages) {
                        atsWarnings.add("Embedded images/graphics detected (e.g. photos, diagrams), which some ATS systems have trouble reading.");
                    }

                    // Extract text & detect columns
                    AtsStripper stripper = new AtsStripper();
                    stripper.setSortByPosition(true);
                    stripper.setSpacingTolerance(0.3f);
                    text = stripper.getText(document);

                    if (stripper.multiColumnDetected) {
                        atsWarnings.add("Potential multi-column layout detected, which can confuse ATS parser reading order.");
                    }
                }
            } else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".docx")
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
                // DOCX Parsing: Manually iterate over body elements to enforce paragraph and cell boundaries
                try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
                    StringBuilder sb = new StringBuilder();
                    for (org.apache.poi.xwpf.usermodel.IBodyElement element : docx.getBodyElements()) {
                        if (element instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph) {
                            String pText = ((org.apache.poi.xwpf.usermodel.XWPFParagraph) element).getText();
                            if (pText != null) {
                                sb.append(pText).append("\n");
                            }
                        } else if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable) {
                            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : ((org.apache.poi.xwpf.usermodel.XWPFTable) element).getRows()) {
                                for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                                    String cText = cell.getText();
                                    if (cText != null) {
                                        sb.append(cText).append(" ");
                                    }
                                }
                                sb.append("\n");
                            }
                        }
                    }
                    text = sb.toString();
                }
                atsWarnings.add("N/A for Word documents");
            } else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".doc")
                || "application/msword".equals(contentType)) {
                // DOC Parsing: Enforce boundaries by iterating over paragraphs
                try (HWPFDocument doc = new HWPFDocument(file.getInputStream())) {
                    StringBuilder sb = new StringBuilder();
                    org.apache.poi.hwpf.usermodel.Range range = doc.getRange();
                    for (int i = 0; i < range.numParagraphs(); i++) {
                        org.apache.poi.hwpf.usermodel.Paragraph paragraph = range.getParagraph(i);
                        String pText = paragraph.text();
                        if (pText != null) {
                            sb.append(pText);
                            if (!pText.endsWith("\n") && !pText.endsWith("\r")) {
                                sb.append("\n");
                            }
                        }
                    }
                    text = sb.toString();
                }
                atsWarnings.add("N/A for Word documents");
            } else {
                result.put("error", "Unsupported file type. Please upload a PDF, DOCX, or DOC file.");
                return ResponseEntity.badRequest().body(result);
            }



            result.put("text", postProcessExtractedText(text));
            result.put("filename", originalFilename);
            result.put("atsWarnings", atsWarnings);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", "Failed to extract text from file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Expose list of dynamic tech roles to the frontend.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getRoles() {
        List<String> roles = new ArrayList<>();
        for (ResumeAnalyzerService.RoleMapping mapping : ResumeAnalyzerService.ROLE_MAPPINGS) {
            roles.add(mapping.getRoleName());
        }
        return ResponseEntity.ok(roles);
    }

    /**
     * Analyzes resume text against either a target role or a pasted Job Description,
     * stores the result in the database, and returns the analysis.
     */
    // Keep this signature to avoid breaking compile-time checks in test classes
    public ResumeResponse analyze(ResumeRequest request) {
        return analyze(request, null, null);
    }

    @PostMapping("/analyze")
    public ResumeResponse analyze(
            @RequestBody ResumeRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        ResumeAnalyzerService analyzer = new ResumeAnalyzerService();

        // Resolve Session ID
        String sessionId = resolveSessionId(headerSessionId, cookieHeader);

        // Check if ATS warnings are provided
        List<String> atsWarnings = request.getAtsWarnings();
        if (atsWarnings == null) {
            atsWarnings = new ArrayList<>();
            String filename = request.getFilename() != null ? request.getFilename() : "Pasted Resume Text";
            if (filename.toLowerCase().endsWith(".docx") || filename.toLowerCase().endsWith(".doc")) {
                atsWarnings.add("N/A for Word documents");
            } else {
                atsWarnings.add("N/A for Pasted Text");
            }
        }

        // 1. Calculate findings
        String filename = request.getFilename() != null ? request.getFilename() : "Pasted Resume Text";
        ResumeResponse response = analyzer.analyze(request.getText(), request.getJobDescription(), request.getSelectedRole(), atsWarnings, null);

        // 2. Persist findings to DB
        AnalysisHistory history = new AnalysisHistory();
        history.setFilename(filename);
        history.setResumeText(request.getText());
        history.setJobDescription(request.getJobDescription());
        history.setScore(response.getScore());
        history.setMatchedSkills(String.join(",", response.getMatchedSkills()));
        history.setMissingSkills(String.join(",", response.getMissingSkills()));
        history.setMatchedSoftSkills(response.getMatchedSoftSkills() != null ? String.join(",", response.getMatchedSoftSkills()) : "");
        history.setMissingSoftSkills(response.getMissingSoftSkills() != null ? String.join(",", response.getMissingSoftSkills()) : "");
        history.setAtsWarnings(response.getAtsWarnings() != null ? String.join(";", response.getAtsWarnings()) : "");
        history.setBestRole(response.getBestRoleRecommendation());
        history.setTimestamp(LocalDateTime.now());
        history.setSessionId(sessionId);

        try {
            ObjectMapper mapper = new ObjectMapper();
            history.setGrammarIssuesJson(mapper.writeValueAsString(response.getGrammarIssues()));
        } catch (Exception e) {
            history.setGrammarIssuesJson("[]");
        }

        AnalysisHistory saved = repository.save(history);

        // 3. Return full response with the database key
        return new ResumeResponse(
            response.getDetectedSkills(),
            response.getRoleRanking(),
            response.getBestRoleRecommendation(),
            response.isJdMatchMode(),
            response.getJdSkills(),
            response.getMatchedSkills(),
            response.getMissingSkills(),
            response.getScore(),
            response.getGrammarIssues(),
            saved.getId(),
            response.getStructureSuggestions(),
            response.getBulletFeedback(),
            response.getAtsWarnings(),
            response.getMissingSkillsPrioritized(),
            response.getDetectedSoftSkills(),
            response.getMatchedSoftSkills(),
            response.getMissingSoftSkills()
        );
    }

    /**
     * Opt-in AI-powered roadmap generator. Calls Google Gemini API,
     * updates the database history entity, and returns the roadmap markdown.
     */
    /**
     * Opt-in AI-powered roadmap generator. Calls Google Gemini API,
     * updates the database history entity, and returns the roadmap markdown.
     */
    @PostMapping("/analyze/{id}/roadmap")
    public ResponseEntity<Map<String, String>> generateRoadmap(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        Optional<AnalysisHistory> optionalHistory = repository.findById(id);
        if (optionalHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AnalysisHistory history = optionalHistory.get();
        String sessionId = resolveSessionId(headerSessionId, cookieHeader);
        String recordSessionId = history.getSessionId() != null ? history.getSessionId() : "anonymous";
        if (!recordSessionId.equals(sessionId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("your_gemini_api_key")) {
            return ResponseEntity.ok(Map.of("roadmap", "Gemini API key is not configured. Please add your GEMINI_API_KEY in application-local.properties."));
        }

        // Construct prompt using missing skills and score
        List<String> combinedMissing = new ArrayList<>();
        if (history.getMissingSkills() != null && !history.getMissingSkills().isEmpty()) {
            combinedMissing.addAll(Arrays.asList(history.getMissingSkills().split(",")));
        }
        if (history.getMissingSoftSkills() != null && !history.getMissingSoftSkills().isEmpty()) {
            combinedMissing.addAll(Arrays.asList(history.getMissingSoftSkills().split(",")));
        }
        String skillsList = combinedMissing.isEmpty() ? "None" : String.join(", ", combinedMissing);
        
        String prompt = "You are an expert career coach and tech lead. A candidate scored " + history.getScore() + 
            "% match for their target role/job description. They are missing these skills: [" + skillsList + "]. " +
            "Create a highly practical, structured 3-month weekly learning roadmap to acquire these missing skills. " +
            "Organize the timeline clearly (Month 1, Month 2, Month 3, and weeks). Include resource suggestions and 1 project suggestion. " +
            "If there are missing soft skills (like Communication, Teamwork, Leadership, etc.), do not treat them as subjects to learn via courses. Instead, suggest specific examples, experiences, or collaborative project scenarios the candidate can add to their resume to demonstrate these skills (e.g. a project where they presented results or coordinated cross-functional collaboration). " +
            "Keep the tone encouraging, technical, and direct. Do not include boilerplate introductory greeting text.";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + geminiApiKey;
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            )
        );

        try {
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);
            if (response != null && response.containsKey("candidates")) {
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                        String roadmap = (String) firstPart.get("text");

                        history.setRoadmap(roadmap);
                        repository.save(history);

                        return ResponseEntity.ok(Map.of("roadmap", roadmap));
                    }
                }
            }
            return ResponseEntity.ok(Map.of("roadmap", "Error: Empty or malformed response returned from the Gemini API."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("roadmap", "Failed to generate roadmap: " + e.getMessage()));
        }
    }

    /**
     * Download the analysis report as a formatted PDF.
     */
    @GetMapping("/analyze/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdfReport(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        Optional<AnalysisHistory> optionalHistory = repository.findById(id);
        if (optionalHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AnalysisHistory history = optionalHistory.get();
        String sessionId = resolveSessionId(headerSessionId, cookieHeader);
        String recordSessionId = history.getSessionId() != null ? history.getSessionId() : "anonymous";
        if (!recordSessionId.equals(sessionId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        try {
            PDFGeneratorUtil pdfUtil = new PDFGeneratorUtil();
            byte[] pdfBytes = pdfUtil.generateReport(history);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resume-insight-report-" + id + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String resolveSessionId(String headerSessionId, String cookieHeader) {
        if (headerSessionId != null && !headerSessionId.trim().isEmpty()) {
            return headerSessionId.trim();
        }
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "resume_session_id".equals(parts[0].trim())) {
                    return parts[1].trim();
                }
            }
        }
        return "anonymous";
    }

    /**
     * Retrieve past analysis logs (history list) matching active session.
     */
    @GetMapping("/history")
    public List<AnalysisHistory> getHistory(
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        String sessionId = resolveSessionId(headerSessionId, cookieHeader);
        return repository.findAllBySessionIdOrderByTimestampDesc(sessionId);
    }

    /**
     * Delete all past analysis logs matching active session.
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory(
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        String sessionId = resolveSessionId(headerSessionId, cookieHeader);
        repository.deleteAllBySessionId(sessionId);
        return ResponseEntity.ok(Map.of("message", "History cleared successfully."));
    }
    public static class AtsStripper extends PDFTextStripper {
        public boolean multiColumnDetected = false;

        public AtsStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            super.writeString(text, textPositions);
            if (textPositions.size() > 1) {
                for (int i = 1; i < textPositions.size(); i++) {
                    TextPosition p1 = textPositions.get(i - 1);
                    TextPosition p2 = textPositions.get(i);
                    // Check if they are on the same line (approx Y overlap)
                    if (Math.abs(p1.getYDirAdj() - p2.getYDirAdj()) < 2) {
                        float gap = p2.getXDirAdj() - (p1.getXDirAdj() + p1.getWidthDirAdj());
                        // If there is a huge horizontal gap on the same line (e.g., > 120 points)
                        if (gap > 120) {
                            multiColumnDetected = true;
                        }
                    }
                }
            }
        }
    }
}