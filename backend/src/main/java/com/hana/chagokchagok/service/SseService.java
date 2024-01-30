package com.hana.chagokchagok.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hana.chagokchagok.contoller.SseController;
import com.hana.chagokchagok.dto.request.CarNumRequest;
import com.hana.chagokchagok.dto.response.CommonAlertResponse;
import com.hana.chagokchagok.entity.Report;
import com.hana.chagokchagok.enums.SseStatus;
import com.hana.chagokchagok.exception.SseEmitterIsNullException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class SseService {

    public SseEmitter subscribe(String keyValue) {
        // 현재 클라이언트를 위한 SseEmitter 생성
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        try {
            sseEmitter.send(SseEmitter.event().name("OPEN").data(keyValue+"의 SSE가 연결되었습니다."));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SseController.sseEmitters.put(keyValue, sseEmitter);

        sseEmitter.onCompletion(() -> SseController.sseEmitters.remove(keyValue));
        sseEmitter.onTimeout(() -> SseController.sseEmitters.remove(keyValue));
        sseEmitter.onError((e) -> SseController.sseEmitters.remove(keyValue));

        return sseEmitter;
    }

    /**
     * 만차의 경우 차량 출차시 대기자를 위한 키오스크 요청
     * @param keyValue
     */
    public void congestionClear(String keyValue) {
        SseEmitter sseEmitter = SseController.sseEmitters.get(keyValue);
        try {
            System.out.println("=> "+sseEmitter);
            if(sseEmitter == null) throw new SseEmitterIsNullException(keyValue+" 연결이 존재하지 않음");
            else{
                sseEmitter.send(SseEmitter.event()
                        .name(String.valueOf(SseStatus.CONGESTION_CLEAR))
                        .data("["+LocalDateTime.now()+"] 차량이 출차되었습니다. 대기자에게 새 자리가 배정됩니다."));
            }

        } catch (SseEmitterIsNullException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * AI서버로부터 추출된 차번호텍스트를 SSE를 통해 키오스크로 전송
     * @param carNumRequest
     */
    public void sendCarNum(CarNumRequest carNumRequest, String keyValue) {
        SseEmitter sseEmitter = SseController.sseEmitters.get(keyValue);
        try {
            if(sseEmitter == null) throw new SseEmitterIsNullException(keyValue+" 연결이 존재하지 않음");
            else{
                sseEmitter.send(SseEmitter.event()
                        .name(String.valueOf(SseStatus.CAR_NUM))
                        .data("["+LocalDateTime.now()+"] AI서버로부터 차량번호 ["+carNumRequest.getCarNum()+"]가 입력됨"));
            }

        } catch (SseEmitterIsNullException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 자동신고시스템 가동시 플로팅알림 데이터 전송
     * @param report
     * @param keyValue
     */
    public void sendSensorReport(Report report, String keyValue) {
        SseEmitter sseEmitter = SseController.sseEmitters.get(keyValue);
        try {
            if(sseEmitter == null) throw new SseEmitterIsNullException(keyValue+" 연결이 존재하지 않음");
            else{
                Map<String, Object> data = new HashMap<>();
                data.put("time", report.getReportTime());
                data.put("park", report.getParkingInfo().getFullName());
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                String jsonData = objectMapper.writeValueAsString(data);

                sseEmitter.send(SseEmitter.event()
                        .name(String.valueOf(SseStatus.SENSOR_REPORT))
                        .data(jsonData));
            }

        } catch (SseEmitterIsNullException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 공통바 업데이터 (트리거 : 입출차, 차단바해제, 자리바꾸기, 자동신고 done)
     * @param keyValue
     */
    public void sendRealtimeCommon(String keyValue) {
        //데이터를 주지 말고... 여기서 특정 SSE요청을 하면 프론트에서 공통바 달라는 요청을 다시 보내도록 하는 방식을 차용하는게 재사용성 측면에서 좋을듯
        SseEmitter sseEmitter = SseController.sseEmitters.get(keyValue);
        try {
            if(sseEmitter == null) throw new SseEmitterIsNullException(keyValue+" 연결이 존재하지 않음");
            else{
                sseEmitter.send(SseEmitter.event()
                        .name(String.valueOf(SseStatus.REALTIME_COMMON))
                        .data("["+LocalDateTime.now()+"] 이 요청이 오면 /admin/common을 다시 실행하세요."));
            }

        } catch (SseEmitterIsNullException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}