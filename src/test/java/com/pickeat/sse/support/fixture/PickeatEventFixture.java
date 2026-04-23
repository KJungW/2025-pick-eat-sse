package com.pickeat.sse.support.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickeat.sse.domain.event.EventMeta;
import com.pickeat.sse.domain.event.PickeatEvent;

public class PickeatEventFixture {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static PickeatEvent createEvent(String pickeatCode, String content) {
        EventMeta eventMeta = new EventMeta("GROUP_1", 1L, "ACTION_1", pickeatCode, System.currentTimeMillis());
        return new PickeatEvent(eventMeta, objectMapper.createObjectNode().put("message", content));
    }

    public static PickeatEvent createEvent(String pickeatCode, Long sequence, String content) {
        EventMeta eventMeta = new EventMeta("GROUP_1", 1L, "ACTION_1", pickeatCode, System.currentTimeMillis());
        return new PickeatEvent(eventMeta, objectMapper.createObjectNode().put("message", content));
    }
}
