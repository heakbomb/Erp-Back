package com.erp.erp_back.repository.ai;

import com.erp.erp_back.dto.ai.EventDto;
import com.erp.erp_back.entity.ai.ExternalEventEntity; // ✅ 이 import가 빠져있었습니다.
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExternalEventRepository extends JpaRepository<ExternalEventEntity, Long> {

    @Query("SELECT new com.erp.erp_back.dto.ai.EventDto(" +
           "  FUNCTION('DATE_FORMAT', e.eventDate, '%Y-%m-%d'), " +
           "  e.name, " +
           "  e.type, " +
           "  e.importance) " +
           "FROM ExternalEventEntity e " +
           "WHERE e.eventDate BETWEEN :startDate AND :endDate")
    List<EventDto> findEvents(LocalDate startDate, LocalDate endDate);
}