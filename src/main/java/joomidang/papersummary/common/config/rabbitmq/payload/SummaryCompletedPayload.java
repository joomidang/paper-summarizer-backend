package joomidang.papersummary.common.config.rabbitmq.payload;

public record SummaryCompletedPayload(
        Long paperId,
        String s3Key
) {
}
