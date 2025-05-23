package joomidang.papersummary.common.config.rabbitmq;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 요약본의 상태(조회수, 좋아요수, 댓글수) 관련 이벤트를 RabbitMQ로 발행하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class StatsEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    /**
     * summaryId: 요약본 ID type: "VIEW" | "LIKE" | "UNLIKE" |"COMMENT" | "UNCOMMENT"
     */
    public void publish(Long summaryId, StatsType type) {
        Map<String, Object> eventMessage = Map.of("summaryId", summaryId, "type", type.toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_STATS, eventMessage);
    }
}
