package com.pickeat.sse.domain;

import com.pickeat.sse.domain.event.EventType;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class EmitterFactory {

    private static final Long CONNECTION_MILL_SEC_TIMEOUT = 1800000L;
    private static final Long RECONNECTION_MILL_SEC_TIMEOUT = 3000L;

    public SseEmitter create(Runnable onTermination) {
        SseEmitter emitter = new SseEmitter(CONNECTION_MILL_SEC_TIMEOUT);
        emitter.onCompletion(onTermination);
        emitter.onTimeout(onTermination);
        emitter.onError((e) -> onTermination.run());

        try {
            emitter.send(SseEmitter.event()
                    .name(EventType.INIT.toString())
                    .data("Connected!")
                    .reconnectTime(RECONNECTION_MILL_SEC_TIMEOUT));
        } catch (IOException e) {
            emitter.complete();
            onTermination.run();
        }

        return emitter;
    }
}
