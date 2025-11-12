package com.erp.erp_back.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.user.OwnerResponse;
import com.erp.erp_back.entity.user.Owner;
import com.erp.erp_back.repository.user.OwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OwnerService {

    private final OwnerRepository ownerRepository;

    /**
     * (Admin) 사장님 계정 페이징 및 검색 조회
     */
    @Transactional(readOnly = true)
    public Page<OwnerResponse> getOwnersForAdmin(String q, Pageable pageable) {
        String effectiveQuery = (q == null) ? "" : q.trim();
        
        // 1. Repository에서 Page<Owner> 조회
        Page<Owner> ownerPage = ownerRepository.findAdminOwners(effectiveQuery, pageable);
        
        // 2. Page<Owner> -> Page<OwnerResponse>로 변환
        return ownerPage.map(this::toDto);
    }

    /**
     * (Admin) 사장님 계정 단건 조회
     */
    @Transactional(readOnly = true)
    public OwnerResponse getOwnerById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사장님(ID=" + id + ")을 찾을 수 없습니다."));
        return toDto(owner);
    }

    /**
     * (Admin) 사장님 계정 삭제
     * (주의: 연결된 BusinessNumber, Store 등이 함께 삭제될 수 있으므로 정책 검토 필요)
     */
    public void deleteOwner(Long id) {
        if (!ownerRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 사장님(ID=" + id + ")을 찾을 수 없습니다.");
        }
        // 
        ownerRepository.deleteById(id);
    }

    /**
     * Entity -> DTO 변환
     */
    private OwnerResponse toDto(Owner o) {
        return OwnerResponse.builder()
                .ownerId(o.getOwnerId())
                .username(o.getUsername())
                .email(o.getEmail())
                .createdAt(o.getCreatedAt())
                .build();
    }
}