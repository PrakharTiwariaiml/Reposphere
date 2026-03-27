package com.Notes.rep.Repository;

import com.Notes.rep.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<CalendarEvent, Long> {
    // Spring Data JPA will automatically generate the SQL for this
    List<CalendarEvent> findByUserId(Long userId);
    List<CalendarEvent> findByUserIdOrderByEventDateAsc(Long userId);
}