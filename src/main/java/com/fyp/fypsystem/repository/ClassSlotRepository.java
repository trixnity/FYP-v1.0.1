package com.fyp.fypsystem.repository;

import com.fyp.fypsystem.model.ClassSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassSlotRepository extends JpaRepository<ClassSlot, Long> {
    List<ClassSlot> findByCoachIdOrderByDayOfWeekAscStartTimeAsc(Long coachId);
}
