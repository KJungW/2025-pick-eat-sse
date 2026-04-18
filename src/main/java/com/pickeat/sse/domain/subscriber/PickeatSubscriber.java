package com.pickeat.sse.domain.subscriber;

import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.event.PickeatEvent;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
public class PickeatSubscriber {

    private final String pickeatCode;
    private final String participantCode;
    private final SseEmitter emitter;
    private final Map<String, EventSlot> eventSlots = new ConcurrentHashMap<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);

    public PickeatSubscriber(String pickeatCode, String participantCode, SseEmitter emitter) {
        this.pickeatCode = pickeatCode;
        this.participantCode = participantCode;
        this.emitter = emitter;
    }

    public void registerEvent(PickeatEvent event) {
        String eventGroup = event.getEventGroup();
        eventSlots.computeIfAbsent(eventGroup, k -> new EventSlot()).update(event);
    }

    public void sendAllEventsUntilEmpty() throws IOException {
        if (!isSending.compareAndSet(false, true)) { // 이미 이벤트를 전송 중이면 바로 종료
            return;
        }

        try {
            // 모든 슬롯이 비워질 때까지 각 슬롯에 담긴 최신 이벤트를 전송
            while (anySlotHasEvent()) {
                for (String eventGroup : eventSlots.keySet()) {
                    EventSlot slot = eventSlots.get(eventGroup);
                    PickeatEvent eventToSend = slot.getAndClear();

                    if (eventToSend != null) {
                        emitter.send(SseEmitter.event()
                                .name(EventType.BUSINESS.toString())
                                .data(eventToSend));
                    }
                }
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

    private boolean anySlotHasEvent() {
        return eventSlots.values().stream().anyMatch(slot -> !slot.isEmpty());
    }

    public boolean isNowSending() {
        return isSending.get();
    }

    public String getPickeatCode() {
        return pickeatCode;
    }

    public String getParticipantCode() {
        return participantCode;
    }
}
