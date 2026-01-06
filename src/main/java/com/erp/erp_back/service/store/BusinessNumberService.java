// src/main/java/com/erp/erp_back/service/store/BusinessNumberService.java
package com.erp.erp_back.service.store;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.infra.nts.NtsOpenApiClient;
import com.erp.erp_back.infra.nts.dto.NtsStatusItem;
import com.erp.erp_back.infra.nts.dto.NtsStatusResponse;
import com.erp.erp_back.mapper.BusinessNumberMapper;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BusinessNumberService {

    private final NtsOpenApiClient ntsClient;
    private final BusinessNumberRepository businessRepo;
    private final OwnerRepository ownerRepository;
    private final BusinessNumberMapper businessNumberMapper;

    private final Long defaultOwnerId = 1L;

    /** 숫자만 남기기(하이픈/공백 제거) */
    private String norm(String input) {
        return input == null ? "" : input.replaceAll("[^0-9]", "");
    }

    public BusinessNumber verifyAndSave(String rawBizNo, String rawPhone) {
        String bno = norm(rawBizNo);
        if (bno.length() != 10) {
            throw new IllegalArgumentException("사업자번호 형식이 올바르지 않습니다. (숫자 10자리)");
        }

        String phone = norm(rawPhone);
        if (phone.isBlank()) {
            throw new IllegalArgumentException("전화번호는 필수 입력입니다.");
        }

        // 1) 중복 체크
        Optional<BusinessNumber> existing = businessRepo.findByBizNum(bno);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다.");
        }

        // 2) NTS 호출 (4xx/5xx 모두 잡고, 실패 시 저장 자체를 막는다)
        NtsStatusResponse res;
        try {
            res = ntsClient.status(List.of(bno));
        } catch (HttpStatusCodeException e) {
            log.warn("NTS 호출 실패 bizNo={}, status={}, body={}",
                    bno, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("국세청 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } catch (RuntimeException e) {
            // client에서 IllegalStateException으로 감쌀 수도 있으니 여기서도 잡아 로그 남김
            log.warn("NTS 호출 실패(런타임) bizNo={}, msg={}", bno, e.getMessage(), e);
            throw new IllegalArgumentException("국세청 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        if (res == null || res.data() == null || res.data().isEmpty()) {
            throw new IllegalStateException("국세청 응답이 비정상입니다. (data 비어있음)");
        }

        NtsStatusItem item = res.data().get(0);

        // ✅ 원인 확정 로그 3줄
        log.info("NTS raw response: status_code={}, dataSize={}",
                res.statusCode(), (res.data() == null ? -1 : res.data().size()));
        log.info("NTS item fields: b_no={}, b_stt={}, tax_type={}, end_dt={}",
                item.bNo(), item.bStt(), item.taxType(), item.endDt());

        String stt = item.bStt();
        String taxType = item.taxType();

        // 3) 유효성 검증 (end_dt는 계속사업자면 null 정상)
        if (stt == null || stt.isBlank() || stt.contains("등록되지")) {
            throw new IllegalArgumentException("유효하지 않은 사업자번호입니다.");
        }
        if (stt.contains("폐업")) {
            throw new IllegalArgumentException("폐업 상태의 사업자번호는 인증 및 등록할 수 없습니다.");
        }

        // ✅ open_status/tax_type가 비어있으면 "인증 성공"으로 저장하지 않는다 (NULL row 방지)
        if (taxType == null || taxType.isBlank()) {
            throw new IllegalStateException("국세청 응답 tax_type 누락으로 등록할 수 없습니다.");
        }

        // 4) Owner 조회
        Owner defaultOwner = ownerRepository.findById(defaultOwnerId)
                .orElseThrow(() -> new IllegalStateException("기본 Owner가 존재하지 않습니다. (owner_id=1)"));

        // 5) 저장 (여기서부터는 필드가 정상인 경우만 도달)
        String openStatus = item.bStt();
        String endDt = (item.endDt() == null || item.endDt().isBlank()) ? null : item.endDt();

        BusinessNumber bn = businessNumberMapper.toEntity(
                bno,
                phone,
                openStatus,
                taxType,
                endDt,
                defaultOwner);

        log.info("Mapped entity: bizNum={}, openStatus={}, taxType={}, endDt={}",
                bn.getBizNum(), bn.getOpenStatus(), bn.getTaxType(), bn.getEndDt());

        return businessRepo.save(bn);
    }

    @Transactional(readOnly = true)
    public List<BusinessNumber> list() {
        return businessRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<BusinessNumber> listActiveByOwner(Long ownerId) {
        return businessRepo.findByOwner_OwnerId(ownerId).stream()
                .filter(bn -> !"폐업자".equals(bn.getOpenStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessNumber> listActiveForDefaultOwner() {
        return listActiveByOwner(defaultOwnerId);
    }
}
