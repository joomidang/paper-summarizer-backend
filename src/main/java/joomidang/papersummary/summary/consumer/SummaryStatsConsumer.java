package joomidang.papersummary.summary.consumer;

import java.util.Map;
import joomidang.papersummary.common.config.rabbitmq.RabbitMQConfig;
import joomidang.papersummary.summary.service.SummaryStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryStatsConsumer {
    private final SummaryStatsService summaryStatsService;

    @RabbitListener(queues = RabbitMQConfig.STATS_QUEUE)
    @Transactional
    @SuppressWarnings("unchecked")
    public void consume(Map<String, Object> payload) {
        Long summaryId = ((Number) payload.get("summaryId")).longValue();
        String type = (String) payload.get("type");

        switch (type) {
            case "VIEW" -> summaryStatsService.increaseViewCount(summaryId);
            case "LIKE" -> summaryStatsService.increaseLikeCount(summaryId);
            case "UNLIKE" -> summaryStatsService.decreaseLikeCount(summaryId);
            case "COMMENT" -> summaryStatsService.increaseCommentCount(summaryId);
            case "UNCOMMENT" -> summaryStatsService.decreaseCommentCount(summaryId);
            default -> throw new IllegalArgumentException("Unsupported stats type: " + type);
        }
    }
}
