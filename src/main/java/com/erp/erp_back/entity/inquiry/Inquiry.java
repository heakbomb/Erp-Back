package com.erp.erp_back.entity.inquiry;

import com.erp.erp_back.entity.enums.InquiryCategory;
import com.erp.erp_back.entity.enums.InquiryStatus;
import com.erp.erp_back.entity.user.Admin;
import com.erp.erp_back.entity.user.Owner;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "inquiry")
@EntityListeners(AuditingEntityListener.class) // 날짜 자동 생성을 위해 필요
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    // 관리자 답변 등록 메서드 (Dirty Checking 용)
    public void reply(Admin admin, String answer) {
        this.admin = admin;
        this.answer = answer;
        this.status = InquiryStatus.RESPONDED;
        this.answeredAt = LocalDateTime.now();
    }
}