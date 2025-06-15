package joomidang.papersummary.common.config.rabbitmq.payload;

public record ParsingRequestedPayload(
        Long paperId,
        Long userId,
        String s3Url
) {
}
