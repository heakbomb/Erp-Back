package com.erp.erp_back.entity.ai;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "external_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private String name;        // "월드컵", "불꽃축제"

    @Column(nullable = false)
    private String type;        // "SPORTS", "FESTIVAL"

    private int importance;     // 1~5
}