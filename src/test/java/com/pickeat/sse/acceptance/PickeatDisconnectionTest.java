package com.pickeat.sse.acceptance;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.subscriber.PickeatSubscriberManager;
import com.pickeat.sse.support.AcceptanceTest;
import com.pickeat.sse.support.utility.SseUtility;
import com.pickeat.sse.support.utility.TokenUtility;
import io.restassured.RestAssured;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class PickeatDisconnectionTest extends AcceptanceTest {

    @Autowired
    private PickeatSubscriberManager subscriberManager;

    @Autowired
    private TokenUtility tokenUtility;

    @Autowired
    private SseUtility sseUtility;

    @Test
    void 참가자가_픽잇_SSE에_대한_연결을_해제한다() throws IOException {
        // given
        String pickeatCode = "PICKEAT_1";
        String participantCode = "PARTICIPANT_1";
        String token = tokenUtility.createToken(participantCode, pickeatCode);

        HttpURLConnection conn = sseUtility.subscribe(getPort(), token);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            // 구독 연결 확인
            assertThat(reader.readLine()).contains("event:" + EventType.INIT);
            assertThat(subscriberManager.getSubscriberCountInPickeat(pickeatCode)).isEqualTo(1);

            // when
            RestAssured.given().log().ifValidationFails()
                    .header("Pickeat-Participant-Token", "Bearer " + token)
                    .when()
                    .delete("/api/v1/pickeats")
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // then
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            assertThat(subscriberManager.getSubscriberCountInPickeat(pickeatCode)).isZero()
                    );

        } finally {
            conn.disconnect();
        }
    }
}
