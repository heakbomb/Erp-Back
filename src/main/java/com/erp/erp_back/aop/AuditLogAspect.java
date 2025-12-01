package com.erp.erp_back.aop;

import com.erp.erp_back.annotation.LogAudit;
import com.erp.erp_back.entity.erp.Inventory;
import com.erp.erp_back.entity.erp.MenuItem;
import com.erp.erp_back.entity.erp.PurchaseHistory;
import com.erp.erp_back.entity.erp.RecipeIngredient;
import com.erp.erp_back.entity.log.AuditLog;
import com.erp.erp_back.entity.store.Store;
import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.mapper.EmployeeMapper;
import com.erp.erp_back.mapper.InventoryMapper;
import com.erp.erp_back.mapper.MenuItemMapper;
import com.erp.erp_back.mapper.PurchaseHistoryMapper;
import com.erp.erp_back.mapper.RecipeIngredientMapper;
import com.erp.erp_back.mapper.StoreMapper; 
import com.erp.erp_back.repository.log.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ApplicationContext context;
    private final ObjectMapper objectMapper;
    
    private final StoreMapper storeMapper; 
    private final EmployeeMapper employeeMapper;
    private final InventoryMapper inventoryMapper;
    private final MenuItemMapper menuItemMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final PurchaseHistoryMapper purchaseHistoryMapper;

    @Around("@annotation(com.erp.erp_back.annotation.LogAudit)")
    public Object handleAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        LogAudit annotation = signature.getMethod().getAnnotation(LogAudit.class);
        
        String action = annotation.action();
        String target = annotation.target();
        int idIndex = annotation.idIndex(); 
        Object[] args = joinPoint.getArgs();
        
        // 1. Before 데이터 조회
        Object beforeData = null; 
        Long targetId = null;

        try {
            if (args.length > 0 && args[0] instanceof Long) {
                targetId = (Long) args[0];
                
                String repositoryName = Character.toLowerCase(target.charAt(0)) + target.substring(1) + "Repository";
                
                if (context.containsBean(repositoryName)) {
                    CrudRepository<?, Long> repo = (CrudRepository<?, Long>) context.getBean(repositoryName);
                    Object entity = repo.findById(targetId).orElse(null);

                    if (entity != null) {
                        // 🔥 [추가 2] Entity를 안전한 DTO로 변환
                        beforeData = convertToDto(target, entity);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Before data capture failed", e);
        }

        // 2. 실제 비즈니스 로직 실행
        Object result = joinPoint.proceed(); 

        // 3. 로그 저장
        try {
            saveAuditLog(action, target, targetId, beforeData, result);
        } catch (Exception e) {
            log.error("Audit log save failed", e);
        }

        return result;
    }

    /**
     * 🧩 Entity -> DTO 변환기
     * (여기서 Mapper를 사용합니다)
     */
    private Object convertToDto(String target, Object entity) {
        if (entity == null) return null;

        // 1. 사업장 (기존 코드)
        if ("Store".equals(target) && entity instanceof Store) {
            return storeMapper.toResponse((Store) entity);
        }
        
        // 2. 직원 (Employee) 처리 로직
        else if ("Employee".equals(target) && entity instanceof Employee) {
            // EmployeeMapper의 toResponse를 사용하여 깔끔한 DTO로 변환
            return employeeMapper.toResponse((Employee) entity);
        }
        // 3. 재고 변환 로직
        else if ("Inventory".equals(target) && entity instanceof Inventory) {
            return inventoryMapper.toResponse((Inventory) entity);
        }
        // 4. 메뉴 (MenuItem)
        else if ("MenuItem".equals(target) && entity instanceof MenuItem) {
            return menuItemMapper.toResponse((MenuItem) entity);
        }
        
        // 5. 레시피 (RecipeIngredient)
        else if ("RecipeIngredient".equals(target) && entity instanceof RecipeIngredient) {
            return recipeIngredientMapper.toResponse((RecipeIngredient) entity);
        }
        // 6. 매입 기록 (PurchaseHistory)
        else if ("PurchaseHistory".equals(target) && entity instanceof PurchaseHistory) {
            return purchaseHistoryMapper.toResponse((PurchaseHistory) entity);
        }

        return entity;
    }

    private void saveAuditLog(String action, String target, Long targetId, Object before, Object after) {
        AuditLog log = new AuditLog();
        log.setActionType(action);
        log.setTargetTable(target);
        log.setCreatedAt(LocalDateTime.now());
        log.setUserId(1L); 
        log.setUserType("SYSTEM"); 

        Map<String, Object> changes = new HashMap<>();
        changes.put("targetId", targetId);
        
        // 이제 before도 DTO(StoreResponse)이므로 안심하고 저장!
        changes.put("before", before); 
        changes.put("after", after);

        try {
            log.setChanges(objectMapper.writeValueAsString(changes));
        } catch (Exception e) {
            log.setChanges("{\"error\": \"JSON parsing error\"}");
        }

        auditLogRepository.save(log);
    }
}