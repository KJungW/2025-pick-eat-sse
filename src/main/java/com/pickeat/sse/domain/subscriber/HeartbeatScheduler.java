package com.pickeat.sse.domain.subscriber;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {

    private static final Long SSE_HEARTBEAT_INTERVAL_MILLIS = 45000L;

    private final PickeatSubscriberManager subscriberManager;

    @Scheduled(fixedDelay = SSE_HEARTBEAT_INTERVAL_MILLIS)
    public void sendHeartbeat() {
        subscriberManager.broadcastHeartbeat();
    }
}
