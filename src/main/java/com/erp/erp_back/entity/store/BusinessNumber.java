package com.erp.erp_back.entity.store;

import java.util.List;

import com.erp.erp_back.entity.user.Owner;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "BusinessNumber") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "biz_id")
    private Long bizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false)
    private String phone;

    @Column(name = "biz_num", nullable = false, length = 10)
    private String bizNum;

    // Corrected relationship: BusinessNumber has a One-to-Many with Store
    @OneToMany(mappedBy = "businessNumber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Store> stores;
}