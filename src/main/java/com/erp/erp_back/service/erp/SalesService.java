package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.common.ErrorCodes;
import com.erp.erp_back.dto.erp.PosOrderRequest;
import com.erp.erp_back.dto.erp.PosOrderResponse;
import com.erp.erp_back.dto.erp.RefundRequest;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.SalesLineItem;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.mapper.SalesMapper;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final StoreRepository storeRepository;
    private final MenuItemRepository menuItemRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    
    private final RecipeIngredientService recipeService;
    private final InventoryService inventoryService;
    
    private final SalesMapper salesMapper;

    @Transactional
    public PosOrderResponse createPosOrder(PosOrderRequest req) {

        // 1. 멱등성 체크 (중복 결제 방지)
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<SalesTransaction> existing = salesTransactionRepository
                    .findTopByStoreStoreIdOrderByTransactionTimeDesc(req.getStoreId())
                    .filter(tx -> req.getIdempotencyKey().equals(tx.getIdempotencyKey()));

            if (existing.isPresent()) {
                return salesMapper.toPosOrderResponse(existing.get());
            }
        }

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCodes.STORE_NOT_FOUND));

        // 요청 들어온 모든 메뉴 ID 추출
        List<Long> menuIds = req.getItems().stream()
                .map(PosOrderRequest.PosOrderLine::getMenuId)
                .toList();
        
        // DB에서 한 번에 조회 후 Map으로 변환 (Key: MenuId, Value: Entity)
        Map<Long, MenuItem> menuMap = menuItemRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(MenuItem::getMenuId, Function.identity()));

        List<SalesLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. 메모리 상에서 라인 아이템 생성 (DB 조회 없음)
        for (PosOrderRequest.PosOrderLine lineReq : req.getItems()) {
            MenuItem menu = menuMap.get(lineReq.getMenuId());
            if (menu == null) {
                throw new EntityNotFoundException(ErrorCodes.MENU_NOT_FOUND);
            }

            menu.validateOwner(store);

            BigDecimal realPrice = menu.getPrice(); 
            BigDecimal qty = BigDecimal.valueOf(lineReq.getQuantity());
            BigDecimal lineAmount = realPrice.multiply(qty);

            // Mapper에게 '진짜 가격'을 넘김
            SalesLineItem line = salesMapper.toLineItem(lineReq, menu, realPrice, lineAmount);
            lineItems.add(line);
            
            totalAmount = totalAmount.add(lineAmount);
        }

        // 3. 트랜잭션(영수증) 엔티티 생성 및 연관관계 설정
        BigDecimal finalAmount = totalAmount.subtract(
                req.getTotalDiscount() != null ? req.getTotalDiscount() : BigDecimal.ZERO
        );
        
        SalesTransaction tx = salesMapper.toEntity(req, store, finalAmount);
        
        for (SalesLineItem line : lineItems) {
            line.setSalesTransaction(tx);
            tx.getLineItems().add(line);
        }

        // 4. [재고 처리] 판매된 메뉴 수량 집계 (Map<MenuId, Qty>)
        Map<Long, Integer> soldMenus = lineItems.stream()
                .collect(Collectors.groupingBy(
                        li -> li.getMenuItem().getMenuId(),
                        Collectors.summingInt(SalesLineItem::getQuantity)
                ));

        Map<Long, BigDecimal> requirements = recipeService.calculateRequiredIngredients(soldMenus);
        
        inventoryService.decreaseStock(requirements);

        SalesTransaction saved = salesTransactionRepository.save(tx);
        return salesMapper.toPosOrderResponse(saved);
    }

    /**
     * [결제 취소/환불]
     * - 전체 취소만 지원 (부분 취소 미지원 정책)
     * - 폐기(Waste) 여부에 따라 재고 복구 로직 분기
     */
    @Transactional
    public PosOrderResponse refundPosOrder(RefundRequest req) {
        // 1. 원본 거래 조회
        SalesTransaction tx = salesTransactionRepository.findById(req.getTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("거래 내역을 찾을 수 없습니다."));

       tx.cancel(req.getReason());

        // 4. 재고 복구 로직 (단순 변심 등 폐기가 아닐 경우에만 복구)
        if (Boolean.FALSE.equals(req.getIsWaste())) {
            
            // 4-1. 취소된 메뉴 수량 집계
            Map<Long, Integer> refundedMenus = tx.getLineItems().stream()
                    .collect(Collectors.groupingBy(
                            li -> li.getMenuItem().getMenuId(),
                            Collectors.summingInt(SalesLineItem::getQuantity)
                    ));

            Map<Long, BigDecimal> restoreList = recipeService.calculateRequiredIngredients(refundedMenus);

            inventoryService.increaseStock(restoreList);
        }

        return salesMapper.toPosOrderResponse(tx);
    }
}