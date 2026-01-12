package com.erp.erp_back.entity.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "holiday_calendar")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayEntity {

    @Id
    @Column(name = "holiday_date")
    private LocalDate date;

    private String name;

    @Column(name = "is_holiday")
    private boolean isHoliday;
}
