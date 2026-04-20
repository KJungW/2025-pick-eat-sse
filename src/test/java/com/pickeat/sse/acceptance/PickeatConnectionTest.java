package com.pickeat.sse.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.subscriber.PickeatSubscriberManager;
import com.pickeat.sse.support.AcceptanceTest;
import com.pickeat.sse.support.utility.SseUtility;
import com.pickeat.sse.support.utility.TokenUtility;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class PickeatConnectionTest extends AcceptanceTest {

    @Autowired
    private PickeatSubscriberManager subscriberManager;

    @Autowired
    private TokenUtility tokenUtility;

    @Autowired
    private SseUtility sseUtility;

    @Test
    void 참가자가_픽잇_SSE에_연결한다() {
        // given
        String participantCode = "PARTICIPANT_1";
        String pickeatCode = "PICKEAT_1";
        String token = tokenUtility.createToken(participantCode, pickeatCode);

        // when
        Response response = RestAssured.given().log().all()
                .header("Pickeat-Participant-Token", "Bearer " + token)
                .when()
                .get("/api/v1/pickeats");

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.contentType()).contains("text/event-stream")
        );
        await()
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(subscriberManager.getSubscriberCountInPickeat(pickeatCode)).isEqualTo(1)
                );
    }

    @Test
    void 참가자가_픽잇_SSE에_연결하면_즉시_초기_더미_데이터를_수신한다() {
        // given
        String token = tokenUtility.createToken("PARTICIPANT_1", "PICKEAT_1");

        // when
        CompletableFuture<List<String>> future = sseUtility.subscribeUntil(getPort(), token, "Connected!");

        // then
        List<String> results = future.orTimeout(5, TimeUnit.SECONDS).join();

        assertThat(results)
                .anyMatch(line -> line.contains("event:" + EventType.INIT))
                .anyMatch(line -> line.contains("data:Connected!"));
    }

    @Test
    void 연결된_참가자들에게_하트비트_메시지가_정상적으로_전송된다() {
        // given
        String token = tokenUtility.createToken("PARTICIPANT_1", "PICKEAT_1");

        // when: 하트비트 데이터가 올 때까지 특정 구독 시작
        CompletableFuture<List<String>> future = sseUtility.subscribeUntil(getPort(), token, "heartbeat");

        // 구독 등록 및 안정화를 위한 대기
        sleep(1000);

        // 매니저를 통해 하트비트 강제 발송
        subscriberManager.broadcastHeartbeat();

        // then
        List<String> results = future.orTimeout(10, TimeUnit.SECONDS).join(); //지터를 고려하여 타임아웃 설정
        assertThat(results)
                .anyMatch(line -> line.contains("event:" + EventType.HEART_BEAT))
                .anyMatch(line -> line.contains("data:heartbeat"));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
