package com.erp.erp_back.repository.erp;

import com.erp.erp_back.entity.erp.SalesLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesLineItemRepository extends JpaRepository<SalesLineItem, Long> {
}