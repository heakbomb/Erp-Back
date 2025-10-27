package com.erp.erp_back.entity.log;

import java.time.LocalDateTime;

import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AttendanceLog")
@Data
@NoArgsConstructor
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(name = "client_ip", length = 40)
    private String clientIp;
}