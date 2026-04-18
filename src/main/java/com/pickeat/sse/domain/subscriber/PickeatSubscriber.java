package com.pickeat.sse.domain.subscriber;

import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.event.PickeatEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
public class PickeatSubscriber {

    private final String pickeatCode;
    private final String participantCode;
    private final SseEmitter emitter;
    private final AtomicReference<PickeatEvent> slot = new AtomicReference<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);

    public PickeatSubscriber(String pickeatCode, String participantCode, SseEmitter emitter) {
        this.pickeatCode = pickeatCode;
        this.participantCode = participantCode;
        this.emitter = emitter;
    }

    public void updateEventSlot(PickeatEvent event) {
        slot.set(event);
    }

    public void sendEventsInSlotUntilEmpty() throws IOException {
        if (!isSending.compareAndSet(false, true)) {
            return; // 이미 이벤트를 전송 중이면 바로 종료
        }

        try {
            PickeatEvent eventToSend;
            // 슬롯이 비워질 때까지 슬롯에 담긴 최신 이벤트를 전송
            while ((eventToSend = slot.getAndSet(null)) != null) {
                emitter.send(SseEmitter.event()
                        .name(EventType.BUSINESS.toString())
                        .data(eventToSend));
            }
        } finally {
            isSending.set(false);
        }
    }

    public void disconnect() {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("구독 종료 중 오류 발생: {}", e.getMessage());
        }
    }

    public Boolean isNowSending() {
        return isSending.get();
    }

    public String getPickeatCode() {
        return pickeatCode;
    }

    public String getParticipantCode() {
        return participantCode;
    }
}
