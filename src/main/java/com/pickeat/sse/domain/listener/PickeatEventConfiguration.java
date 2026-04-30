package com.pickeat.sse.domain.listener;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class PickeatEventConfiguration {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            PickeatEventListener eventListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        for (PickeatEventChannelTopic topic : PickeatEventChannelTopic.values()) {
            ChannelTopic channelTopic = new ChannelTopic(topic.getValue());
            container.addMessageListener(eventListener, channelTopic);
        }
        return container;
    }
}
