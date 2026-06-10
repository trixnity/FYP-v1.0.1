package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.ZoomSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ZoomSessionRepository extends JpaRepository<ZoomSession, Long> {
    List<ZoomSession> findByCoachId(Long coachId);
    List<ZoomSession> findByCoachIdAndScheduledAtAfter(Long coachId, LocalDateTime after);
    List<ZoomSession> findByScheduledAtAfterOrderByScheduledAtAsc(LocalDateTime after);
    List<ZoomSession> findByCoachIdInAndScheduledAtAfterOrderByScheduledAtAsc(List<Long> coachIds, LocalDateTime after);
}
