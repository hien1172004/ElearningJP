package com.jp.elearningjp.config;

import com.jp.elearningjp.service.notification.NotificationPublisher;
import com.jp.elearningjp.service.curriculum.realtime.CourseContentPublisher;
import com.jp.elearningjp.service.curriculum.realtime.CourseContentRedisSubscriber;
import com.jp.elearningjp.service.notification.NotificationRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Đăng ký instance này vào kênh Redis phát thông báo real-time, để thông báo tới được
 * người dùng kết nối WebSocket ở bất kỳ instance nào (xem {@link NotificationPublisher}).
 * Cùng container nghe kênh báo nội dung khóa học vừa đổi ({@link CourseContentPublisher}).
 */
@Configuration
public class NotificationRedisConfig {

    @Bean
    public RedisMessageListenerContainer notificationListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       NotificationRedisSubscriber subscriber,
                                                                       CourseContentRedisSubscriber courseContentSubscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(NotificationPublisher.CHANNEL));
        container.addMessageListener(courseContentSubscriber, new ChannelTopic(CourseContentPublisher.CHANNEL));
        return container;
    }
}
