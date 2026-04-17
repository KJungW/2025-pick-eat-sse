package com.pickeat.sse.domain.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickeat.sse.domain.event.PickeatEvent;
import com.pickeat.sse.domain.subscriber.PickeatSubscriberManager;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PickeatEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final PickeatSubscriberManager subscriberManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonMessage = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

            log.info("수신 채널: {}, 메시지: {}", channel, jsonMessage);

            PickeatEvent event = objectMapper.readValue(jsonMessage, PickeatEvent.class);
            subscriberManager.broadcast(event.getMeta().pickeatCode(), event);

        } catch (Exception e) {
            log.error("메시지 역직렬화 실패: {}", e.getMessage());
        }
    }

}
