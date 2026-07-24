package com.example.resumeinsight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {
    List<AnalysisHistory> findAllBySessionIdOrderByTimestampDesc(String sessionId);

    @Modifying
    @Transactional
    void deleteAllBySessionId(String sessionId);

    @Modifying
    @Transactional
    void deleteAllByTimestampBefore(LocalDateTime dateTime);
}
