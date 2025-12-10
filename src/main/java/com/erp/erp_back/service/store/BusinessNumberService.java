package com.erp.erp_back.service.store;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

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

        // 1. DB에서 먼저 조회 및 중복 체크
        Optional<BusinessNumber> existing = businessRepo.findByBizNum(bno);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다.");
        }

        // 2. 외부 API 호출
        NtsStatusResponse res;
        try {
            res = ntsClient.status(List.of(bno));
        } catch (HttpClientErrorException e) {
            log.warn("국세청 사업자 상태 조회 실패. bizNo={}, status={}, body={}",
                    bno, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("국세청에서 사업자번호를 확인할 수 없습니다. 번호를 다시 확인해주세요.");
        }

        if (res == null || res.data() == null || res.data().isEmpty()) {
            throw new IllegalStateException("국세청 응답이 비정상입니다.");
        }

        NtsStatusItem item = res.data().get(0);

        String stt = item.bStt();
        if (stt == null || stt.isBlank() || stt.contains("등록되지")) {
            throw new IllegalArgumentException("유효하지 않은 사업자번호입니다.");
        }

        if (stt.contains("폐업")) {
            throw new IllegalArgumentException("폐업 상태의 사업자번호는 인증 및 등록할 수 없습니다.");
        }

        // 3. Owner 조회
        Owner defaultOwner = ownerRepository.findById(defaultOwnerId)
                .orElseThrow(() -> new IllegalStateException("기본 Owner가 존재하지 않습니다. (owner_id=1)"));

        // 4. Mapper를 사용하여 Entity 생성 (여기서 정제된 phone이 들어감)
        BusinessNumber bn = businessNumberMapper.toEntity(bno, phone, item, defaultOwner);

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