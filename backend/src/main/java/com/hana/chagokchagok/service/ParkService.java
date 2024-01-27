package com.hana.chagokchagok.service;

import com.hana.chagokchagok.dto.AllocationDto;
import com.hana.chagokchagok.dto.ValidationParkingInfoDto;
import com.hana.chagokchagok.dto.request.AllocateCarRequest;
import com.hana.chagokchagok.dto.request.ValidateAreaRequest;
import com.hana.chagokchagok.dto.response.AllocateCarResponse;
import com.hana.chagokchagok.dto.response.ValidateAreaResponse;
import com.hana.chagokchagok.entity.AllocationLog;
import com.hana.chagokchagok.entity.RealtimeParking;
import com.hana.chagokchagok.repository.AllocationLogRepository;
import com.hana.chagokchagok.repository.ParkingInfoRepository;
import com.hana.chagokchagok.repository.RealTimeParkingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkService {
    private final RealTimeParkingRepository realTimeParkingRepository;
    private final AllocationLogRepository allocationLogRepository;
    private final ParkingInfoRepository parkingInfoRepository;
    /**
     * 자리 배정 로직 수행 메소드
     * @author 김용준
     * @param allocateCarRequest 장애 여부, 차량 번호
     * @return 자리가 있다면 allocateCarResponse, 만차라면 null
     */
    public AllocateCarResponse getAllocatedInfo(AllocateCarRequest allocateCarRequest) {
        RealtimeParking allocatedLocation;
        // 장애 배려 차량 여부 확인
        if (allocateCarRequest.getIsDisabled()) {
            allocatedLocation = realTimeParkingRepository.findFirstWhoIsDisabled();
        } else {
            allocatedLocation = realTimeParkingRepository.findFirstWhoIsNotDisabled();
        }

        // 자리가 없다면 만차 응답 반환
        if (allocatedLocation == null) {
            System.out.println("자리업슴");
            return null;
        }
        // 자리 있다면 배정 로직 수행
        else {
            System.out.println("allocated location : " + allocatedLocation.getParkId());

            // 입출차기록 테이블에 저장
            AllocationDto allocationDto = new AllocationDto(allocatedLocation, allocateCarRequest.getCarNo());
            AllocationLog allocationLog = AllocationLog.createAllocationLog(allocationDto);
            allocationLogRepository.save(allocationLog);

            // 주차현황 테이블 업데이트
            allocatedLocation.setLog(allocationLog);
            realTimeParkingRepository.save(allocatedLocation);

            return new AllocateCarResponse(allocatedLocation, allocationLog);
        }
    }

    /**
     * 구역 판별 메소드
     * @author 김용준
     * @param validateAreaRequest 차량 번호, 구역 코드
     * @return 배정된 구역이라면 배정 자리 번호, 그 외에는 null
     */
    public ValidateAreaResponse validateArea(ValidateAreaRequest validateAreaRequest) {
        // 차량 번호로 배정된 구역 검색
        ValidationParkingInfoDto area = parkingInfoRepository.findValidationParkingInfo(validateAreaRequest.getCarNo());

        // 차량 번호로 검색했을 때 배정된 구역이 나오지 않음
        if (area == null) {
            return null;
        }
        // 배정된 구역이 검색됨
        else {
            // 배정된 구역에 진입했다면
            if (area.getAreaCode().equals(validateAreaRequest.getArea())) {
                return new ValidateAreaResponse(area);
            }
            // 다른 구역에 진입했다면
            else {
                return null;
            }
        }
    }
}
