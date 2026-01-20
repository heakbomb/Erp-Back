package com.erp.erp_back.entity.ai;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "weather_hourly")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherHourlyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weatherId;

    private Integer nx;
    private Integer ny;

    @Column(name = "forecast_date")
    private LocalDate forecastDate;

    @Column(name = "forecast_time")
    private LocalTime forecastTime;

    private Double temperature;
    
    @Column(name = "rainfall_mm")
    private Double rainfallMm;
    
    private Integer humidity;
}
