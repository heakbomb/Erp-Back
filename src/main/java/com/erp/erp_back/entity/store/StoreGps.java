package com.erp.erp_back.entity.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "store_gps")
@Getter
@Setter
@NoArgsConstructor
public class StoreGps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_gps_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "gps_radius_m")
    private Integer gpsRadiusM;
}