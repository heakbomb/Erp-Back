package com.erp.erp_back.repository.ai;

import com.erp.erp_back.dto.ai.HolidayDto;
import com.erp.erp_back.entity.ai.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<HolidayEntity, LocalDate> {

    @Query("SELECT new com.erp.erp_back.dto.ai.HolidayDto(" +
           "  function('date_format', h.date, '%Y-%m-%d'), " +
           "  h.name, h.isHoliday) " +
           "FROM HolidayEntity h " +
           "WHERE h.date BETWEEN :startDate AND :endDate")
    List<HolidayDto> findHolidays(LocalDate startDate, LocalDate endDate);
}