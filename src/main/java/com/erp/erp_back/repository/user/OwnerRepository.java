package com.erp.erp_back.repository.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.user.Owner;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
    // 기본적인 CRUD 메소드가 이미 모두 구현되어 있음
    Optional<Owner> findByEmail(String email);

    @Query("SELECT o FROM Owner o " +
           "WHERE (:q = '' OR o.username LIKE %:q% OR o.email LIKE %:q%)")
    Page<Owner> findAdminOwners(
            @Param("q") String q,
            Pageable pageable
    );
}