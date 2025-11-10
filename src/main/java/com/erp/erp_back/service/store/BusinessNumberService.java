package com.erp.erp_back.service.store;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;   // ✅ 추가

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.infra.nts.NtsOpenApiClient;
import com.erp.erp_back.infra.nts.dto.NtsStatusItem;
import com.erp.erp_back.infra.nts.dto.NtsStatusResponse;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;   // ✅ 추가

@Slf4j  // ✅ 경고 로그 찍으려고 추가
@Service
@RequiredArgsConstructor
@Transactional
public class BusinessNumberService {

    private final NtsOpenApiClient ntsClient;
    private final BusinessNumberRepository businessRepo;
    private final OwnerRepository ownerRepository;

    private final Long defaultOwnerId = 1L;

    /** 숫자만 남기기(하이픈/공백 제거) */
    private String norm(String bizNo) {
        return bizNo == null ? "" : bizNo.replaceAll("[^0-9]", "");
    }

    public BusinessNumber verifyAndSave(String rawBizNo, String rawPhone) {
        String bno = norm(rawBizNo);
        if (bno.length() != 10) {
            throw new IllegalArgumentException("사업자번호 형식이 올바르지 않습니다. (숫자 10자리)");
        }

        String phone = rawPhone == null ? "" : rawPhone.trim();
        if (phone.isBlank()) {
            throw new IllegalArgumentException("전화번호는 필수 입력입니다.");
        }

        // ✅ 외부 API 호출만 안전하게 감싼다
        NtsStatusResponse res;
        try {
            res = ntsClient.status(List.of(bno));
        } catch (HttpClientErrorException e) {
            // 국세청이 400을 주면 여기서만 잡고 우리식으로 응답
            log.warn("국세청 사업자 상태 조회 실패. bizNo={}, status={}, body={}",
                    bno, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("국세청에서 사업자번호를 확인할 수 없습니다. 번호를 다시 확인해주세요.");
        }

        if (res == null || res.data() == null || res.data().isEmpty()) {
            throw new IllegalStateException("국세청 응답이 비정상입니다.");
        }

        NtsStatusItem item = res.data().get(0);

        String stt = item.bStt(); // 예: "계속사업자"
        if (stt == null || stt.isBlank() || stt.contains("등록되지")) {
            throw new IllegalArgumentException("유효하지 않은 사업자번호입니다.");
        }

        // ✅ 업서트(upsert)
        BusinessNumber bn = businessRepo.findByBizNum(bno)
                .orElseGet(() -> BusinessNumber.builder().bizNum(bno).build());

        // ✅ 임시 Owner (로그인 기능 도입 전까지 기본 owner_id=1)
        Owner defaultOwner = ownerRepository.findById(defaultOwnerId)
                .orElseThrow(() -> new IllegalStateException("기본 Owner가 존재하지 않습니다. (owner_id=1)"));
        bn.setOwner(defaultOwner);

        // ✅ phone 저장
        bn.setPhone(phone);

        // ✅ 외부 API 결과 반영
        bn.setOpenStatus(item.bStt());
        bn.setTaxType(item.taxType());
        bn.setStartDt(item.startDt());
        bn.setEndDt(item.endDt());
        bn.setCertifiedAt(LocalDateTime.now());

        return businessRepo.save(bn);
    }

    @Transactional(readOnly = true)
    public List<BusinessNumber> list() {
        return businessRepo.findAll();
    }
}