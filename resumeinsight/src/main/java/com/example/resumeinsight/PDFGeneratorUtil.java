package com.example.resumeinsight;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFGeneratorUtil {

    private static final float PAGE_WIDTH = 612; // Standard Letter size
    private static final float PAGE_HEIGHT = 792;
    private static final float MARGIN = 50;
    private static final float WRAP_WIDTH = PAGE_WIDTH - (2 * MARGIN);

    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream contentStream;
    private float yPosition;

    public PDFGeneratorUtil() {
        this.document = new PDDocument();
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

    /**
     * Entry point to export analysis results to PDF byte array.
     */
    public byte[] generateReport(AnalysisHistory history) throws IOException {
        addNewPage();

        // Header Title
        writeTitle("Resume Insight Analysis Report");
        writeSubtitle("Timestamp: " + history.getTimestamp().toString());
        writeSubtitle("Source File: " + (history.getFilename() != null ? history.getFilename() : "Pasted Resume Text"));
        drawSeparator();

        // Match Scores
        writeSectionHeader("Match Results");
        writeParagraph("Match Score: " + history.getScore() + "%");
        writeParagraph("Best Matched Role: " + (history.getBestRole() != null ? history.getBestRole() : "N/A"));
        drawSeparator();

        // Skill Badges list representation
        writeSectionHeader("Skills Summary");
        writeParagraph("Matched Skills: " + (history.getMatchedSkills() != null && !history.getMatchedSkills().isEmpty() 
            ? history.getMatchedSkills().replace(",", ", ") : "None"));
        writeParagraph("Missing Skills: " + (history.getMissingSkills() != null && !history.getMissingSkills().isEmpty() 
            ? history.getMissingSkills().replace(",", ", ") : "None"));
        drawSeparator();

        // Grammar & Spelling Review list
        writeSectionHeader("Grammar & Spelling Review");
        String grammarJson = history.getGrammarIssuesJson();
        if (grammarJson == null || grammarJson.trim().isEmpty() || grammarJson.equals("[]")) {
            writeParagraph("Excellent! No grammar or spelling issues were detected in your resume.");
        } else {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<?> issuesRaw = mapper.readValue(grammarJson, List.class);
                if (issuesRaw.isEmpty()) {
                    writeParagraph("Excellent! No grammar or spelling issues were detected in your resume.");
                } else {
                    for (int i = 0; i < Math.min(issuesRaw.size(), 10); i++) {
                        java.util.LinkedHashMap<?, ?> issue = (java.util.LinkedHashMap<?, ?>) issuesRaw.get(i);
                        writeParagraph((i + 1) + ". Issue: " + issue.get("message"));
                        writeParagraph("   Context: \"" + String.valueOf(issue.get("context")).trim() + "\"");
                        
                        List<?> sugs = (List<?>) issue.get("suggestions");
                        if (sugs != null && !sugs.isEmpty()) {
                            List<String> sugsStr = new ArrayList<>();
                            for (Object sug : sugs) sugsStr.add(String.valueOf(sug));
                            writeParagraph("   Suggestions: " + String.join(", ", sugsStr));
                        }
                        yPosition -= 4; // micro line gap
                    }
                    if (issuesRaw.size() > 10) {
                        writeParagraph("... and " + (issuesRaw.size() - 10) + " more issues detected.");
                    }
                }
            } catch (Exception e) {
                writeParagraph("Grammar issues: " + grammarJson);
            }
        }
        drawSeparator();

        // AI Learning Roadmap
        if (history.getRoadmap() != null && !history.getRoadmap().trim().isEmpty()) {
            writeSectionHeader("AI-Powered 3-Month Learning Roadmap");
            writeLongText(history.getRoadmap());
        } else {
            writeSectionHeader("AI Learning Roadmap");
            writeParagraph("No AI roadmap was generated for this analysis. (Opt-in to generate a roadmap on the dashboard.)");
        }

        if (contentStream != null) {
            contentStream.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        return baos.toByteArray();
    }

    private void writeTitle(String text) throws IOException {
        checkNewPageRequired(35);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        yPosition -= 28;
    }

    private void writeSubtitle(String text) throws IOException {
        checkNewPageRequired(18);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.setNonStrokingColor(100, 100, 100);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.setNonStrokingColor(0, 0, 0); // reset color
        yPosition -= 14;
    }

    private void writeSectionHeader(String text) throws IOException {
        checkNewPageRequired(28);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 13);
        contentStream.setNonStrokingColor(13, 115, 119); // Deep Teal (#0D7377)
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.setNonStrokingColor(0, 0, 0); // reset
        yPosition -= 18;
    }

    private void writeParagraph(String text) throws IOException {
        // Standard body leading 14
        writeWrappedText(text, PDType1Font.HELVETICA, 9, 14);
    }

    private void writeLongText(String text) throws IOException {
        // Strip out non-printable ASCII or characters PDFBox standard fonts fail to draw (e.g. asterisks for bolding)
        // Markdown formatting like **bold** is cleaned to plain text for pdf drawing simplicity
        String cleanText = text.replace("**", "").replace("* ", "• ").replace("### ", "").replace("## ", "").replace("# ", "");
        String[] paragraphs = cleanText.split("\n");
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) {
                yPosition -= 8;
                continue;
            }
            writeWrappedText(para, PDType1Font.HELVETICA, 9, 13);
        }
    }

    private void writeWrappedText(String text, PDFont font, float fontSize, float leading) throws IOException {
        List<String> wrappedLines = wrapText(text, WRAP_WIDTH, font, fontSize);
        for (String wrappedLine : wrappedLines) {
            checkNewPageRequired(leading);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(wrappedLine);
            contentStream.endText();
            yPosition -= leading;
        }
    }

    private void drawSeparator() throws IOException {
        checkNewPageRequired(15);
        yPosition -= 5;
        contentStream.setStrokingColor(220, 220, 220);
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, yPosition);
        contentStream.stroke();
        yPosition -= 12;
    }

    private void checkNewPageRequired(float heightNeeded) throws IOException {
        if (yPosition - heightNeeded < MARGIN) {
            addNewPage();
        }
    }

    private List<String> wrapText(String text, float width, PDFont font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        // Replace all line breaks/newlines with a space first to prevent word merging
        String safeText = text.replace("\r", " ").replace("\n", " ");
        // PDFBox HELVETICA doesn't support some special control characters, so clean them up
        safeText = safeText.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");
        String[] words = safeText.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String testLine = line.length() == 0 ? word : line + " " + word;
            float lineWidth = 0;
            try {
                lineWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            } catch (IllegalArgumentException e) {
                // If character cannot be mapped in WinAnsiEncoding, strip it out
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
}
