package com.erp.erp_back.service.erp;

import com.erp.erp_back.repository.erp.StoreNeighborBuildRepository;
import com.erp.erp_back.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreNeighborBuildService {

    private final StoreNeighborBuildRepository buildRepo;
    private final StoreRepository storeRepository;

    @Transactional
    public int rebuildForStore(Long storeId, int radiusM, boolean deleteBefore) {
        if (deleteBefore) {
            buildRepo.deleteNeighborsForStore(storeId, radiusM);
        }
        return buildRepo.upsertNeighborsForStore(storeId, radiusM);
    }

    /**
     * 초기 1회 전체 빌드용 (store 수가 많아지면 배치/큐로 나누는 게 더 좋음)
     */
    public long rebuildAll(int radiusM) {
        List<Long> storeIds = storeRepository.findAllStoreIds();

        long total = 0;
        for (Long storeId : storeIds) {
            // deleteBefore=false: upsert라서 중복 갱신만 됨
            total += rebuildForStore(storeId, radiusM, false);
        }
        return total;
    }
}
