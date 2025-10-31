package com.erp.erp_back.entity.hr;

import java.time.LocalDate;

import com.erp.erp_back.entity.store.Store;

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
@Table(name = "EmployeeDocument")
@Data
@NoArgsConstructor
public class EmployeeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;
    
    // [추가] 원본 파일명 (다운로드 시 필요)
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    // [추가] 파일 MIME 타입 (다운로드 시 필요)
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "retention_end_date", nullable = false)
    private LocalDate retentionEndDate;
}