package com.pickeat.sse.support.utility;

import com.pickeat.sse.domain.subscriber.PickeatSubscriber;
import com.pickeat.sse.domain.subscriber.PickeatSubscriberManager;
import java.lang.reflect.Field;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
@RequiredArgsConstructor
public class PickeatSubscribeManagerCleaner {

    private final PickeatSubscriberManager subscriberManager;

    public void clean() {
        try {
            Field field = PickeatSubscriberManager.class.getDeclaredField("subscriberStorage");
            field.setAccessible(true);

            Map<String, Map<String, PickeatSubscriber>> storage =
                    (Map<String, Map<String, PickeatSubscriber>>) field.get(subscriberManager);

            storage.values().forEach(group ->
                    group.values().forEach(PickeatSubscriber::disconnect)
            );

            storage.clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("PickeatSubscriberManager 초기화 중 오류가 발생했습니다.", e);
        }
    }
}
