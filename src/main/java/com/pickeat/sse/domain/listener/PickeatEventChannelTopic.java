package com.pickeat.sse.domain.listener;

import lombok.Getter;

@Getter
public enum PickeatEventChannelTopic {

    PICKEAT_EVENT_TOPIC("pickeat-event-topic"),
    PARTICIPANT_EVENT_TOPIC("participant-event-topic"),
    RESTAURANT_EVENT_TOPIC("restaurant-event-topic");

    private final String value;

    PickeatEventChannelTopic(String value) {
        this.value = value;
    }
}
