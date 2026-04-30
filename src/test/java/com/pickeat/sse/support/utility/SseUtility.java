package com.pickeat.sse.support.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class SseUtility {

    public HttpURLConnection subscribe(int port, String token) {
        try {
            URL url = new URL("http://localhost:" + port + "/api/v1/pickeats");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Pickeat-Participant-Token", "Bearer " + token);
            conn.setReadTimeout(15000);

            // 중요: getResponseCode()를 호출하여 실제 HTTP 연결을 수행하고 서버의 응답을 확인합니다.
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("SSE 연결 실패. 응답 코드: " + conn.getResponseCode());
            }

            return conn;
        } catch (IOException e) {
            throw new RuntimeException("SSE 연결 중 오류 발생", e);
        }
    }

    public CompletableFuture<List<String>> subscribeUntil(
            int port,
            String token,
            String targetContent
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> lines = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://localhost:" + port + "/api/v1/pickeats");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Pickeat-Participant-Token", "Bearer " + token);
                conn.setReadTimeout(15000);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) {
                            lines.add(line);
                            // 원하는 내용이 포함된 라인이 읽히면 지금까지 수집한 리스트 반환 후 종료
                            if (line.contains(targetContent)) {
                                return lines;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                lines.add("Error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return lines;
        });
    }
}
