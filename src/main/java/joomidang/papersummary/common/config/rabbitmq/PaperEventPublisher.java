package joomidang.papersummary.common.config.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaperEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(PaperEvent event) {
        rabbitTemplate.convertAndSend("paper.direct", event.type().name(), event);
    }
}