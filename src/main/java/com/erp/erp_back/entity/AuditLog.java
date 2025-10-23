package com.erp.erp_back.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AuditLog")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_type", length = 20)
    private String userType;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "target_table", nullable = false, length = 50)
    private String targetTable;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}