package com.pickeat.sse.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.event.PickeatEvent;
import com.pickeat.sse.domain.listener.PickeatEventChannelTopic;
import com.pickeat.sse.support.AcceptanceTest;
import com.pickeat.sse.support.fixture.PickeatEventFixture;
import com.pickeat.sse.support.utility.SseUtility;
import com.pickeat.sse.support.utility.TokenUtility;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PickeatBroadcastTest extends AcceptanceTest {

    @Autowired
    private TokenUtility tokenUtility;

    @Autowired
    private SseUtility sseUtility;

    @Test
    void 픽잇에_이벤트가_발생하면_연결된_모든_참가자에게_브로드캐스트된다() throws JsonProcessingException {
        // given
        String pickeatCode = "PICKEAT_1";
        String participant1 = "PARTICIPANT_1";
        String participant2 = "PARTICIPANT_2";

        String token1 = tokenUtility.createToken(participant1, pickeatCode);
        String token2 = tokenUtility.createToken(participant2, pickeatCode);

        // when: 특정 메시지가 올 때까지 구독 시작
        CompletableFuture<List<String>> future1 = sseUtility.subscribeUntil(getPort(), token1, "Hello Pickeat!");
        CompletableFuture<List<String>> future2 = sseUtility.subscribeUntil(getPort(), token2, "Hello Pickeat!");

        // 구독 및 안정화를 위한 대기
        sleep(1000);

        // Redis에 이벤트 발행
        PickeatEvent event = PickeatEventFixture.createEvent(pickeatCode, "Hello Pickeat!");
        String jsonEvent = objectMapper.writeValueAsString(event);
        redisTemplate.convertAndSend(PickeatEventChannelTopic.RESTAURANT_EVENT_TOPIC.getValue(), jsonEvent);

        // then: 참가자 1, 2가 수집한 메시지 리스트에 해당 이벤트가 포함되어 있는지 검증
        List<String> result1 = future1.orTimeout(5, TimeUnit.SECONDS).join();
        List<String> result2 = future2.orTimeout(5, TimeUnit.SECONDS).join();

        assertAll(
                () -> assertThat(result1).anyMatch(line -> line.contains("event:" + EventType.BUSINESS)),
                () -> assertThat(result1).anyMatch(line -> line.contains("Hello Pickeat!")),
                () -> assertThat(result2).anyMatch(line -> line.contains("event:" + EventType.BUSINESS)),
                () -> assertThat(result2).anyMatch(line -> line.contains("Hello Pickeat!"))
        );
    }

    @Test
    void WW_전략에_따라_대기_중인_이벤트는_최신_이벤트로_대체되어_전송된다() {
        // given
        String pickeatCode = "PICKEAT_LWW";
        String token = tokenUtility.createToken("PARTICIPANT", pickeatCode);
        int messageCount = 10;

        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<List<String>> future = sseUtility.subscribeUntil(getPort(), token, "Message 9");
        sleep(1000);

        // when: 10개의 이벤트를 비동기로 동시에 발행 시도
        for (int i = 0; i < messageCount; i++) {
            int index = i;
            CompletableFuture.runAsync(() -> {
                try {
                    latch.await();
                    publishEvent(pickeatCode, (long) index, "Message " + index);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.countDown();

        // then
        List<String> results = future.orTimeout(5, TimeUnit.SECONDS).join();
        long businessEventCount = results.stream()
                .filter(line -> line.contains("event:" + EventType.BUSINESS))
                .count();
        assertAll(
                () -> assertThat(results).anyMatch(line -> line.contains("Message 9")), // 최신 메세지 전송 보장
                () -> assertThat(businessEventCount).isLessThan(messageCount) // 최신 버전이 아닌 메세지는 일부 생략됨
        );
        System.out.println(businessEventCount);
    }

    private void publishEvent(String pickeatCode, Long sequence, String message) throws JsonProcessingException {
        PickeatEvent event = PickeatEventFixture.createEvent(pickeatCode, sequence, message);
        String jsonEvent = objectMapper.writeValueAsString(event);
        redisTemplate.convertAndSend(PickeatEventChannelTopic.RESTAURANT_EVENT_TOPIC.getValue(), jsonEvent);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
