package com.pickeat.sse.domain.subscriber;

import com.pickeat.sse.domain.event.PickeatEvent;
import java.util.concurrent.atomic.AtomicReference;

public class EventSlot {

    private final AtomicReference<PickeatEvent> slot = new AtomicReference<>();

    public void update(PickeatEvent event) {
        slot.set(event);
    }

    public PickeatEvent getAndClear() {
        return slot.getAndSet(null);
    }

    public boolean isEmpty() {
        return slot.get() == null;
    }
}
