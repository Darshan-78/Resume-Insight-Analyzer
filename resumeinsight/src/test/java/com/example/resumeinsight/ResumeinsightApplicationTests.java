package com.example.resumeinsight;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class ResumeinsightApplicationTests {

    @Autowired
    private ResumeController controller;

    @Autowired
    private AnalysisHistoryRepository repository;

    private static final float PAGE_WIDTH = 612;
    private static final float PAGE_HEIGHT = 792;
    private static final float MARGIN = 50;
    private static final float WRAP_WIDTH = PAGE_WIDTH - (2 * MARGIN);

    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream contentStream;
    private float yPosition;

    @Test
    void contextLoads() {
    }

    private byte[] createMockPdf(String content) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createMockDocx(String content) throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            String[] paragraphs = content.split("\n");
            for (String para : paragraphs) {
                org.apache.poi.xwpf.usermodel.XWPFParagraph p = doc.createParagraph();
                org.apache.poi.xwpf.usermodel.XWPFRun r = p.createRun();
                r.setText(para);
            }
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testExactRealWorldBulletSpacing() throws Exception {
        // Mock the exact raw text sequence: "problem\r\nsolving\r\n– Prepared"
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("time-based problem");
                stream.endText();
            }
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 680);
                stream.showText("solving");
                stream.endText();
            }
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 660);
                stream.showText("\u2013 Prepared a Business report.");
                stream.endText();
            }
            doc.save(baos);
            
            MockMultipartFile file = new MockMultipartFile("file", "resume_realworld.pdf", "application/pdf", baos.toByteArray());
            ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            String text = (String) response.getBody().get("text");

            System.out.println("=== REAL WORLD SPACED BULLET TEST ===");
            System.out.println("Result:\n" + text);
            System.out.println("=====================================");

            // Assert that non-hyphenated line-wrap was joined with a space: "problem solving"
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("problem solving"),
                "Plain line-wrapped word was not joined with a space!");

            // Assert that bullet/dash has a newline/space in front of it: "problem solving \n– Prepared"
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("solving \n\u2013 Prepared") 
                || text.contains("solving \r\n\u2013 Prepared"),
                "Space was not placed before the bullet / newline!");
        }
    }

    @Test
    void testPdfLigatureExtraction() throws IOException {
        String testText = "completing effectively collaborative compatibility predefined till normalization identify integration";
        byte[] pdfBytes = createMockPdf(testText);
        
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String extracted = stripper.getText(document).trim();
            
            System.out.println("=== PDF LIGATURE EXTRACTION TEST ===");
            System.out.println("Original:  " + testText);
            System.out.println("Extracted: " + extracted);
            System.out.println("=====================================");

            String[] expectedWords = testText.split(" ");
            for (String word : expectedWords) {
                org.junit.jupiter.api.Assertions.assertTrue(extracted.contains(word),
                    "Ligature failed extraction! Word lost letters: " + word);
            }
        }
    }

    @Test
    void testUploadResumePdf() throws Exception {
        String resumeText = "Java Developer with Spring Boot experience.";
        byte[] pdfBytes = createMockPdf(resumeText);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes);
        
        ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertTrue(response.getBody().get("text").toString().contains("Java Developer"));
        org.junit.jupiter.api.Assertions.assertEquals("resume.pdf", response.getBody().get("filename").toString());
    }

    @Test
    void testUploadResumeDocx() throws Exception {
        String resumeText = "Python Developer with machine learning expertise.";
        byte[] docxBytes = createMockDocx(resumeText);
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);
        
        ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertTrue(response.getBody().get("text").toString().contains("Python Developer"));
        org.junit.jupiter.api.Assertions.assertEquals("resume.docx", response.getBody().get("filename").toString());
    }

    @Test
    void testPdfSpacingAndHyphenation() throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("DeloitteHacksplosion is an event for developers.");
                stream.endText();
            }
            
            // Write second line
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 680);
                stream.showText("We are Pro-");
                stream.endText();
            }

            // Write third line
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 660);
                stream.showText("grammers who build software in JavaScript.");
                stream.endText();
            }

            // Write fourth line (for compound word wrapping)
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 640);
                stream.showText("They excel at problem-");
                stream.endText();
            }

            // Write fifth line (with bullet marker)
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 620);
                stream.showText("solving– Prepared a Business report.");
                stream.endText();
            }
            
            doc.save(baos);
            
            // Upload the generated PDF
            MockMultipartFile file = new MockMultipartFile("file", "resume_spacing_test.pdf", "application/pdf", baos.toByteArray());
            ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
            
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            String extractedText = (String) response.getBody().get("text");
            
            System.out.println("=== PDF SPACING AND HYPHENATION TEST ===");
            System.out.println("Extracted & Processed Text:\n" + extractedText);
            System.out.println("=========================================");
            
            org.junit.jupiter.api.Assertions.assertTrue(extractedText.contains("Deloitte Hacksplosion"), 
                "DeloitteHacksplosion was not split correctly!");
            
            org.junit.jupiter.api.Assertions.assertTrue(extractedText.contains("Programmers"), 
                "Hyphenated line-wrap Pro-grammers was not rejoined correctly!");
            
            org.junit.jupiter.api.Assertions.assertTrue(extractedText.contains("JavaScript"), 
                "JavaScript was incorrectly split or not restored!");

            org.junit.jupiter.api.Assertions.assertTrue(extractedText.contains("problem-solving"),
                "problem-solving hyphen was not preserved!");

            org.junit.jupiter.api.Assertions.assertTrue(extractedText.contains("problem-solving – Prepared"),
                "Space was not added before the bullet marker!");
        }
    }

    @Test
    void testDocxSpacingAndHyphenation() throws Exception {
        // Multi-paragraph input
        String mockDocxContent = "problem solving\nPrepared to learn Spring Boot.";
        byte[] docxBytes = createMockDocx(mockDocxContent);
        MockMultipartFile file = new MockMultipartFile("file", "resume_spacing.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);
        
        ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        String text = (String) response.getBody().get("text");
        
        System.out.println("=== DOCX SPACING TEST ===");
        System.out.println("Extracted & Processed:\n" + text);
        System.out.println("=========================");

        // Assert that paragraph break was preserved with a newline: "solving\nPrepared"
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("solving\nPrepared") || text.contains("solving\r\nPrepared"),
            "DOCX paragraph boundaries were merged!");
        
        // Assert that "Spring Boot" is preserved (not converted to "SpringBoot")
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("Spring Boot"),
            "Spring Boot was incorrectly mutated!");

        // Verify skill matching of Spring Boot and Java displays with proper casings
        ResumeRequest req = new ResumeRequest();
        req.setText("Java Developer with Spring Boot experience.");
        req.setSelectedRole("Java Developer");
        ResumeResponse res = controller.analyze(req);
        
        System.out.println("=== MATCHED SKILLS CASING TEST ===");
        System.out.println("Detected Skills: " + res.getDetectedSkills());
        System.out.println("Matched Skills:  " + res.getMatchedSkills());
        System.out.println("===================================");
        
        org.junit.jupiter.api.Assertions.assertTrue(res.getMatchedSkills().contains("Spring Boot"),
            "Spring Boot matched skill casing is incorrect!");
        org.junit.jupiter.api.Assertions.assertTrue(res.getMatchedSkills().contains("Java"),
            "Java matched skill casing is incorrect!");
    }

    @Test
    void testRealWorldSpacingEdgeCase() throws Exception {
        // Snippet 1: Hyphen captured in PDF layout wrap, followed by en-dash bullet with no space
        // "problem-\r\nsolving– Prepared" -> should post-process to "problem-solving – Prepared"
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("They excel at problem-");
                stream.endText();
            }
            try (PDPageContentStream stream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 680);
                stream.showText("solving\u2013 Prepared a Business report.");
                stream.endText();
            }
            doc.save(baos);
            
            MockMultipartFile file = new MockMultipartFile("file", "resume_edge_case1.pdf", "application/pdf", baos.toByteArray());
            ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            String text = (String) response.getBody().get("text");

            System.out.println("=== REAL WORLD SPACING EDGE CASE 1 (PDF WRAP) ===");
            System.out.println("Result:\n" + text);
            System.out.println("=================================================");

            org.junit.jupiter.api.Assertions.assertTrue(text.contains("problem-solving – Prepared"),
                "problem-solving or en-dash spacing failed to process in PDF wraps!");
        }

        // Snippet 2: Raw merged text fallback test (problemsolving with en-dash bullet and no space)
        // "time-based problemsolving– Prepared" -> should post-process to "time-based problem-solving – Prepared"
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("time-based problemsolving\u2013 Prepared a Business report.");
                stream.endText();
            }
            doc.save(baos);
            
            MockMultipartFile file = new MockMultipartFile("file", "resume_edge_case2.pdf", "application/pdf", baos.toByteArray());
            ResponseEntity<Map<String, Object>> response = controller.uploadResume(file);
            org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
            String text = (String) response.getBody().get("text");

            System.out.println("=== REAL WORLD SPACING EDGE CASE 2 (FALLBACK MERGE) ===");
            System.out.println("Result:\n" + text);
            System.out.println("=====================================================");

            org.junit.jupiter.api.Assertions.assertTrue(text.contains("problem-solving – Prepared"),
                "Fallback word replacement or en-dash spacing failed!");
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("time-based"),
                "Legitimate single-line compound word lost its hyphen!");
        }
    }

    private List<GrammarIssue> checkGrammarLegacy(String text) {
        List<GrammarIssue> issues = new ArrayList<>();
        try {
            org.languagetool.JLanguageTool langTool = new org.languagetool.JLanguageTool(new org.languagetool.language.AmericanEnglish());
            List<org.languagetool.rules.RuleMatch> matches = langTool.check(text);
            for (org.languagetool.rules.RuleMatch match : matches) {
                int from = match.getFromPos();
                int to = match.getToPos();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return issues;
    }

    @Test
    void testGrammarCheckComparison() {
        String testText = "EngineeringJECRC University is located in Sagwara. " +
            "I passed my RBSE examinations. " +
            "I won Hacksplosion and CodeVita. " +
            "My CGPA is 8.5. " +
            "I has a degree in software engineering.";

        System.out.println("=== RUNNING GRAMMAR/SPELLING CHECK COMPARISON ===");
        
        // 1. Run Legacy Grammar Check (Before fixes)
        List<GrammarIssue> legacyIssues = checkGrammarLegacy(testText);
        System.out.println("BEFORE FIXES (Legacy Count: " + legacyIssues.size() + "):");
        for (GrammarIssue issue : legacyIssues) {
            System.out.println(" - Flagged: " + issue.getContext() + " -> " + issue.getMessage());
        }

        // 2. Run Enhanced Grammar Check (After fixes)
        ResumeAnalyzerService service = new ResumeAnalyzerService();
        List<GrammarIssue> enhancedIssues = service.checkGrammar(testText);
        System.out.println("\nAFTER FIXES (Enhanced Count: " + enhancedIssues.size() + "):");
        for (GrammarIssue issue : enhancedIssues) {
            System.out.println(" - Flagged: " + issue.getContext() + " -> " + issue.getMessage());
        }

        System.out.println("=================================================");

        // Verify the behavior:
        // Proper nouns and spacing merged issues should not be flagged.
        // The real error "I has" should still be flagged.
        boolean foundRealError = false;
        boolean foundFalsePositive = false;
        
        for (GrammarIssue issue : enhancedIssues) {
            if (issue.getContext().contains("I has")) {
                foundRealError = true;
            }
            if (issue.getMessage().toLowerCase().contains("spelling")) {
                if (issue.getContext().contains("Sagwara") || 
                    issue.getContext().contains("JECRC") || 
                    issue.getContext().contains("RBSE") || 
                    issue.getContext().contains("Hacksplosion") || 
                    issue.getContext().contains("CodeVita") || 
                    issue.getContext().contains("CGPA") || 
                    issue.getContext().contains("EngineeringJECRC")) {
                    foundFalsePositive = true;
                }
            }
        }

        org.junit.jupiter.api.Assertions.assertTrue(foundRealError, "The real grammar error 'I has' was not flagged!");
        org.junit.jupiter.api.Assertions.assertFalse(foundFalsePositive, "Proper nouns/acronyms/spacing errors are still being flagged as false positives!");
    }

    @Test
    void generateChangesSummaryPdf() throws IOException {
        document = new PDDocument();
        addNewPage();

        // Title
        writeTitle("Resume Insight Analyzer - Upgrade Summary");
        writeSubtitle("Technical Documentation of the 10 Core Application Enhancements");
        drawSeparator();

        // Change 1
        writeChangeSection("Change 1: Job Description Input & Core Matching",
            "Added a Job Description input text area alongside the resume text area, extracting required skills from both texts and calculating exact matched and missing skill sets.",
            "ResumeRequest.java, ResumeResponse.java, ResumeAnalyzerService.java, index.html, script.js",
            "Allows users to analyze their resumes against specific target job descriptions instead of generic roles, offering a personalized match score.",
            "I extended the matching engine to extract and compare skills from user-provided job descriptions, offering tailored skill alignment reports alongside general role suggestions.");

        // Change 2
        writeChangeSection("Change 2: PDF Resume Upload via Apache PDFBox",
            "Integrated Apache PDFBox PDFTextStripper to parse uploaded PDF resumes, extract the raw text content, and feed it directly into the analysis text area.",
            "pom.xml, ResumeController.java, index.html, script.js",
            "Pasting resumes manually is a tedious user experience. Allowing file uploads increases usability and modernizes the application workflow.",
            "I integrated Apache PDFBox to enable direct PDF uploads, extracting text programmatically on the backend and populating the analyzer.");

        // Change 3
        writeChangeSection("Change 3: Regex Word-Boundary Skill Matching & Synonyms",
            "Refactored keyword detection using regex boundaries (\\b) to avoid token split errors on hyphenated skills (like scikit-learn) and added synonym mapping (e.g., JS to JavaScript).",
            "ResumeAnalyzerService.java",
            "Fixed a critical bug where compound skills split on non-letters could never match, and normalized common variations of technology terms.",
            "I optimized the matching engine using regex word boundaries to accurately capture compound skills like scikit-learn and resolved synonyms using a mapping dictionary.");

        // Change 4
        writeChangeSection("Change 4: Spelling & Grammar Check via LanguageTool",
            "Embedded the JLanguageTool library to run spelling and grammar analysis on the resume text, returning a structured list of warnings and correction recommendations.",
            "pom.xml, GrammarIssue.java, ResumeAnalyzerService.java, index.html, script.js",
            "Typos on resumes lead to automatic candidate rejection; adding automated grammar scanning helps users find mistakes before submitting.",
            "I embedded the open-source LanguageTool library to automatically flag spelling and grammar mistakes in resumes, complete with correction suggestions.");

        // Change 5
        writeChangeSection("Change 5: JPA Persistence & PostgreSQL Logging",
            "Configured Spring Data JPA with PostgreSQL to save the results of every analysis (filename, text, score, matched/missing skills, grammar issues) and built a historical logs feed.",
            "pom.xml, AnalysisHistory.java, AnalysisHistoryRepository.java, ResumeController.java, index.html, script.js",
            "Saves past analysis runs so that users can view their application logs, reload previous configurations, and review roadmaps.",
            "I added persistence using Spring Data JPA and PostgreSQL to record historical analyses, allowing users to reload previous configurations.");

        addNewPage(); // start page 2 for remaining changes

        // Change 6
        writeChangeSection("Change 6: Opt-in AI Learning Roadmap (Gemini)",
            "Configured RestTemplate to call the Google Gemini API (gemini-3.1-flash-lite) using the missing skills as a prompt, generating an opt-in 3-month timeline study plan.",
            "ResumeController.java, index.html, script.js",
            "Helps candidates bridge their technical skills gaps by suggesting structured resources, timelines, and project topics.",
            "I implemented an opt-in AI roadmap feature using Google's Gemini API that generates a structured 3-month study timeline to bridge detected skill gaps.");

        // Change 7
        writeChangeSection("Change 7: Multi-page PDF Report Generation",
            "Developed custom PDF export code using PDFBox to write wrapped, multi-page reports containing match score, skills clouds, grammar alerts, and the AI roadmap.",
            "PDFGeneratorUtil.java, ResumeController.java, index.html, script.js",
            "Enables users to download, store, and print high-quality analysis reports for offline reading and presentation.",
            "I authored a PDF generation utility using PDFBox that exports a well-formatted multi-page report containing the analysis dashboard results.");

        // Change 8
        writeChangeSection("Change 8: Chart.js Dashboard Visualization",
            "Integrated Chart.js CDN on the frontend, rendering a horizontal bar chart that visually represents the match score index.",
            "index.html, script.js",
            "Replaces numeric strings with interactive visual representations, making match scores instantly readable and professional.",
            "I integrated Chart.js to present resume-matching indices as interactive horizontal bar charts, enhancing visual data analysis.");

        // Change 9
        writeChangeSection("Change 9: Cloud-Ready Env Configurations",
            "Bound database credentials to environment variables, added H2 configurations for isolated maven tests, and programmatically parsed Render's DATABASE_URL string.",
            "application.properties, DatabaseConfig.java, pom.xml",
            "Decoupled credentials from source code for safety and resolved PostgreSQL connection failures during test runs and Render cloud builds.",
            "I modernized the application's configuration by binding credentials to env vars and adding a parser to cleanly connect to Render's native PostgreSQL strings.");

        // Change 10
        writeChangeSection("Change 10: Tailored Design & Space Scale",
            "Redesigned frontend styling with Space Grotesk (headers), Inter (body), a teal/amber (#0D7377 / #E8A33D) palette, 8px spacing blocks, and sharp borders.",
            "index.html, style.css, script.js",
            "Transforms the basic static look into a high-end dashboard interface, establishing visual hierarchy and scannability.",
            "I redesigned the frontend using Space Grotesk typography, a teal/amber palette, and an 8px spacing system, achieving a premium responsive dashboard.");

        if (contentStream != null) {
            contentStream.close();
        }

        // Save PDF to workspace root folder
        File targetFile = new File("../changes-summary.pdf");
        document.save(targetFile);
        document.close();
        System.out.println("Changes summary PDF written successfully to: " + targetFile.getCanonicalPath());
    }

    private void addNewPage() throws IOException {
        if (contentStream != null) {
            contentStream.close();
        }
        currentPage = new PDPage();
        document.addPage(currentPage);
        contentStream = new PDPageContentStream(document, currentPage);
        yPosition = PAGE_HEIGHT - MARGIN;
    }

    private void writeTitle(String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        yPosition -= 22;
    }

    private void writeSubtitle(String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.setNonStrokingColor(100, 100, 100);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.setNonStrokingColor(0, 0, 0);
        yPosition -= 14;
    }

    private void drawSeparator() throws IOException {
        yPosition -= 5;
        contentStream.setStrokingColor(220, 220, 220);
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, yPosition);
        contentStream.stroke();
        yPosition -= 15;
    }

    private void writeChangeSection(String heading, String what, String files, String why, String interview) throws IOException {
        // Heading
        checkNewPageRequired(18);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.setNonStrokingColor(13, 115, 119); // Teal (#0D7377)
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(heading);
        contentStream.endText();
        contentStream.setNonStrokingColor(0, 0, 0);
        yPosition -= 13;

        // Details
        writeWrappedBullet("What changed: ", what);
        writeWrappedBullet("Files modified: ", files);
        writeWrappedBullet("Why this change: ", why);
        writeWrappedBullet("Interview summary: ", interview);
        yPosition -= 8; // gap between items
    }

    private void writeWrappedBullet(String prefix, String body) throws IOException {
        String fullText = "• " + prefix + body;
        List<String> wrappedLines = wrapText(fullText, WRAP_WIDTH, PDType1Font.HELVETICA, 8);
        for (String line : wrappedLines) {
            checkNewPageRequired(11);
            contentStream.beginText();
            // Bolding the bullet prefix for readability
            contentStream.setFont(PDType1Font.HELVETICA, 8);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(line);
            contentStream.endText();
            yPosition -= 11;
        }
    }

    private void checkNewPageRequired(float height) throws IOException {
        if (yPosition - height < MARGIN) {
            addNewPage();
        }
    }

    private List<String> wrapText(String text, float width, PDFont font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        String safeText = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
        String[] words = safeText.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String testLine = line.length() == 0 ? word : line + " " + word;
            float lineWidth = 0;
            try {
                lineWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            } catch (IllegalArgumentException e) {
                String cleanWord = word.replaceAll("[^\\x20-\\x7E]", "");
                testLine = line.length() == 0 ? cleanWord : line + " " + cleanWord;
                lineWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            }
            if (lineWidth > width) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    lines.add(testLine);
                    line = new StringBuilder();
                }
            } else {
                line.append(line.length() == 0 ? "" : " ").append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
    @Test
    void testSessionIsolationAndHistoryDeletion() {
        String sessionA = "uuid-session-aaaa-1111";
        String sessionB = "uuid-session-bbbb-2222";

        // Create Analysis requests
        ResumeRequest reqA = new ResumeRequest();
        reqA.setText("Java Developer resume text.");
        reqA.setSelectedRole("Java Developer");
        
        ResumeRequest reqB = new ResumeRequest();
        reqB.setText("Python Developer resume text.");
        reqB.setSelectedRole("Python Developer");

        // Run analyze for session A
        ResumeResponse resA = controller.analyze(reqA, sessionA, null);
        // Run analyze for session B
        ResumeResponse resB = controller.analyze(reqB, sessionB, null);

        // Fetch history for session A
        List<AnalysisHistory> historyA = controller.getHistory(sessionA, null);
        org.junit.jupiter.api.Assertions.assertEquals(1, historyA.size());
        org.junit.jupiter.api.Assertions.assertEquals("Java Developer resume text.", historyA.get(0).getResumeText());

        // Fetch history for session B
        List<AnalysisHistory> historyB = controller.getHistory(sessionB, null);
        org.junit.jupiter.api.Assertions.assertEquals(1, historyB.size());
        org.junit.jupiter.api.Assertions.assertEquals("Python Developer resume text.", historyB.get(0).getResumeText());

        // Clear history for session A
        controller.clearHistory(sessionA, null);

        // Verify history A is empty
        List<AnalysisHistory> historyACleared = controller.getHistory(sessionA, null);
        org.junit.jupiter.api.Assertions.assertEquals(0, historyACleared.size());

        // Verify history B is STILL intact!
        List<AnalysisHistory> historyBIntact = controller.getHistory(sessionB, null);
        org.junit.jupiter.api.Assertions.assertEquals(1, historyBIntact.size());
        org.junit.jupiter.api.Assertions.assertEquals("Python Developer resume text.", historyBIntact.get(0).getResumeText());

        // Cleanup session B
        controller.clearHistory(sessionB, null);
    }

    @Test
    void testAutoExpiryScheduler() {
        // Clear all database records first
        repository.deleteAll();

        // Save a stale record (older than 30 days, e.g. 35 days ago)
        AnalysisHistory staleRecord = new AnalysisHistory();
        staleRecord.setResumeText("Old Stale Resume");
        staleRecord.setScore(50);
        staleRecord.setSessionId("scheduler-test-session");
        staleRecord.setTimestamp(LocalDateTime.now().minusDays(35));
        repository.save(staleRecord);

        // Save an active record (younger than 30 days, e.g. 5 days ago)
        AnalysisHistory activeRecord = new AnalysisHistory();
        activeRecord.setResumeText("Active Fresh Resume");
        activeRecord.setScore(80);
        activeRecord.setSessionId("scheduler-test-session");
        activeRecord.setTimestamp(LocalDateTime.now().minusDays(5));
        repository.save(activeRecord);

        // Verify initial state (2 records in database)
        org.junit.jupiter.api.Assertions.assertEquals(2, repository.count());

        // Instantiate and trigger the cleanup task manually
        HistoryCleanupScheduler scheduler = new HistoryCleanupScheduler();
        org.springframework.test.util.ReflectionTestUtils.setField(scheduler, "repository", repository);

        scheduler.cleanupOldHistory();

        // Verify only 1 active record remains in database
        org.junit.jupiter.api.Assertions.assertEquals(1, repository.count());
        List<AnalysisHistory> remaining = repository.findAll();
        org.junit.jupiter.api.Assertions.assertEquals("Active Fresh Resume", remaining.get(0).getResumeText());

        // Cleanup
        repository.deleteAll();
    }
}
