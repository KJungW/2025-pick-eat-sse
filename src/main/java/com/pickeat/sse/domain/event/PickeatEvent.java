package com.pickeat.sse.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class PickeatEvent {

    private final EventMeta meta;
    private final JsonNode content;

    @JsonCreator
    public PickeatEvent(
            @JsonProperty("meta") EventMeta meta,
            @JsonProperty("content") JsonNode content) {
        this.meta = meta;
        this.content = content;
    }
}
