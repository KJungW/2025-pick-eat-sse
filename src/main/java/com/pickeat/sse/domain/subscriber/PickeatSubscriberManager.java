package com.pickeat.sse.domain.subscriber;

import com.pickeat.sse.domain.event.PickeatEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PickeatSubscriberManager {

    private final Map<String, Map<String, PickeatSubscriber>> subscriberStorage = new ConcurrentHashMap<>();
    private final Executor taskExecutor;

    public PickeatSubscriberManager(@Qualifier("virtualThreadExecutor") Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

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

    public void broadcastEvent(String pickeatCode, PickeatEvent event) {
        Map<String, PickeatSubscriber> subscribers = subscriberStorage.get(pickeatCode);
        if (subscribers == null) {
            return;
        }

        subscribers.forEach((participantCode, subscriber) -> {
            subscriber.registerEvent(event);
            processSendEvent(pickeatCode, participantCode, subscriber);
        });
    }

    public void broadcastHeartbeat() {
        for (Map<String, PickeatSubscriber> group : subscriberStorage.values()) {
            for (PickeatSubscriber subscriber : group.values()) {
                processSendHeartBeat(subscriber);
            }
        }
    }

    public int getSubscriberCountInPickeat(String pickeatCode) {
        return Optional.ofNullable(subscriberStorage.get(pickeatCode))
                .map(Map::size)
                .orElse(0);
    }

    private void processSendEvent(String pickeatCode, String participantCode, PickeatSubscriber subscriber) {
        if (subscriber.isNowSending()) {
            return; // 이미 이벤트를 전송 중이면 종료
        }

        taskExecutor.execute(() -> {
            try {
                subscriber.sendAllEventsUntilEmpty();
                log.debug("전송 사이클 완료: [참가자: {}]", participantCode);
            } catch (IOException | IllegalStateException e) {
                log.warn("전송 실패로 인한 커넥션 드롭: [참가자: {}]", participantCode);
                remove(pickeatCode, participantCode);
            }
        });
    }

    private void processSendHeartBeat(PickeatSubscriber subscriber) {
        if (subscriber.isNowSending()) {
            return; // 이미 이벤트를 전송 중이면 종료
        }

        taskExecutor.execute(() -> {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(5000)); // 부하 분산을 위한 지터 적용
                subscriber.sendHeartbeat();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException | IllegalStateException e) {
                log.warn("하트 비트 실패로 인한 커넥션 드롭: [참가자: {}]", subscriber.getParticipantCode());
                remove(subscriber.getPickeatCode(), subscriber.getParticipantCode());
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
