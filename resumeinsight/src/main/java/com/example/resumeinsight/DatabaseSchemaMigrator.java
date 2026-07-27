package com.example.resumeinsight;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class DatabaseSchemaMigrator implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        String[] columns = {
            "resume_text", "job_description", "matched_skills", "missing_skills",
            "matched_soft_skills", "missing_soft_skills", "ats_warnings",
            "grammar_issues_json", "roadmap", "best_role", "filename", "session_id"
        };
        System.out.println("Running database migration to widen columns in analysis_history...");
        for (String col : columns) {
            try {
                jdbcTemplate.execute("ALTER TABLE analysis_history ALTER COLUMN " + col + " TYPE TEXT");
                System.out.println(" - Column '" + col + "' successfully widened to TEXT.");
            } catch (Exception e) {
                System.out.println(" - Column '" + col + "' migration skipped/failed: " + e.getMessage());
            }
        }
        System.out.println("Database migration finished.");
    }
}
