package com.pickeat.sse.controller;

import com.pickeat.sse.domain.EmitterFactory;
import com.pickeat.sse.domain.subscriber.PickeatSubscriber;
import com.pickeat.sse.domain.subscriber.PickeatSubscriberManager;
import com.pickeat.sse.global.auth.participant.ParticipantInPickeat;
import com.pickeat.sse.global.auth.participant.ParticipantPrincipal;
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
    private final EmitterFactory emitterFactory;

    @GetMapping(value = "/pickeats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @ParticipantInPickeat ParticipantPrincipal principal
    ) {
        String pickeatCode = principal.pickeatCode();
        String participantCode = principal.participantCode();

        SseEmitter emitter = createEmitter(pickeatCode, participantCode);
        PickeatSubscriber subscriber = new PickeatSubscriber(pickeatCode, participantCode, emitter);
        subscriberManager.register(subscriber);

        return emitter;
    }

    @DeleteMapping(value = "/pickeats")
    public void disconnect(
            @ParticipantInPickeat ParticipantPrincipal principal
    ) {
        subscriberManager.remove(principal.pickeatCode(), principal.participantCode());
    }

    private SseEmitter createEmitter(String pickeatCode, String participantCode) {
        return emitterFactory.create(() ->
                subscriberManager.remove(pickeatCode, participantCode)
        );
    }
}
