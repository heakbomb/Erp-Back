package com.erp.erp_back.service.erp;

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
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.repository.erp.MenuItemRepository;
import com.erp.erp_back.repository.erp.RecipeIngredientRepository;
import com.erp.erp_back.repository.erp.SalesLineItemRepository;
import com.erp.erp_back.repository.erp.SalesTransactionRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final StoreRepository storeRepository;
    private final MenuItemRepository menuItemRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final InventoryRepository inventoryRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesLineItemRepository salesLineItemRepository;

    private static final DateTimeFormatter TX_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public PosOrderResponse createPosOrder(PosOrderRequest req) {

        // 1-1. 멱등성 체크 (store + idempotencyKey 기준으로 마지막 거래 재사용)
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<SalesTransaction> existing = salesTransactionRepository
                    .findTopByStoreStoreIdOrderByTransactionTimeDesc(req.getStoreId())
                    .filter(tx -> req.getIdempotencyKey().equals(tx.getIdempotencyKey()));

            if (existing.isPresent()) {
                return toDTO(existing.get());
            }
        }

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("STORE_NOT_FOUND"));

        List<SalesLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PosOrderRequest.PosOrderLine lineReq : req.getItems()) {
            MenuItem menu = menuItemRepository.findById(lineReq.getMenuId())
                    .orElseThrow(() -> new EntityNotFoundException("MENU_NOT_FOUND"));

            if (!Objects.equals(menu.getStore().getStoreId(), store.getStoreId())) {
                throw new IllegalArgumentException("MENU_STORE_MISMATCH");
            }

            BigDecimal qty = BigDecimal.valueOf(lineReq.getQuantity());
            BigDecimal lineAmount = lineReq.getUnitPrice().multiply(qty);

            SalesLineItem line = SalesLineItem.builder()
                    .menuItem(menu)
                    .quantity(lineReq.getQuantity())
                    .unitPrice(lineReq.getUnitPrice())
                    .lineAmount(lineAmount)
                    .build();

            lineItems.add(line);
            totalAmount = totalAmount.add(lineAmount);
        }

        BigDecimal totalDiscount = req.getTotalDiscount() != null ? req.getTotalDiscount() : BigDecimal.ZERO;

        totalAmount = totalAmount.subtract(totalDiscount);

        // 1-3. SalesTransaction Builder 사용
        SalesTransaction tx = SalesTransaction.builder()
                .store(store)
                .transactionTime(LocalDateTime.now())
                .totalAmount(totalAmount)
                .totalDiscount(totalDiscount)
                .paymentMethod(req.getPaymentMethod())
                .status("PAID")
                .idempotencyKey(req.getIdempotencyKey())
                .build();

        for (SalesLineItem line : lineItems) {
            line.setSalesTransaction(tx);
            tx.getLineItems().add(line);
        }

        for (SalesLineItem line : lineItems) {
            consumeInventoryByRecipe(line.getMenuItem().getMenuId(), line.getQuantity());
        }

        SalesTransaction saved = salesTransactionRepository.save(tx);

        return toDTO(saved);
    }

    private void consumeInventoryByRecipe(Long menuId, int soldQty) {
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByMenuItemMenuId(menuId);

        for (RecipeIngredient ri : ingredients) {
            Inventory inv = ri.getInventory();
            if (inv == null)
                continue;
            if (inv.getStatus() == ActiveStatus.INACTIVE)
                continue;

            BigDecimal baseQty = ri.getConsumptionQty(); // 1인분 소모량
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

    /**
     * ⚡ 기존 일별 집계용 메서드는 내부적으로 DAY 기준 집계 메서드를 사용하게 변경
     */
    @Transactional(readOnly = true)
    public List<SalesDailyStatResponse> getDailyStats(Long storeId, LocalDate from, LocalDate to) {
        return getSalesStats(storeId, from, to, "DAY");
    }

    /**
     * 📊 period(DAY/WEEK/MONTH/YEAR)에 맞게 그룹핑해서 매출 합계를 반환
     */
    @Transactional(readOnly = true)
    public List<SalesDailyStatResponse> getSalesStats(
            Long storeId,
            LocalDate from,
            LocalDate to,
            String period) {

        LocalDateTime startDate = from.atStartOfDay();
        LocalDateTime endDate = to.atTime(23, 59, 59);

        // 1) 일자별로 먼저 가져옴
        List<Map<String, Object>> rows = salesTransactionRepository.findDailySalesStats(storeId, startDate, endDate);

        String normalized = (period == null) ? "DAY" : period.toUpperCase(Locale.ROOT);

        if ("DAY".equals(normalized)) {
            List<SalesDailyStatResponse> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String date = (String) row.get("date"); // "YYYY-MM-DD"
                BigDecimal sales = (BigDecimal) row.getOrDefault("sales", BigDecimal.ZERO);
                result.add(new SalesDailyStatResponse(date, sales));
            }
            return result;
        }

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, BigDecimal> agg = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String dateStr = (String) row.get("date"); // "2025-11-21"
            BigDecimal sales = (BigDecimal) row.get("sales");
            if (sales == null)
                sales = BigDecimal.ZERO;

            LocalDate date = LocalDate.parse(dateStr, dayFormatter);

            String keyLabel;
            switch (normalized) {
                case "WEEK" -> {
                    // ✅ 일요일 ~ 토요일 한 주 기준
                    LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                    LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
                    DateTimeFormatter mmdd = DateTimeFormatter.ofPattern("MM/dd");
                    keyLabel = weekStart.format(mmdd) + "~" + weekEnd.format(mmdd); // 예) 11/23~11/29
                }
                case "MONTH" -> {
                    int year = date.getYear();
                    int month = date.getMonthValue();
                    keyLabel = String.format("%04d-%02d", year, month); // 예) 2025-11
                }
                case "YEAR" -> keyLabel = String.valueOf(date.getYear()); // 예) 2025
                default -> keyLabel = dateStr; // fallback
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
            String name = (String) row[0];
            long qty = ((Number) row[1]).longValue();
            BigDecimal revenue = (BigDecimal) row[2];

            result.add(TopMenuStatsResponse.builder()
                    .name(name)
                    .quantity(qty)
                    .revenue(revenue)
                    .build());
        }
        return result;
    }

    // 오늘 기준 최근 거래 내역 (최대 20건)
    @Transactional(readOnly = true)
    public List<RecentTransactionResponse> getTodayRecentTransactions(Long storeId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<SalesTransaction> txs = salesTransactionRepository.findRecentTransactions(storeId, start, end);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        List<RecentTransactionResponse> result = new ArrayList<>();
        int limit = Math.min(20, txs.size());

        for (int i = 0; i < limit; i++) {
            SalesTransaction tx = txs.get(i);

            String timeStr = tx.getTransactionTime() != null
                    ? tx.getTransactionTime().format(timeFmt)
                    : "";

            String itemsSummary = "";
            if (tx.getLineItems() != null && !tx.getLineItems().isEmpty()) {
                itemsSummary = tx.getLineItems().stream()
                        .map(li -> li.getMenuItem().getMenuName() + " x " + li.getQuantity())
                        .collect(Collectors.joining(", "));
            }

            result.add(RecentTransactionResponse.builder()
                    .id(tx.getTransactionId())
                    .time(timeStr)
                    .items(itemsSummary)
                    .amount(tx.getTotalAmount())
                    .build());
        }

        return result;
    }

    /**
     * 📌 매출 현황(그래프)용: period 기준(from~today)으로 한번에 가져오기
     * - FRONT에서 /owner/sales/daily?storeId=&from=&to=&period= 로 부르는다면
     * Controller에서 이 메서드를 사용하면 됨
     */
    @Transactional(readOnly = true)
    public List<SalesDailyStatResponse> getStatsByPeriod(Long storeId, String period) {
        LocalDate today = LocalDate.now();
        String norm = (period == null) ? "DAY" : period.toUpperCase(Locale.ROOT);

        // ✅ MONTH: 카드와 동일하게 sumSales 기준 (최근 3개월)
        if ("MONTH".equals(norm)) {
            List<SalesDailyStatResponse> result = new ArrayList<>();

            YearMonth thisMonth = YearMonth.from(today);
            YearMonth startMonth = thisMonth.minusMonths(2); // 최근 3개월: -2, -1, 0

            DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");

            for (int i = 0; i < 3; i++) {
                YearMonth ym = startMonth.plusMonths(i);
                LocalDate start = ym.atDay(1);
                // 이번 달은 오늘까지만, 지난 달/지지난 달은 해당 월 말일까지
                LocalDate end = ym.equals(thisMonth) ? today : ym.atEndOfMonth();

                BigDecimal total = sumSales(storeId, start, end);
                result.add(new SalesDailyStatResponse(ym.format(ymFmt), total));
            }

            return result;
        }

        // ✅ WEEK: 그래프도 "일요일~토요일" 기준으로, 카드와 동일하게
        if ("WEEK".equals(norm)) {
            // 이번 주 시작/끝 (일요일 ~ 토요일)
            LocalDate thisWeekStart = today
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
            LocalDate thisWeekEnd = thisWeekStart.plusDays(6); // 토요일

            // 최근 4주: 3주 전 일요일 ~ 이번 주 토요일
            LocalDate from = thisWeekStart.minusWeeks(3);
            LocalDate to = thisWeekEnd;

            return getSalesStats(storeId, from, to, "WEEK");
        }

        // ✅ DAY / YEAR 등 나머지는 기존 로직 유지
        LocalDate from;
        switch (norm) {
            case "DAY" -> from = today.minusDays(6); // 최근 7일
            case "YEAR" -> from = today.minusYears(1); // 최근 1년
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
                // 최근 7일 vs 그 직전 7일
                curTo = today;
                curFrom = today.minusDays(6);

                prevTo = curFrom.minusDays(1);
                prevFrom = prevTo.minusDays(6);
                break;
            }
            case "MONTH": {
                // 이번 달(1일~오늘) vs 지난 달 전체
                curFrom = today.withDayOfMonth(1);
                curTo = today;

                LocalDate lastMonth = curFrom.minusMonths(1);
                prevFrom = lastMonth.withDayOfMonth(1);
                prevTo = curFrom.minusDays(1); // 지난 달 말일
                break;
            }
            case "YEAR": {
                // 올해(1/1~오늘) vs 작년 전체
                curFrom = today.withDayOfYear(1);
                curTo = today;

                LocalDate lastYear = curFrom.minusYears(1);
                prevFrom = lastYear.withDayOfYear(1);
                prevTo = curFrom.minusDays(1); // 작년 12/31
                break;
            }
            case "DAY":
            default: {
                // 오늘 vs 어제
                curFrom = today;
                curTo = today;

                prevFrom = today.minusDays(1);
                prevTo = prevFrom;
                break;
            }
        }

        LocalDateTime curStart = curFrom.atStartOfDay();
        LocalDateTime curEnd = curTo.plusDays(1).atStartOfDay(); // [start, end)
        LocalDateTime prevStart = prevFrom.atStartOfDay();
        LocalDateTime prevEnd = prevTo.plusDays(1).atStartOfDay();

        // 1) 현재 기간 집계
        List<Object[]> curRows = salesLineItemRepository.findMenuAggBetween(storeId, curStart, curEnd);

        // 2) 이전 기간 집계
        List<Object[]> prevRows = salesLineItemRepository.findMenuAggBetween(storeId, prevStart, prevEnd);

        // 3) 이전 기간 매출 Map(menuId -> revenue)
        Map<Long, BigDecimal> prevRevenueMap = new HashMap<>();
        for (Object[] row : prevRows) {
            Long menuId = (Long) row[0];
            BigDecimal revenue = (BigDecimal) row[3];
            if (revenue == null)
                revenue = BigDecimal.ZERO;
            prevRevenueMap.put(menuId, revenue);
        }

        // 4) 현재 기간 기준으로 증감률 계산
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

            result.add(
                    TopMenuStatsResponse.builder()
                            .menuId(menuId)
                            .name(name)
                            .quantity(quantity)
                            .revenue(revenue)
                            .growth(growth)
                            .build());
        }

        // 매출액 기준 내림차순 정렬 후 TOP 5만
        result.sort(Comparator.comparing(TopMenuStatsResponse::getRevenue).reversed());
        return result.size() > 5 ? result.subList(0, 5) : result;
    }

    @Transactional(readOnly = true)
    public List<RecentTransactionResponse> getRecentTransactions(Long storeId) {
        List<SalesTransaction> txList = salesTransactionRepository
                .findTop20ByStoreStoreIdOrderByTransactionTimeDesc(storeId);

        List<RecentTransactionResponse> result = new ArrayList<>();

        for (SalesTransaction tx : txList) {
            String items = "";
            if (tx.getLineItems() != null && !tx.getLineItems().isEmpty()) {
                items = tx.getLineItems().stream()
                        .map(li -> li.getMenuItem().getMenuName() + " x " + li.getQuantity())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
            }

            String timeLabel = "";
            if (tx.getTransactionTime() != null) {
                timeLabel = tx.getTransactionTime().format(TX_TIME_FMT);
            }

            result.add(new RecentTransactionResponse(
                    tx.getTransactionId(),
                    timeLabel,
                    items,
                    tx.getTotalAmount()));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<SalesTransactionSummaryResponse> getTransactionsByRange(
            Long storeId,
            LocalDate from,
            LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // to 날짜의 23:59:59까지 포함

        List<SalesTransaction> list = salesTransactionRepository
                .findByStoreStoreIdAndTransactionTimeBetweenOrderByTransactionTimeDesc(
                        storeId, start, end);

        return list.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    private SalesTransactionSummaryResponse toSummaryDTO(SalesTransaction tx) {
        // 메뉴명 x 수량 한 줄로 요약
        String itemsSummary = "";
        if (tx.getLineItems() != null && !tx.getLineItems().isEmpty()) {
            itemsSummary = tx.getLineItems().stream()
                    .map(li -> li.getMenuItem().getMenuName() + " x " + li.getQuantity())
                    .collect(Collectors.joining(", "));
        }

        String timeLabel = "";
        if (tx.getTransactionTime() != null) {
            timeLabel = tx.getTransactionTime().format(TX_TIME_FMT); // ⭐ 예: 2025-11-21 16:20
        }

        return new SalesTransactionSummaryResponse(
                tx.getTransactionId(),
                timeLabel,
                tx.getPaymentMethod(),
                tx.getStatus(),
                tx.getTotalAmount(),
                tx.getTotalDiscount(),
                itemsSummary);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(Long storeId) {

        LocalDate today = LocalDate.now();

        // ===== 오늘 / 어제 =====
        BigDecimal todaySales = sumSales(storeId, today, today);
        BigDecimal yesterdaySales = sumSales(storeId, today.minusDays(1), today.minusDays(1));
        BigDecimal todayRate = calcChangeRate(todaySales, yesterdaySales);

        // ===== 이번 주 / 지난 주 ===== (그래프 WEEK와 동일: 일요일~토요일)
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate thisWeekEnd = thisWeekStart.plusDays(6); // 토요일

        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);
        LocalDate lastWeekStart = lastWeekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        BigDecimal thisWeekSales = sumSales(storeId, thisWeekStart, thisWeekEnd);
        BigDecimal lastWeekSales = sumSales(storeId, lastWeekStart, lastWeekEnd);
        BigDecimal weekRate = calcChangeRate(thisWeekSales, lastWeekSales);

        // ===== 이번 달 / 지난 달 ===== ✅ 카드 & 그래프 둘 다 이 기준 사용
        YearMonth ym = YearMonth.from(today);
        LocalDate thisMonthStart = ym.atDay(1);
        LocalDate lastMonthStart = ym.minusMonths(1).atDay(1);
        LocalDate lastMonthEnd = thisMonthStart.minusDays(1);

        BigDecimal thisMonthSales = sumSales(storeId, thisMonthStart, today);
        BigDecimal lastMonthSales = sumSales(storeId, lastMonthStart, lastMonthEnd);
        BigDecimal monthRate = calcChangeRate(thisMonthSales, lastMonthSales);

        // ===== 평균 객단가 (이번 달 vs 지난 달) =====
        BigDecimal thisMonthAvg = calcAvgTicket(storeId, thisMonthStart, today);
        BigDecimal lastMonthAvg = calcAvgTicket(storeId, lastMonthStart, lastMonthEnd);
        BigDecimal avgRate = calcChangeRate(thisMonthAvg, lastMonthAvg);

        return SalesSummaryResponse.builder()
                .todaySales(todaySales)
                .todaySalesChangeRate(todayRate)
                .weekSales(thisWeekSales)
                .weekSalesChangeRate(weekRate)
                .monthSales(thisMonthSales)
                .monthSalesChangeRate(monthRate)
                .avgTicket(thisMonthAvg)
                .avgTicketChangeRate(avgRate)
                .build();
    }

    private PosOrderResponse toDTO(SalesTransaction tx) {
        List<PosOrderResponse.LineSummary> lineSummaries = new ArrayList<>();
        if (tx.getLineItems() != null) {
            for (SalesLineItem line : tx.getLineItems()) {
                lineSummaries.add(PosOrderResponse.LineSummary.builder()
                        .lineId(line.getLineId())
                        .menuId(line.getMenuItem().getMenuId())
                        .menuName(line.getMenuItem().getMenuName())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .lineAmount(line.getLineAmount())
                        .build());
            }
        }

        return PosOrderResponse.builder()
                .transactionId(tx.getTransactionId())
                .storeId(tx.getStore().getStoreId())
                .transactionTime(tx.getTransactionTime())
                .totalAmount(tx.getTotalAmount())
                .totalDiscount(tx.getTotalDiscount())
                .status(tx.getStatus())
                .paymentMethod(tx.getPaymentMethod())
                .lines(lineSummaries)
                .build();
    }

    /** from~to (LocalDate, 포함 범위) 사이 매출 합계 */
    private BigDecimal sumSales(Long storeId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // 끝나는 날 다음날 00:00 까지
        BigDecimal v = salesTransactionRepository
                .sumTotalAmountByStoreIdBetween(storeId, start, end);
        return v != null ? v : BigDecimal.ZERO;
    }

    /** 증감률 = (current - prev) / prev * 100 (%) */
    private BigDecimal calcChangeRate(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 1, RoundingMode.HALF_UP); // 소수 1자리
    }

    /** 객단가 = 총 매출 / 거래 건수 */
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
