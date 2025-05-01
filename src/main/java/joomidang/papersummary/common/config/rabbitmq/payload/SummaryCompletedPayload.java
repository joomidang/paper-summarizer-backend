package joomidang.papersummary.common.config.rabbitmq.payload;

public record SummaryCompletedPayload(
        Long paperId,
        Long summaryId,
        Long userId
) {
}
