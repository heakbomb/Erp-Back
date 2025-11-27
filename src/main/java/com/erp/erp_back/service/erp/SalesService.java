package com.erp.erp_back.service.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.PosOrderRequest;
import com.erp.erp_back.dto.erp.PosOrderResponse;
import com.erp.erp_back.dto.erp.RecentTransactionResponse;
import com.erp.erp_back.dto.erp.SalesDailyStatResponse;
import com.erp.erp_back.dto.erp.SalesSummaryResponse;
import com.erp.erp_back.dto.erp.SalesTransactionSummaryResponse;
import com.erp.erp_back.dto.erp.TopMenuStatsResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.entity.erp.SalesLineItem;
import com.erp.erp_back.entity.erp.SalesTransaction;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.mapper.SalesMapper;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.erp.SalesLineItemRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;
import com.erp.erp_back.repository.store.StoreRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final StoreRepository storeRepository;
    private final MenuItemRepository menuItemRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final InventoryRepository inventoryRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesLineItemRepository salesLineItemRepository;
    private final SalesMapper salesMapper;

    @Transactional
    public PosOrderResponse createPosOrder(PosOrderRequest req) {

        // 1. 멱등성 체크
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<SalesTransaction> existing = salesTransactionRepository
                    .findTopByStoreStoreIdOrderByTransactionTimeDesc(req.getStoreId())
                    .filter(tx -> req.getIdempotencyKey().equals(tx.getIdempotencyKey()));

            if (existing.isPresent()) {
                return salesMapper.toPosOrderResponse(existing.get());
            }
        }

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        List<SalesLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. 라인 아이템 생성 및 총액 계산
        for (PosOrderRequest.PosOrderLine lineReq : req.getItems()) {
            MenuItem menu = menuItemRepository.findById(lineReq.getMenuId())
                    .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

            if (!Objects.equals(menu.getStore().getStoreId(), store.getStoreId())) {
                throw new IllegalArgumentException("MENU_STORE_MISMATCH");
            }

            BigDecimal qty = BigDecimal.valueOf(lineReq.getQuantity());
            BigDecimal lineAmount = lineReq.getUnitPrice().multiply(qty);

            // Mapper 사용 (Entity 생성)
            SalesLineItem line = salesMapper.toLineItem(lineReq, menu, lineAmount);

            lineItems.add(line);
            totalAmount = totalAmount.add(lineAmount);
        }

        BigDecimal totalDiscount = req.getTotalDiscount() != null ? req.getTotalDiscount() : BigDecimal.ZERO;
        totalAmount = totalAmount.subtract(totalDiscount);

        // 3. 트랜잭션 Entity 생성
        SalesTransaction tx = salesMapper.toEntity(req, store, totalAmount);

        // 4. 연관관계 편의 메서드 대체
        for (SalesLineItem line : lineItems) {
            line.setSalesTransaction(tx);
            tx.getLineItems().add(line);
        }

        // 5. 재고 차감 로직
        for (SalesLineItem line : lineItems) {
            consumeInventoryByRecipe(line.getMenuItem().getMenuId(), line.getQuantity());
        }

        SalesTransaction saved = salesTransactionRepository.save(tx);
        return salesMapper.toPosOrderResponse(saved);
    }

    private void consumeInventoryByRecipe(Long menuId, int soldQty) {
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByMenuItemMenuId(menuId);

        for (RecipeIngredient ri : ingredients) {
            Inventory inv = ri.getInventory();
            if (inv == null)
                continue;
            if (inv.getStatus() == ActiveStatus.INACTIVE)
                continue;

            BigDecimal baseQty = ri.getConsumptionQty(); 
            BigDecimal totalConsumption = baseQty.multiply(BigDecimal.valueOf(soldQty));

            BigDecimal current = inv.getStockQty();
            BigDecimal after = current.subtract(totalConsumption);

            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("INSUFFICIENT_STOCK: " + inv.getItemName());
            }

            inv.setStockQty(after);
            inventoryRepository.save(inv);
        }
    }

    @Transactional(readOnly = true)
    public List<SalesDailyStatResponse> getDailyStats(Long storeId, LocalDate from, LocalDate to) {
        return getSalesStats(storeId, from, to, "DAY");
    }

    @Transactional(readOnly = true)
    public List<SalesDailyStatResponse> getSalesStats(
            Long storeId,
            LocalDate from,
            LocalDate to,
            String period) {

        LocalDateTime startDate = from.atStartOfDay();
        LocalDateTime endDate = to.atTime(23, 59, 59);

        List<Map<String, Object>> rows = salesTransactionRepository.findDailySalesStats(storeId, startDate, endDate);
        String normalized = (period == null) ? "DAY" : period.toUpperCase(Locale.ROOT);

        if ("DAY".equals(normalized)) {
            List<SalesDailyStatResponse> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String date = (String) row.get("date");
                BigDecimal sales = (BigDecimal) row.getOrDefault("sales", BigDecimal.ZERO);
                result.add(new SalesDailyStatResponse(date, sales));
            }
            return result;
        }

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, BigDecimal> agg = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String dateStr = (String) row.get("date");
            BigDecimal sales = (BigDecimal) row.get("sales");
            if (sales == null)
                sales = BigDecimal.ZERO;

            LocalDate date = LocalDate.parse(dateStr, dayFormatter);

            String keyLabel;
            switch (normalized) {
                case "WEEK" -> {
                    LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                    LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
                    DateTimeFormatter mmdd = DateTimeFormatter.ofPattern("MM/dd");
                    keyLabel = weekStart.format(mmdd) + "~" + weekEnd.format(mmdd);
                }
                case "MONTH" -> {
                    int year = date.getYear();
                    int month = date.getMonthValue();
                    keyLabel = String.format("%04d-%02d", year, month);
                }
                case "YEAR" -> keyLabel = String.valueOf(date.getYear());
                default -> keyLabel = dateStr;
            }

            agg.merge(keyLabel, sales, BigDecimal::add);
        }

        List<SalesDailyStatResponse> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : agg.entrySet()) {
            result.add(new SalesDailyStatResponse(e.getKey(), e.getValue()));
        }
        return result;
    }

    // 오늘 기준 인기 메뉴 TOP5
    @Transactional(readOnly = true)
    public List<TopMenuStatsResponse> getTodayTopMenus(Long storeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Object[]> rows = salesLineItemRepository.findMenuAggBetween(storeId, start, end);

        List<TopMenuStatsResponse> result = new ArrayList<>();
        int limit = Math.min(5, rows.size());

        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            
            // ✅ [수정됨] Repository 쿼리 순서: menuId(0), menuName(1), quantity(2), revenue(3)
            Long menuId = (Long) row[0];        // 1. Long으로 캐스팅
            String name = (String) row[1];      // 2. String으로 캐스팅
            long qty = ((Number) row[2]).longValue();
            BigDecimal revenue = (BigDecimal) row[3];

            // Mapper 호출
            result.add(salesMapper.toTopMenuStats(
                    menuId, 
                    name, 
                    qty,
                    revenue,
                    0.0 
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RecentTransactionResponse> getTodayRecentTransactions(Long storeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<SalesTransaction> txs = salesTransactionRepository.findRecentTransactions(storeId, start, end);
        
        return txs.stream()
                .limit(20)
                .map(salesMapper::toRecentTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesDailyStatResponse> getStatsByPeriod(Long storeId, String period) {
        LocalDate today = LocalDate.now();
        String norm = (period == null) ? "DAY" : period.toUpperCase(Locale.ROOT);

        if ("MONTH".equals(norm)) {
            List<SalesDailyStatResponse> result = new ArrayList<>();
            YearMonth thisMonth = YearMonth.from(today);
            YearMonth startMonth = thisMonth.minusMonths(2); 

            DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");

            for (int i = 0; i < 3; i++) {
                YearMonth ym = startMonth.plusMonths(i);
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.equals(thisMonth) ? today : ym.atEndOfMonth();

                BigDecimal total = sumSales(storeId, start, end);
                result.add(new SalesDailyStatResponse(ym.format(ymFmt), total));
            }
            return result;
        }

        if ("WEEK".equals(norm)) {
            LocalDate thisWeekStart = today
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
            LocalDate thisWeekEnd = thisWeekStart.plusDays(6);

            LocalDate from = thisWeekStart.minusWeeks(3);
            LocalDate to = thisWeekEnd;

            return getSalesStats(storeId, from, to, "WEEK");
        }

        LocalDate from;
        switch (norm) {
            case "DAY" -> from = today.minusDays(6);
            case "YEAR" -> from = today.minusYears(1);
            default -> from = today.minusDays(6);
        }

        return getSalesStats(storeId, from, today, norm);
    }

    @Transactional(readOnly = true)
    public List<TopMenuStatsResponse> getTopMenusByPeriod(Long storeId, String period) {
        LocalDate today = LocalDate.now();

        LocalDate curFrom;
        LocalDate curTo;
        LocalDate prevFrom;
        LocalDate prevTo;

        String norm = (period == null) ? "DAY" : period.toUpperCase();

        switch (norm) {
            case "WEEK": {
                curTo = today;
                curFrom = today.minusDays(6);
                prevTo = curFrom.minusDays(1);
                prevFrom = prevTo.minusDays(6);
                break;
            }
            case "MONTH": {
                curFrom = today.withDayOfMonth(1);
                curTo = today;
                LocalDate lastMonth = curFrom.minusMonths(1);
                prevFrom = lastMonth.withDayOfMonth(1);
                prevTo = curFrom.minusDays(1);
                break;
            }
            case "YEAR": {
                curFrom = today.withDayOfYear(1);
                curTo = today;
                LocalDate lastYear = curFrom.minusYears(1);
                prevFrom = lastYear.withDayOfYear(1);
                prevTo = curFrom.minusDays(1);
                break;
            }
            case "DAY":
            default: {
                curFrom = today;
                curTo = today;
                prevFrom = today.minusDays(1);
                prevTo = prevFrom;
                break;
            }
        }

        LocalDateTime curStart = curFrom.atStartOfDay();
        LocalDateTime curEnd = curTo.plusDays(1).atStartOfDay();
        LocalDateTime prevStart = prevFrom.atStartOfDay();
        LocalDateTime prevEnd = prevTo.plusDays(1).atStartOfDay();

        List<Object[]> curRows = salesLineItemRepository.findMenuAggBetween(storeId, curStart, curEnd);
        List<Object[]> prevRows = salesLineItemRepository.findMenuAggBetween(storeId, prevStart, prevEnd);

        Map<Long, BigDecimal> prevRevenueMap = new HashMap<>();
        for (Object[] row : prevRows) {
            Long menuId = (Long) row[0];
            BigDecimal revenue = (BigDecimal) row[3];
            if (revenue == null)
                revenue = BigDecimal.ZERO;
            prevRevenueMap.put(menuId, revenue);
        }

        List<TopMenuStatsResponse> result = new ArrayList<>();

        for (Object[] row : curRows) {
            Long menuId = (Long) row[0];
            String name = (String) row[1];
            long quantity = ((Number) row[2]).longValue();
            BigDecimal revenue = (BigDecimal) row[3];
            if (revenue == null)
                revenue = BigDecimal.ZERO;

            BigDecimal prevRevenue = prevRevenueMap.getOrDefault(menuId, BigDecimal.ZERO);

            double growth = 0.0;
            if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growth = revenue.subtract(prevRevenue)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prevRevenue, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(salesMapper.toTopMenuStats(menuId, name, quantity, revenue, growth));
        }

        result.sort(Comparator.comparing(TopMenuStatsResponse::getRevenue).reversed());
        return result.size() > 5 ? result.subList(0, 5) : result;
    }

    @Transactional(readOnly = true)
    public List<RecentTransactionResponse> getRecentTransactions(Long storeId) {
        List<SalesTransaction> txList = salesTransactionRepository
                .findTop20ByStoreStoreIdOrderByTransactionTimeDesc(storeId);
        
        return txList.stream()
                .map(salesMapper::toRecentTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalesTransactionSummaryResponse> getTransactionsByRange(Long storeId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<SalesTransaction> list = salesTransactionRepository
                .findByStoreStoreIdAndTransactionTimeBetweenOrderByTransactionTimeDesc(storeId, start, end);

        return list.stream()
                .map(salesMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(Long storeId) {

        LocalDate today = LocalDate.now();

        BigDecimal todaySales = sumSales(storeId, today, today);
        BigDecimal yesterdaySales = sumSales(storeId, today.minusDays(1), today.minusDays(1));
        BigDecimal todayRate = calcChangeRate(todaySales, yesterdaySales);

        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);

        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);
        LocalDate lastWeekStart = lastWeekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        BigDecimal thisWeekSales = sumSales(storeId, thisWeekStart, thisWeekEnd);
        BigDecimal lastWeekSales = sumSales(storeId, lastWeekStart, lastWeekEnd);
        BigDecimal weekRate = calcChangeRate(thisWeekSales, lastWeekSales);

        YearMonth ym = YearMonth.from(today);
        LocalDate thisMonthStart = ym.atDay(1);
        LocalDate lastMonthStart = ym.minusMonths(1).atDay(1);
        LocalDate lastMonthEnd = thisMonthStart.minusDays(1);

        BigDecimal thisMonthSales = sumSales(storeId, thisMonthStart, today);
        BigDecimal lastMonthSales = sumSales(storeId, lastMonthStart, lastMonthEnd);
        BigDecimal monthRate = calcChangeRate(thisMonthSales, lastMonthSales);

        BigDecimal thisMonthAvg = calcAvgTicket(storeId, thisMonthStart, today);
        BigDecimal lastMonthAvg = calcAvgTicket(storeId, lastMonthStart, lastMonthEnd);
        BigDecimal avgRate = calcChangeRate(thisMonthAvg, lastMonthAvg);

        return salesMapper.toSalesSummary(
                todaySales, todayRate,
                thisWeekSales, weekRate,
                thisMonthSales, monthRate,
                thisMonthAvg, avgRate
        );
    }

    private BigDecimal sumSales(Long storeId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); 
        BigDecimal v = salesTransactionRepository
                .sumTotalAmountByStoreIdBetween(storeId, start, end);
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal calcChangeRate(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal calcAvgTicket(Long storeId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        long count = salesTransactionRepository
                .countByStoreStoreIdAndTransactionTimeBetween(storeId, start, end);

        if (count == 0)
            return BigDecimal.ZERO;

        BigDecimal total = sumSales(storeId, from, to);
        return total.divide(BigDecimal.valueOf(count), 0, RoundingMode.DOWN);
    }

}