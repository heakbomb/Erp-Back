package com.erp.erp_back.service.store;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.store.BusinessNumber;
import com.erp.erp_back.entity.user.Owner; // ✅ Owner import 추가 필요
import com.erp.erp_back.infra.nts.NtsOpenApiClient;
import com.erp.erp_back.infra.nts.dto.NtsStatusItem;
import com.erp.erp_back.infra.nts.dto.NtsStatusResponse;
import com.erp.erp_back.repository.store.BusinessNumberRepository;
import com.erp.erp_back.repository.user.OwnerRepository; // ✅ OwnerRepo import 추가 필요

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessNumberService {

    private final NtsOpenApiClient ntsClient;
    private final BusinessNumberRepository businessRepo;
    private final OwnerRepository ownerRepository; // ✅ 기본 Owner를 임시로 연결하기 위해 추가

    private final Long defaultOwnerId = 1L; // ✅ 로그인 붙기 전 임시 owner

    /** 숫자만 남기기(하이픈/공백 제거) */
    private String norm(String bizNo) {
        return bizNo == null ? "" : bizNo.replaceAll("[^0-9]", "");
    }

    /**
     * 사업자번호 인증 → DB 업서트
     * - 외부(OpenAPI)에서 상태 조회
     * - 정책에 맞게 유효성 판단
     * - 기존 존재 시 갱신, 없으면 신규 저장
     */
    public BusinessNumber verifyAndSave(String rawBizNo, String rawPhone) { // ✅ phone 추가됨
        String bno = norm(rawBizNo);
        if (bno.length() != 10) {
            throw new IllegalArgumentException("사업자번호 형식이 올바르지 않습니다. (숫자 10자리)");
        }

        String phone = rawPhone == null ? "" : rawPhone.trim(); // ✅ phone 처리
        if (phone.isBlank()) {
            throw new IllegalArgumentException("전화번호는 필수 입력입니다.");
        }

        NtsStatusResponse res = ntsClient.status(List.of(bno));
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

    /** 드롭다운/목록용 조회 */
    @Transactional(readOnly = true)
    public List<BusinessNumber> list() {
        return businessRepo.findAll();
    }
}