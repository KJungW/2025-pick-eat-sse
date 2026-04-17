package com.pickeat.sse.domain.event;

public record EventMeta(
        String group,
        Long groupSequence,
        String action,
        String pickeatCode
) {

}
