package com.pickeat.sse.controller;

import com.pickeat.sse.domain.event.EventType;
import com.pickeat.sse.domain.subscriber.PickeatSubscriberManager;
import com.pickeat.sse.global.auth.participant.ParticipantInPickeat;
import com.pickeat.sse.global.auth.participant.ParticipantPrincipal;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class PickeatController {

    private final PickeatSubscriberManager subscriberManager;

    @GetMapping(value = "/pickeats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @ParticipantInPickeat ParticipantPrincipal principal
    ) {
        String pickeatCode = principal.pickeatCode();
        String participantCode = principal.participantCode();
        SseEmitter emitter = makeSseEmitter(pickeatCode, participantCode);
        subscriberManager.register(pickeatCode, participantCode, emitter);
        return emitter;
    }

    @DeleteMapping(value = "/pickeats")
    public void disconnect(
            @ParticipantInPickeat ParticipantPrincipal principal
    ) {
        String participantCode = principal.participantCode();
        subscriberManager.remove(principal.pickeatCode(), principal.participantCode());
    }

    private SseEmitter makeSseEmitter(String pickeatCode, String participantCode) {
        SseEmitter emitter = new SseEmitter(1800000L);
        emitter.onCompletion(() -> subscriberManager.remove(pickeatCode, participantCode));
        emitter.onTimeout(() -> subscriberManager.remove(pickeatCode, participantCode));
        emitter.onError((e) -> subscriberManager.remove(pickeatCode, participantCode));

        try {
            emitter.send(SseEmitter.event()
                    .name(EventType.INIT.toString())
                    .data("Connected!")
                    .reconnectTime(3000));
        } catch (IOException e) {
            emitter.complete();
            subscriberManager.remove(pickeatCode, participantCode);
        }

        return emitter;
    }
}
