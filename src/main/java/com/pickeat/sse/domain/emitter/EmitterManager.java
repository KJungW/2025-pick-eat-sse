package com.pickeat.sse.domain.emitter;

import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.event.PickeatEvent;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmitterManager {

    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(String pickeatCode, String participantCode, SseEmitter emitter) {
        Map<String, SseEmitter> participants = emitters.computeIfAbsent(pickeatCode, k -> new ConcurrentHashMap<>());

        SseEmitter oldEmitter = participants.put(participantCode, emitter);
        if (oldEmitter != null) {
            completeEmitter(oldEmitter);
        }

        log.info("참가자 등록 완료: [픽잇: {}] [참가자: {}] (현재 방 접속자: {}명)",
                pickeatCode, participantCode, participants.size());
    }

    public void remove(String pickeatCode, String participantCode) {
        Map<String, SseEmitter> participants = emitters.get(pickeatCode);
        if (participants == null) {
            return;
        }

        SseEmitter emitter = participants.remove(participantCode);
        if (emitter != null) {
            log.info("참가자 제거 완료: [픽잇: {}] [참가자: {}] (현재 방 접속자: {}명)",
                    pickeatCode, participantCode, participants.size());
            completeEmitter(emitter);
        }

        removeEmptyParticipants(participants, pickeatCode);
    }

    public void broadcast(String pickeatCode, PickeatEvent event) {
        Map<String, SseEmitter> participants = emitters.get(pickeatCode);

        if (participants == null || participants.isEmpty()) {
            return;
        }

        participants.forEach((participantCode, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(EventType.BUSINESS.toString())
                        .data(event));
            } catch (IOException e) {
                log.warn("메세지 전송 실패: [픽잇: {}] [참가자: {}]", pickeatCode, participantCode);
                remove(pickeatCode, participantCode);
            }
        });
    }

    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("Emitter 종료 중 오류 발생: {}", e.getMessage());
        }
    }

    private void removeEmptyParticipants(Map<String, SseEmitter> participants, String pickeatCode) {
        if (participants.isEmpty()) {
            emitters.remove(pickeatCode);
            log.info("비어있는 픽잇 정리 완료: [픽잇: {}]", pickeatCode);
        }
    }
}
