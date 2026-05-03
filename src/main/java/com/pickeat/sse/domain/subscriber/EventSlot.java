package com.pickeat.sse.domain.subscriber;

import com.pickeat.sse.domain.event.PickeatEvent;
import java.util.concurrent.atomic.AtomicReference;

public class EventSlot {

    private final AtomicReference<PickeatEvent> slot = new AtomicReference<>();

    public void replaceWithLatest(PickeatEvent newEvent) {
        slot.updateAndGet(currentEvent -> {
            if (currentEvent == null || newEvent.getSequence() > currentEvent.getSequence()) {
                return newEvent;
            }
            return currentEvent;
        });
    }

    public PickeatEvent getAndClear() {
        return slot.getAndSet(null);
    }

    public boolean isEmpty() {
        return slot.get() == null;
    }
}
