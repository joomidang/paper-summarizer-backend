package joomidang.papersummary.common.config.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 논문 관련 이벤트를 RabbitMQ로 발행하는 컴포넌트
 * <p>
 * 이벤트는 타입(PaperEventType)에 따라 Exchange로 전송되며, 라우팅 키는 type.name()을 그대로 사용한다.
 */
@Component
@RequiredArgsConstructor
public class PaperEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(PaperEventEnvelop<?> event) {
        rabbitTemplate.convertAndSend("paper.direct", event.type().name(), event);
    }
}