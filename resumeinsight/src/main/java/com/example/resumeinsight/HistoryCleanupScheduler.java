package com.example.resumeinsight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class HistoryCleanupScheduler {

    @Autowired
    private AnalysisHistoryRepository repository;

    // Cron expression: daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        System.out.println("Running daily cleanup task for AnalysisHistory records older than " + cutoff);
        try {
            repository.deleteAllByTimestampBefore(cutoff);
            System.out.println("Daily cleanup task completed successfully.");
        } catch (Exception e) {
            System.err.println("Daily cleanup task failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
