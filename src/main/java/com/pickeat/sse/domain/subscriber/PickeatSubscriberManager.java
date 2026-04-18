package com.pickeat.sse.domain.subscriber;

import com.pickeat.sse.domain.event.PickeatEvent;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PickeatSubscriberManager {

    private final Map<String, Map<String, PickeatSubscriber>> subscriberStorage = new ConcurrentHashMap<>();
    private final Executor taskExecutor;

    public void register(PickeatSubscriber subscriber) {
        Map<String, PickeatSubscriber> subscriberGroup = subscriberStorage.computeIfAbsent(
                subscriber.getPickeatCode(), k -> new ConcurrentHashMap<>());

        PickeatSubscriber oldSubscriber = subscriberGroup.put(subscriber.getParticipantCode(), subscriber);
        if (oldSubscriber != null) {
            oldSubscriber.disconnect(); // 이전에 연결된 구독이 있다면 종료시킨다
        }

        log.info("참가자 등록 완료: [픽잇: {}] [참가자: {}] (현재 방 접속자: {}명)",
                subscriber.getPickeatCode(), subscriber.getParticipantCode(), subscriberGroup.size());
    }

    public void remove(String pickeatCode, String participantCode) {
        Map<String, PickeatSubscriber> subscriberGroup = subscriberStorage.get(pickeatCode);
        if (subscriberGroup == null) {
            return;
        }

        PickeatSubscriber removedSubscriber = subscriberGroup.remove(participantCode);
        if (removedSubscriber != null) {
            log.info("참가자 제거 완료: [픽잇: {}] [참가자: {}] (현재 방 접속자: {}명)",
                    pickeatCode, participantCode, subscriberGroup.size());
            removedSubscriber.disconnect();
        }

        removeEmptySubscriberGroup(subscriberGroup, pickeatCode);
    }

    public void broadcast(String pickeatCode, PickeatEvent event) {
        Map<String, PickeatSubscriber> subscribers = subscriberStorage.get(pickeatCode);
        if (subscribers == null) {
            return;
        }

        subscribers.forEach((participantCode, subscriber) -> {
            subscriber.updateEventSlot(event);
            processSend(pickeatCode, participantCode, subscriber);
        });
    }

    private void processSend(String pickeatCode, String participantCode, PickeatSubscriber subscriber) {
        if (subscriber.isNowSending()) {
            return; // 이미 이벤트를 전송 중이면 종료
        }

        taskExecutor.execute(() -> {
            try {
                subscriber.sendEventsInSlotUntilEmpty();
                log.debug("전송 사이클 완료: [참가자: {}]", participantCode);
            } catch (IOException | IllegalStateException e) {
                log.warn("전송 실패로 인한 커넥션 드롭: [참가자: {}]", participantCode);
                remove(pickeatCode, participantCode);
            }
        });
    }

    private void removeEmptySubscriberGroup(Map<String, PickeatSubscriber> participants, String pickeatCode) {
        if (participants.isEmpty()) {
            subscriberStorage.remove(pickeatCode);
            log.info("비어있는 픽잇 정리 완료: [픽잇: {}]", pickeatCode);
        }
    }
}
